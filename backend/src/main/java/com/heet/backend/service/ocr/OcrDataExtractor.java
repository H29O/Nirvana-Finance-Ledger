package com.heet.backend.service.ocr;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OcrDataExtractor {

    private static final Set<String> GENERIC_HEADERS = new HashSet<>(Arrays.asList(
            "receipt", "tax invoice", "invoice", "bill", "welcome",
            "official receipt", "payment receipt", "customer copy", "original copy",
            "cash receipt", "merchant copy", "sale", "transaction receipt",
            "store", "thank you", "order", "e-receipt", "cash bill"
    ));

    private static final Pattern TOTAL_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:grand\\s*total|net\\s*total|total\\s*amount|balance\\s*due|amount\\s*due|total|amount|paid)\\s*[:=]?\\s*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)\\s*)?([0-9]{1,7}(?:[.,][0-9]{2}))"
    );

    private static final Pattern FALLBACK_AMOUNT_PATTERN = Pattern.compile(
            "(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)\\s*)?([0-9]{1,7}\\.[0-9]{2})"
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{4})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])\\b"
    );

    private static final Pattern DMY_DATE_PATTERN = Pattern.compile(
            "\\b(0?[1-9]|[12]\\d|3[01])[-/.](0?[1-9]|1[0-2])[-/.](\\d{4})\\b"
    );

    private static final Pattern TEXT_DATE_PATTERN = Pattern.compile(
            "(?i)\\b(0?[1-9]|[12]\\d|3[01])\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+(\\d{4})\\b"
    );

    public String extractMerchant(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        String[] lines = rawText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 2) {
                continue;
            }

            String normalized = trimmed.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (GENERIC_HEADERS.contains(normalized)) {
                continue;
            }

            // Remove excessive special characters around name
            String cleaned = trimmed.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9.)]+$", "").trim();
            if (!cleaned.isEmpty() && cleaned.matches(".*[a-zA-Z0-9].*")) {
                if (cleaned.length() > 80) {
                    cleaned = cleaned.substring(0, 80).trim();
                }
                return cleaned;
            }
        }

        return null;
    }

    public BigDecimal extractAmount(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        Matcher keywordMatcher = TOTAL_KEYWORD_PATTERN.matcher(rawText);
        BigDecimal foundAmount = null;

        while (keywordMatcher.find()) {
            String amountStr = keywordMatcher.group(1).replace(',', '.');
            try {
                BigDecimal parsed = new BigDecimal(amountStr);
                if (parsed.compareTo(BigDecimal.ZERO) > 0) {
                    foundAmount = parsed; // Use latest/last total keyword match
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (foundAmount != null) {
            return foundAmount;
        }

        // Fallback: search for standard monetary amounts and pick the largest
        Matcher fallbackMatcher = FALLBACK_AMOUNT_PATTERN.matcher(rawText);
        BigDecimal maxAmount = null;

        while (fallbackMatcher.find()) {
            String amountStr = fallbackMatcher.group(1);
            try {
                BigDecimal parsed = new BigDecimal(amountStr);
                if (maxAmount == null || parsed.compareTo(maxAmount) > 0) {
                    maxAmount = parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return maxAmount;
    }

    public LocalDate extractDate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        // 1. Check ISO format (YYYY-MM-DD)
        Matcher isoMatcher = ISO_DATE_PATTERN.matcher(rawText);
        if (isoMatcher.find()) {
            int year = Integer.parseInt(isoMatcher.group(1));
            int month = Integer.parseInt(isoMatcher.group(2));
            int day = Integer.parseInt(isoMatcher.group(3));
            try {
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
            }
        }

        // 2. Check DMY format (DD-MM-YYYY)
        Matcher dmyMatcher = DMY_DATE_PATTERN.matcher(rawText);
        if (dmyMatcher.find()) {
            int day = Integer.parseInt(dmyMatcher.group(1));
            int month = Integer.parseInt(dmyMatcher.group(2));
            int year = Integer.parseInt(dmyMatcher.group(3));
            try {
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
            }
        }

        // 3. Check Text date format (e.g. 21 Aug 2026)
        Matcher textMatcher = TEXT_DATE_PATTERN.matcher(rawText);
        if (textMatcher.find()) {
            String dayStr = String.format("%02d", Integer.parseInt(textMatcher.group(1)));
            String monthStr = textMatcher.group(2).substring(0, 3);
            String yearStr = textMatcher.group(3);
            String formatted = dayStr + " " + monthStr + " " + yearStr;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
                return LocalDate.parse(formatted, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }
}
