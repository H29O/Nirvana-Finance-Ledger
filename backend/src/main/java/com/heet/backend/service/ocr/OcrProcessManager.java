package com.heet.backend.service.ocr;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the lifecycle of the local Python-based PaddleOCR microservice.
 * Starts the OCR process automatically when Spring Boot starts up,
 * monitors its health, streams diagnostic logs, and cleans up the child
 * process on application shutdown.
 */
@Slf4j
@Component
public class OcrProcessManager implements SmartLifecycle, DisposableBean {

    @Getter
    private final boolean enabled;
    @Getter
    private final String paddleOcrUrl;
    @Getter
    private final String configuredWorkingDir;
    @Getter
    private final String configuredPythonPath;
    @Getter
    private final String configuredScriptPath;
    @Getter
    private final int readinessTimeoutMs;

    private Process process;
    @Getter
    private boolean processOwned = false;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Deque<String> recentLogs = new ArrayDeque<>();
    private final Object lock = new Object();
    private Thread shutdownHook;

    @Autowired
    public OcrProcessManager(
            @Value("${app.ocr.auto-start.enabled:true}") boolean enabled,
            @Value("${app.ocr.paddle.url:http://localhost:5005}") String paddleOcrUrl,
            @Value("${app.ocr.auto-start.working-dir:}") String configuredWorkingDir,
            @Value("${app.ocr.auto-start.python-path:}") String configuredPythonPath,
            @Value("${app.ocr.auto-start.script-path:}") String configuredScriptPath,
            @Value("${app.ocr.auto-start.readiness-timeout-ms:30000}") int readinessTimeoutMs) {
        this.enabled = enabled;
        this.paddleOcrUrl = (paddleOcrUrl != null && !paddleOcrUrl.isBlank()) ? paddleOcrUrl.trim() : "http://localhost:5005";
        this.configuredWorkingDir = configuredWorkingDir;
        this.configuredPythonPath = configuredPythonPath;
        this.configuredScriptPath = configuredScriptPath;
        this.readinessTimeoutMs = readinessTimeoutMs > 0 ? readinessTimeoutMs : 30000;
    }

    @Override
    public void start() {
        synchronized (lock) {
            if (isRunning.get()) {
                return;
            }
            if (!enabled) {
                log.info("OCR service auto-start is disabled by configuration (app.ocr.auto-start.enabled=false).");
                return;
            }
            if (!isLocalAddress(paddleOcrUrl)) {
                log.info("OCR service URL '{}' is remote. Skipping local process auto-start.", paddleOcrUrl);
                return;
            }

            log.info("Initiating automatic startup of OCR service on {}...", paddleOcrUrl);
            boolean started = ensureStarted();
            if (started) {
                isRunning.set(true);
            } else {
                log.error("================================================================================");
                log.error("[OCR STARTUP ERROR] Automatic OCR service startup failed.");
                log.error("Backend will continue, but receipt OCR processing will remain unavailable.");
                log.error("Please verify Python 3.10+ and requirements in ocr-service/venv or start manually via ./run.sh.");
                log.error("================================================================================");
            }
        }
    }

