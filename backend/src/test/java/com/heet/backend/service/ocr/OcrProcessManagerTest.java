package com.heet.backend.service.ocr;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class OcrProcessManagerTest {

    @Test
    void testResolveWorkingDir_FindsOcrService() {
        OcrProcessManager manager = new OcrProcessManager(true, "http://localhost:5005", null, null, null, 30000);
        File ocrDir = manager.resolveWorkingDir();
        assertNotNull(ocrDir, "Should resolve ocr-service directory");
        assertTrue(ocrDir.exists(), "Resolved directory must exist");
        assertTrue(ocrDir.isDirectory(), "Resolved path must be a directory");
        assertEquals("ocr-service", ocrDir.getName());
    }

    @Test
    void testResolvePythonExecutable_FindsVenvPython() {
        OcrProcessManager manager = new OcrProcessManager(true, "http://localhost:5005", null, null, null, 30000);
        File ocrDir = manager.resolveWorkingDir();
        assertNotNull(ocrDir);

        File pythonExec = manager.resolvePythonExecutable(ocrDir);
        assertNotNull(pythonExec, "Should resolve Python executable in ocr-service");
        assertTrue(pythonExec.exists(), "Resolved python executable must exist");
        assertTrue(pythonExec.canExecute(), "Resolved python executable must be executable");
    }

    @Test
    void testAutoStartDisabled_DoesNotStart() {
        OcrProcessManager manager = new OcrProcessManager(false, "http://localhost:5005", null, null, null, 30000);
        assertFalse(manager.isEnabled());

        manager.start();
        assertFalse(manager.isRunning());
        assertFalse(manager.isProcessOwned());
    }

    @Test
    void testRemoteUrl_SkipsAutoStart() {
        OcrProcessManager manager = new OcrProcessManager(true, "https://api.external-ocr.com", null, null, null, 30000);
        manager.start();
        assertFalse(manager.isRunning());
        assertFalse(manager.isProcessOwned());
    }

    @Test
    void testStop_IsIdempotent() {
        OcrProcessManager manager = new OcrProcessManager(true, "http://localhost:5005", null, null, null, 30000);
        assertDoesNotThrow(() -> manager.stop());
        assertDoesNotThrow(() -> manager.stop());
        assertFalse(manager.isProcessOwned());
    }

    @Test
    void testLiveProcessStartReadinessAndShutdown() {
        OcrProcessManager manager = new OcrProcessManager(true, "http://127.0.0.1:5005", null, null, null, 30000);
        boolean owned = false;
        try {
            boolean started = manager.ensureStarted();
            owned = manager.isProcessOwned();
            assertTrue(started, "OCR process should successfully start and pass readiness");
            assertTrue(manager.isServiceReady(), "OCR service should report ready via health endpoint");

            // Verify idempotency when already started
            boolean secondStart = manager.ensureStarted();
            assertTrue(secondStart, "Subsequent call to ensureStarted should succeed without duplicate launch");

        } finally {
            manager.stop();
            assertFalse(manager.isProcessOwned(), "Process ownership should be released after stop");
            // If this manager instance owned the process, verify it terminated; otherwise if externally managed, service remains online
            if (owned) {
                boolean stopped = false;
                for (int i = 0; i < 30; i++) {
                    if (!manager.isServiceReady()) {
                        stopped = true;
                        break;
                    }
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
                assertTrue(stopped, "OCR service should no longer be responding after stop");
            }
        }
    }
}
