package com.heet.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

@Slf4j
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.resend.com}")
    private String host;

    @Value("${spring.mail.port:465}")
    private int port;

    @Value("${spring.mail.username:resend}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);

        String effectivePassword = password;
        if (effectivePassword == null || effectivePassword.isBlank()) {
            effectivePassword = loadSecretFromEnvFile();
        }
        mailSender.setPassword(effectivePassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.resend.com");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return mailSender;
    }

    private String loadSecretFromEnvFile() {
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
                        if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                            continue;
                        }
                        if (trimmed.startsWith("Resend_SMTP=") || trimmed.startsWith("RESEND_SMTP_PASSWORD=")) {
                            String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (!val.isBlank()) {
                                return val;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Failed reading .env file: {}", e.getMessage());
                }
            }
        }
        return "";
    }
}
