package com.heet.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		loadEnvIfPresent();
		SpringApplication.run(BackendApplication.class, args);
	}

	private static void loadEnvIfPresent() {
		List<File> candidates = List.of(
				new File(".env"),
				new File("../.env"),
				new File(System.getProperty("user.dir"), ".env"),
				new File(System.getProperty("user.dir"), "../.env")
		);

		for (File candidate : candidates) {
			if (candidate.exists() && candidate.isFile()) {
				try {
					List<String> lines = Files.readAllLines(candidate.toPath(), StandardCharsets.UTF_8);
					for (String line : lines) {
						String trimmed = line.trim();
						if (trimmed.startsWith("#") || trimmed.isEmpty() || !trimmed.contains("=")) {
							continue;
						}
						int eqIdx = trimmed.indexOf('=');
						String key = trimmed.substring(0, eqIdx).trim();
						String value = trimmed.substring(eqIdx + 1).trim();
						if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
							value = value.substring(1, value.length() - 1);
						}
						if (System.getenv(key) == null) {
							System.setProperty(key, value);
						}
						if ((key.equals("Google") || key.equalsIgnoreCase("GMAIL_APP_PASSWORD")) 
								&& System.getProperty("SPRING_MAIL_PASSWORD") == null 
								&& System.getenv("SPRING_MAIL_PASSWORD") == null) {
							String sanitizedPassword = value.replace(" ", "");
							System.setProperty("SPRING_MAIL_PASSWORD", sanitizedPassword);
							System.setProperty("GMAIL_APP_PASSWORD", sanitizedPassword);
							System.setProperty("Google", sanitizedPassword);
						}
						if ((key.equalsIgnoreCase("GMAIL_USERNAME") || key.equalsIgnoreCase("GMAIL_ADDRESS")) 
								&& System.getProperty("SPRING_MAIL_USERNAME") == null 
								&& System.getenv("SPRING_MAIL_USERNAME") == null) {
							System.setProperty("SPRING_MAIL_USERNAME", value);
							System.setProperty("GMAIL_USERNAME", value);
						}
					}
					break;
				} catch (Exception ignored) {
				}
			}
		}
	}
}
