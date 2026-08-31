package com.heet.backend.service.ocr;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OcrDataExtractor {

    private static final Set<String> NON_MERCHANT_SUBSTRINGS = new HashSet<>(Arrays.asList(
            "tax invoice", "taxinvoice", "invoice", "gstin", "gst no", "gst.no", "gstin:", "gst :",
            "receipt", "tax bill", "cash bill", "cash memo", "retail invoice", "customer copy",
            "original copy", "duplicate copy", "merchant copy", "original for buyer", "welcome",
            "thank you", "order no", "order #", "table no", "cashier", "counter", "pos terminal",
            "cin:", "fssai", "dl no", "dl.no", "d.l.no", "tin no", "tel:", "tel no", "phone", "mob:", "ph:",
            "email:", "website", "www.", "http", "address", "road", "street", "nagar", "marg",
            "complex", "floor", "near ", "opp.", "pin:", "pincode", "pan no", "state:", "state code",
            "terms & conditions", "e.&o.e", "subject to", "jurisdiction", "date:", "time:", "batch no",
            "hsn code", "exp date", "mfg date"
    ));

    private static final Set<String> BUSINESS_KEYWORDS = new HashSet<>(Arrays.asList(
            "pharmacy", "medical", "medicals", "chemist", "chemists", "druggist", "druggists",
            "pharma", "pharmaceuticals", "pharmaceutical", "hospital", "clinic", "diagnostics",
            "laboratories", "labs", "healthcare", "ayurvedic", "homeo", "medico", "supermarket",
            "mart", "hypermarket", "store", "stores", "restaurant", "cafe", "foods", "bakers",
            "bakery", "bistro", "kitchen", "dhaba", "sweets", "jewellers", "textiles", "garments",
            "enterprises", "enterprise", "traders", "agency", "agencies", "corporation", "ltd",
            "limited", "pvt", "private", "co.", "inc", "footwear", "stationers", "petrol", "pump"
    ));

    // High-priority explicit total labels (strictly same-line using horizontal whitespace only)
    private static final Pattern HIGH_PRIORITY_TOTAL_PATTERN = Pattern.compile(
            "(?i)(?:grand\\s*total|net\\s*payable|amount\\s*payable|payable\\s*amount|total\\s*payable|net\\s*amount|net\\s*amt|bill\\s*total|bill\\s*amount|total\\s*amount|total\\s*amt|balance\\s*due|amount\\s*due|final\\s*amount|total\\s*bill|round\\s*off\\s*total|taxable\\s*amount|taxable\\s*amt|gross\\s*amount|gross\\s*amt|inv(?:oice)?\\s*total)[ \\t:=.\\-|]*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:,[0-9]{2,3})*(?:[ \\t]*[\\.,][ \\t]*[0-9]{1,2})?|[0-9]+(?:[ \\t]*[\\.,][ \\t]*[0-9]{1,2})?)[ \\t]*(?:\\/[-=]?)?"
    );

    // Standard total labels (strictly same-line)
    private static final Pattern STANDARD_TOTAL_PATTERN = Pattern.compile(
            "(?i)(?:total|amount|amt|sub\\s*total|subtotal|mrp|payable|paid)[ \\t:=.\\-|]*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:,[0-9]{2,3})*(?:[ \\t]*[\\.,][ \\t]*[0-9]{1,2})?|[0-9]+(?:[ \\t]*[\\.,][ \\t]*[0-9]{1,2})?)[ \\t]*(?:\\/[-=]?)?"
    );

    // Currency prefixed standalone amounts
    private static final Pattern CURRENCY_PREFIXED_PATTERN = Pattern.compile(
            "(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*([0-9]{1,3}(?:,[0-9]{2,3})*(?:[ \\t]*\\.[ \\t]*[0-9]{1,2})?|[0-9]+(?:[ \\t]*\\.[ \\t]*[0-9]{1,2})?)"
    );

    // Table line item amount: matches numbers at the end of item rows (e.g. "ITEM ... 1 450.00 450.00")
    private static final Pattern TABLE_ROW_AMOUNT_PATTERN = Pattern.compile(
            "(?m)(?:.*[ \\t]+)?([0-9]{1,3}(?:,[0-9]{2,3})*\\.[0-9]{2}|[0-9]+\\.[0-9]{2})[ \\t]*$"
    );

    // Fallback general monetary pattern (requires decimal)
    private static final Pattern FALLBACK_AMOUNT_PATTERN = Pattern.compile(
            "\\b([0-9]{1,3}(?:,[0-9]{2,3})*\\.[0-9]{2}|[0-9]+\\.[0-9]{2})\\b"
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
        String bestCandidate = null;
        int bestScore = -100;

        int linesToCheck = Math.min(lines.length, 12);

        for (int i = 0; i < linesToCheck; i++) {
            String line = lines[i].trim();
            if (line.length() < 3) {
                continue;
            }

            String normalized = line.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (isNonMerchantLine(normalized, line)) {
                continue;
            }

            int score = calculateMerchantScore(line, normalized, i);

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = cleanMerchantName(line);
            }
        }

        return bestCandidate;
    }

    private boolean isNonMerchantLine(String normalized, String original) {
        for (String nonMerchant : NON_MERCHANT_SUBSTRINGS) {
            if (normalized.contains(nonMerchant)) {
                return true;
            }
        }

        int letters = 0;
        int digits = 0;
        int symbols = 0;

        for (char c : original.toCharArray()) {
            if (Character.isLetter(c)) letters++;
            else if (Character.isDigit(c)) digits++;
            else if (!Character.isWhitespace(c)) symbols++;
        }

        if (letters < 3) {
            return true;
        }

        double nonLetterRatio = (double) (digits + symbols) / original.length();
        if (nonLetterRatio > 0.40) {
            return true;
        }

        return false;
    }

    private int calculateMerchantScore(String original, String normalized, int lineIndex) {
        int score = 20 - (lineIndex * 2);

        for (String keyword : BUSINESS_KEYWORDS) {
            if (normalized.contains(keyword)) {
                score += 30;
                break;
            }
        }

        if (original.equals(original.toUpperCase(Locale.ROOT)) && original.length() >= 4) {
            score += 8;
        }

        if (original.contains("*") || original.contains("+") || original.contains("#")) {
            score -= 10;
        }

        return score;
    }

    private String cleanMerchantName(String name) {
        if (name == null) return null;
        String cleaned = name.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9.)]+$", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    public BigDecimal extractAmount(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        // 1. High-priority explicit total labels (Net Payable, Grand Total, Total Amount, Net Amt, etc.)
        Matcher highPriorityMatcher = HIGH_PRIORITY_TOTAL_PATTERN.matcher(rawText);
        BigDecimal foundHighPriority = null;
        while (highPriorityMatcher.find()) {
            BigDecimal parsed = parseAmount(highPriorityMatcher.group(1));
            if (parsed != null && parsed.compareTo(BigDecimal.ZERO) > 0) {
                foundHighPriority = parsed;
            }
        }
        if (foundHighPriority != null) {
            return foundHighPriority;
        }

        // 2. Standard total labels (Total, Amount, Subtotal, Paid, etc.)
        Matcher standardMatcher = STANDARD_TOTAL_PATTERN.matcher(rawText);
        BigDecimal foundStandard = null;
        while (standardMatcher.find()) {
            BigDecimal parsed = parseAmount(standardMatcher.group(1));
            if (parsed != null && parsed.compareTo(BigDecimal.ZERO) > 0) {
                foundStandard = parsed;
            }
        }
        if (foundStandard != null) {
            return foundStandard;
        }

        // 3. Currency-prefixed amounts (e.g. ₹1,250.00, Rs. 450)
        Matcher currencyMatcher = CURRENCY_PREFIXED_PATTERN.matcher(rawText);
        BigDecimal maxCurrencyAmount = null;
        while (currencyMatcher.find()) {
            BigDecimal parsed = parseAmount(currencyMatcher.group(1));
            if (parsed != null && parsed.compareTo(BigDecimal.ZERO) > 0) {
                if (maxCurrencyAmount == null || parsed.compareTo(maxCurrencyAmount) > 0) {
                    maxCurrencyAmount = parsed;
                }
            }
        }
        if (maxCurrencyAmount != null) {
            return maxCurrencyAmount;
        }

        // 4. Line item table column amounts (pick the largest valid item line amount)
        Matcher tableRowMatcher = TABLE_ROW_AMOUNT_PATTERN.matcher(rawText);
        BigDecimal maxTableRowAmount = null;
        while (tableRowMatcher.find()) {
            BigDecimal parsed = parseAmount(tableRowMatcher.group(1));
            if (parsed != null && isValidMonetaryAmount(parsed, rawText)) {
                if (maxTableRowAmount == null || parsed.compareTo(maxTableRowAmount) > 0) {
                    maxTableRowAmount = parsed;
                }
            }
        }
        if (maxTableRowAmount != null) {
            return maxTableRowAmount;
        }

        // 5. Fallback: search for standard decimal amounts and pick the largest valid monetary amount
        Matcher fallbackMatcher = FALLBACK_AMOUNT_PATTERN.matcher(rawText);
        BigDecimal maxFallback = null;
        while (fallbackMatcher.find()) {
            BigDecimal parsed = parseAmount(fallbackMatcher.group(1));
            if (parsed != null && isValidMonetaryAmount(parsed, rawText)) {
                if (maxFallback == null || parsed.compareTo(maxFallback) > 0) {
                    maxFallback = parsed;
                }
            }
        }

        return maxFallback;
    }

    private boolean isValidMonetaryAmount(BigDecimal amount, String rawText) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        long valLong = amount.longValue();

        // Reject 10-digit phone numbers
        if (valLong >= 6000000000L && valLong <= 9999999999L) {
            return false;
        }

        // Reject 6-digit pin codes
        if (valLong >= 100000L && valLong <= 999999L && amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        // Reject years like 2020 to 2035 if integer
        if (valLong >= 2020 && valLong <= 2035 && amount.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        return true;
    }

    private BigDecimal parseAmount(String rawAmountStr) {
        if (rawAmountStr == null || rawAmountStr.isBlank()) {
            return null;
        }

        String clean = rawAmountStr.replaceAll("[ \\t]+", "")
                .replace(",", "")
                .trim();

        try {
            BigDecimal bd = new BigDecimal(clean);
            return bd.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
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