    /**
     * Ensures that the OCR service is started and healthy.
     * Can be invoked during initial startup or dynamically if needed.
     *
     * @return true if OCR service is responsive, false otherwise.
     */
    public boolean ensureStarted() {
        synchronized (lock) {
            if (!enabled) {
                return isServiceReady();
            }

            // 1. Check if OCR service is ALREADY running and healthy
            if (isServiceReady()) {
                if (process != null && process.isAlive() && processOwned) {
                    log.debug("Managed OCR service (PID: {}) is already running and healthy.", getPidSafe(process));
                    return true;
                }
                log.info("OCR service is already running and healthy at {} - skipping process spawn.", paddleOcrUrl);
                this.processOwned = false;
                return true;
            }

            // If we previously launched a process that exited, clear it
            if (process != null && !process.isAlive()) {
                log.warn("Previous OCR child process (PID: {}) is no longer alive. Restarting...", getPidSafe(process));
                this.process = null;
                this.processOwned = false;
            }

            String host = extractHost(paddleOcrUrl, "127.0.0.1");
            int port = extractPort(paddleOcrUrl, 5005);

            // 2. Check if port is bound by an unhealthy process
            if (isPortBound(host, port)) {
                log.warn("Port {} is already bound, but health check at {} did not succeed. Checking if currently booting...", port, paddleOcrUrl);
                if (waitForReadiness(5000)) {
                    log.info("OCR service on port {} became ready.", port);
                    this.processOwned = false;
                    return true;
                }
                log.error("Port {} is in use by an unresponsive or non-OCR process. Cannot start managed OCR process.", port);
                return false;
            }

            // 3. Resolve working directory
            File ocrDir = resolveWorkingDir();
            if (ocrDir == null || !ocrDir.exists()) {
                log.error("Cannot start OCR service: Unable to locate 'ocr-service' directory relative to {}", System.getProperty("user.dir"));
                return false;
            }

            // 4. Resolve executable or script
            File scriptToRun = resolveScript(ocrDir);
            File pythonExec = (scriptToRun == null) ? resolvePythonExecutable(ocrDir) : null;
            if (scriptToRun == null && pythonExec == null) {
                File fallbackRunSh = new File(ocrDir, "run.sh");
                if (fallbackRunSh.exists() && fallbackRunSh.canExecute()) {
                    scriptToRun = fallbackRunSh;
                }
            }

            if (scriptToRun == null && pythonExec == null) {
                log.error("Cannot start OCR service: Neither Python executable nor startup script found in {}", ocrDir.getAbsolutePath());
                return false;
            }

            // 5. Build command
            List<String> command = new ArrayList<>();
            if (scriptToRun != null) {
                if (scriptToRun.getName().endsWith(".sh")) {
                    command.add("bash");
                    command.add(scriptToRun.getAbsolutePath());
                } else {
                    command.add(scriptToRun.getAbsolutePath());
                }
            } else {
                command.add(pythonExec.getAbsolutePath());
                command.add("-u"); // Unbuffered stdout/stderr
                command.add("-m");
                command.add("uvicorn");
                command.add("app.main:app");
                command.add("--host");
                command.add(host);
                command.add("--port");
                command.add(String.valueOf(port));
            }

            log.info("Launching OCR service process in '{}': {}", ocrDir.getAbsolutePath(), String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(ocrDir);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("OCR_PORT", String.valueOf(port));
            pb.environment().put("OCR_HOST", host);

            try {
                this.process = pb.start();
                this.processOwned = true;
                registerShutdownHook();
                startLogReader(this.process);

                long pid = getPidSafe(this.process);
                log.info("OCR service child process spawned (PID: {}). Awaiting health check on port {}...", pid, port);

                // 6. Wait for service readiness
                boolean ready = waitForReadiness(this.readinessTimeoutMs);
                if (ready) {
                    log.info("Nivara OCR Service is UP and healthy (PID: {}) at {}/health.", pid, paddleOcrUrl);
                    return true;
                } else {
                    log.error("OCR service failed readiness check within {}ms. Process PID: {}. Recent logs:\n{}",
                            this.readinessTimeoutMs, pid, getRecentLogsSummary());
                    stop();
                    return false;
                }

            } catch (Exception ex) {
                log.error("Exception occurred while launching OCR service: {}", ex.getMessage(), ex);
                stop();
                return false;
            }
        }
    }

    /**
     * Checks if the OCR service is responding with status UP.
     */
    public boolean isServiceReady() {
        String base = paddleOcrUrl.endsWith("/") ? paddleOcrUrl.substring(0, paddleOcrUrl.length() - 1) : paddleOcrUrl;
        String healthUrl = base + "/health";
        if (checkHttpHealth(healthUrl)) {
            return true;
        }
        if (healthUrl.contains("localhost")) {
            String fallbackUrl = healthUrl.replace("localhost", "127.0.0.1");
            return checkHttpHealth(fallbackUrl);
        }
        return false;
    }

    private boolean checkHttpHealth(String healthUrl) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(800))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofMillis(1500))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body() != null && response.body().contains("\"status\":\"UP\"");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean waitForReadiness(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (processOwned && process != null && !process.isAlive()) {
                int exitCode = process.exitValue();
                log.error("OCR service child process terminated prematurely with exit code: {}", exitCode);
                return false;
            }
            if (isServiceReady()) {
                return true;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void startLogReader(Process p) {
        Thread logThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[PaddleOCR] {}", line);
                    synchronized (recentLogs) {
                        if (recentLogs.size() >= 50) {
                            recentLogs.pollFirst();
                        }
                        recentLogs.addLast(line);
                    }
                }
            } catch (IOException ignored) {
            }
        }, "ocr-service-logger");
        logThread.setDaemon(true);
        logThread.start();
    }

    public String getRecentLogsSummary() {
        synchronized (recentLogs) {
            if (recentLogs.isEmpty()) {
                return "(No logs recorded)";
            }
            return String.join("\n", recentLogs);
        }
    }

    private boolean isPortBound(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 600);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public File resolveWorkingDir() {
        if (configuredWorkingDir != null && !configuredWorkingDir.isBlank()) {
            File dir = new File(configuredWorkingDir.trim());
            if (dir.isDirectory()) {
                return getCanonicalSafe(dir);
            }
        }
        List<File> candidates = List.of(
                new File("../ocr-service"),
                new File("ocr-service"),
                new File(System.getProperty("user.dir"), "../ocr-service"),
                new File(System.getProperty("user.dir"), "ocr-service")
        );
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isDirectory()) {
                return getCanonicalSafe(candidate);
            }
        }
        return null;
    }

    public File resolveScript(File ocrDir) {
        if (configuredScriptPath != null && !configuredScriptPath.isBlank()) {
            File custom = new File(configuredScriptPath.trim());
            if (custom.isFile()) {
                return custom;
            }
            File rel = new File(ocrDir, configuredScriptPath.trim());
            if (rel.isFile()) {
                return rel;
            }
        }
        return null;
    }

    public File resolvePythonExecutable(File ocrDir) {
        if (configuredPythonPath != null && !configuredPythonPath.isBlank()) {
            File custom = new File(configuredPythonPath.trim());
            if (custom.isFile() && custom.canExecute()) {
                return custom;
            }
        }
        List<File> candidates = List.of(
                new File(ocrDir, "venv/bin/python"),
                new File(ocrDir, "venv/bin/python3"),
                new File(ocrDir, "venv/Scripts/python.exe"),
                new File(ocrDir, ".venv/bin/python"),
                new File(ocrDir, ".venv/bin/python3"),
                new File(ocrDir, ".venv/Scripts/python.exe")
        );
        for (File candidate : candidates) {
            if (candidate.exists() && candidate.canExecute()) {
                return candidate;
            }
        }

        return findSystemExecutable("python3", "python");
    }

    private File findSystemExecutable(String... names) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }
        String[] dirs = pathEnv.split(File.pathSeparator);
        for (String name : names) {
            for (String dir : dirs) {
                File candidate = new File(dir, name);
                if (candidate.exists() && candidate.canExecute()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private File getCanonicalSafe(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return file.getAbsoluteFile();
        }
    }

    private boolean isLocalAddress(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return host == null || host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0");
        } catch (Exception e) {
            return false;
        }
    }

    private String extractHost(String url, String defaultHost) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return (host != null && !host.isBlank()) ? host : defaultHost;
        } catch (Exception e) {
            return defaultHost;
        }
    }

    private int extractPort(String url, int defaultPort) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            return port > 0 ? port : defaultPort;
        } catch (Exception e) {
            return defaultPort;
        }
    }

    private long getPidSafe(Process p) {
        if (p == null) return -1;
        try {
            return p.pid();
        } catch (Exception e) {
            return -1;
        }
    }

    private synchronized void registerShutdownHook() {
        if (shutdownHook == null) {
            shutdownHook = new Thread(() -> {
                log.info("JVM shutdown hook triggered: stopping managed OCR service...");
                stop();
            }, "ocr-service-shutdown-hook");
            try {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    @Override
    @PreDestroy
    public void destroy() {
        stop();
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (!processOwned || process == null) {
                isRunning.set(false);
                return;
            }
            long pid = getPidSafe(process);
            log.info("Stopping managed OCR service process (PID: {})...", pid);
            try {
                // Destroy descendants first
                process.descendants().forEach(ph -> {
                    try { ph.destroy(); } catch (Exception ignored) {}
                });
                process.destroy();
                boolean terminated = process.waitFor(5, TimeUnit.SECONDS);
                if (!terminated) {
                    log.warn("OCR process (PID: {}) did not terminate within 5 seconds. Forcibly destroying...", pid);
                    process.descendants().forEach(ph -> {
                        try { ph.destroyForcibly(); } catch (Exception ignored) {}
                    });
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                log.info("Managed OCR service process (PID: {}) terminated cleanly.", pid);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            } catch (Exception e) {
                log.warn("Error while stopping OCR process (PID: {}): {}", pid, e.getMessage());
                process.destroyForcibly();
            } finally {
                this.process = null;
                this.processOwned = false;
                this.isRunning.set(false);
                if (shutdownHook != null) {
                    try {
                        Runtime.getRuntime().removeShutdownHook(shutdownHook);
                    } catch (IllegalStateException ignored) {
                    }
                    shutdownHook = null;
                }
            }
        }
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
