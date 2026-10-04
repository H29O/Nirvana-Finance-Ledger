package com.heet.backend;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class SmtpDiagnosticRunner {

    private String getSecret() {
        String fromEnv = System.getenv("SPRING_MAIL_PASSWORD");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.replace(" ", "");
        }
        String fromEnv2 = System.getenv("Google");
        if (fromEnv2 != null && !fromEnv2.isBlank()) {
            return fromEnv2.replace(" ", "");
        }
        String fromEnv3 = System.getenv("GMAIL_APP_PASSWORD");
        if (fromEnv3 != null && !fromEnv3.isBlank()) {
            return fromEnv3.replace(" ", "");
        }

        List<File> candidates = List.of(
                new File("../.env"),
                new File(".env"),
                new File(System.getProperty("user.dir"), ".env"),
                new File(System.getProperty("user.dir"), "../.env")
        );

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(candidate.toPath(), StandardCharsets.UTF_8);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("Google=") || trimmed.startsWith("GMAIL_APP_PASSWORD=") || trimmed.startsWith("SPRING_MAIL_PASSWORD=")) {
                            String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isBlank()) {
                                return val.replace(" ", "");
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private String getUsername() {
        String fromEnv = System.getenv("SPRING_MAIL_USERNAME");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromEnv2 = System.getenv("GMAIL_USERNAME");
        if (fromEnv2 != null && !fromEnv2.isBlank()) {
            return fromEnv2;
        }

        List<File> candidates = List.of(
                new File("../.env"),
                new File(".env"),
                new File(System.getProperty("user.dir"), ".env"),
                new File(System.getProperty("user.dir"), "../.env")
        );

        for (File candidate : candidates) {
            if (candidate.exists() && candidate.isFile()) {
                try {
                    List<String> lines = Files.readAllLines(candidate.toPath(), StandardCharsets.UTF_8);
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("GMAIL_USERNAME=") || trimmed.startsWith("SPRING_MAIL_USERNAME=")) {
                            String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isBlank()) {
                                return val;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return "nivara.app@gmail.com";
    }

    @Test
    public void runFullSmtpDiagnostic() {
        System.out.println("================================================================================");
        System.out.println("                        SMTP DIAGNOSTIC REPORT                                  ");
        System.out.println("================================================================================");

        String host = "smtp.gmail.com";
        int port = 465;
        String username = getUsername();
        String password = getSecret();
        String sender = "Nivara <" + username + ">";

        // Safe diagnostics
        System.out.println("1. RUNTIME CONFIGURATION INSPECTION:");
        System.out.println("   SMTP host configured:       " + (host != null ? host : "false"));
        System.out.println("   SMTP port configured:       " + port);
        System.out.println("   SMTP username configured:   " + (username != null ? username : "false"));
        System.out.println("   SMTP password configured:   " + (password != null && !password.isBlank() ? "true (length: " + password.length() + ")" : "false"));
        System.out.println("   SMTP sender configured:     " + sender);
        System.out.println("   SMTP SSL enabled:           true");
        System.out.println("   Java version:               " + System.getProperty("java.version"));
        System.out.println("   Working dir:                " + System.getProperty("user.dir"));

        if (password == null || password.isBlank()) {
            System.err.println("FATAL: Gmail SMTP password could not be resolved from environment or .env!");
            return;
        }

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Test SMTP connection and authentication
        System.out.println("\n2. ISOLATED SMTP CONNECTION & AUTHENTICATION TEST:");
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(host, port, username, password);
            System.out.println("   RESULT: PASS (Connected to " + host + ":" + port + " and authenticated successfully with Gmail)");
        } catch (Exception e) {
            System.out.println("   RESULT: FAIL");
            printFullExceptionChain("AUTHENTICATION FAILURE", e);
            return;
        }

        // Test delivery to Nivara registration email
        System.out.println("\n3. RECIPIENT DELIVERY TEST:");
        String recipient = "heetoswal7@gmail.com";
        System.out.println("\n   --> Sending to Registration Email: " + recipient);
        sendSingleTest(session, sender, recipient, "Nivara Diagnostic Test - Registration Email");

        System.out.println("\n================================================================================");
        System.out.println("                     END OF SMTP DIAGNOSTIC REPORT                              ");
        System.out.println("================================================================================");
    }

    private void sendSingleTest(Session session, String from, String to, String subject) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText("This is an isolated SMTP diagnostic probe from Nivara backend.");

            Transport.send(message);
            System.out.println("       STATUS: ACCEPTED / DELIVERED (250 OK)");
            System.out.println("       The SMTP server accepted the message for " + to);
        } catch (Exception e) {
            System.out.println("       STATUS: REJECTED / FAILED");
            printFullExceptionChain("DELIVERY TO " + to, e);
        }
    }

    private void printFullExceptionChain(String context, Throwable t) {
        System.out.println("       Exception in " + context + ":");
        Throwable current = t;
        int depth = 0;
        while (current != null) {
            String indent = "         ".repeat(Math.min(depth + 1, 4));
            System.out.println(indent + "-> [" + current.getClass().getName() + "]: " + current.getMessage());
            if (current instanceof SendFailedException sfe) {
                Address[] invalid = sfe.getInvalidAddresses();
                if (invalid != null && invalid.length > 0) {
                    for (Address a : invalid) {
                        System.out.println(indent + "   [Invalid Address]: " + a);
                    }
                }
            }
            current = current.getCause();
            depth++;
        }
    }
}
