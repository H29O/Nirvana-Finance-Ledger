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
            "hsn code", "exp date", "mfg date", "having issues", "share & split", "split expense",
            "item amount", "item name", "item description", "items", "particulars", "description",
            "unit price", "net payable", "total amount", "net amt", "grand total", "sub total",
            "bill total", "docs params authorization", "postman"
    ));

    private static final Set<String> BUSINESS_KEYWORDS = new HashSet<>(Arrays.asList(
            "pharmacy", "medical", "medicals", "chemist", "chemists", "druggist", "druggists",
            "pharma", "pharmaceuticals", "pharmaceutical", "hospital", "clinic", "diagnostics",
            "laboratories", "labs", "healthcare", "ayurvedic", "homeo", "medico", "supermarket",
            "mart", "hypermarket", "store", "stores", "restaurant", "cafe", "foods", "bakers",
            "bakery", "bistro", "kitchen", "dhaba", "sweets", "jewellers", "textiles", "garments",
            "enterprises", "enterprise", "traders", "agency", "agencies", "corporation", "ltd",
            "limited", "pvt", "private", "co.", "inc", "footwear", "stationers", "petrol", "pump",
            "cinema", "cinemas", "theatre", "bazaar", "bazar", "retail", "wholesale", "shree", "market"
    ));

    // High-priority explicit total labels (supports quotes, optional colon/dash/newline, and OCR typos)
    private static final Pattern HIGH_PRIORITY_TOTAL_PATTERN = Pattern.compile(
            "(?i)[\"']?(?:grand\\s*tot[a-z0-9]*|net\\s*payab[a-z0-9]*|tot[a-z0-9]*\\s*payab[a-z0-9]*|amount\\s*payab[a-z0-9]*|payab[a-z0-9]*\\s*amount|tot[a-z0-9]*\\s*amount|tot[a-z0-9]*\\s*amt|bill\\s*tot[a-z0-9]*|bill\\s*amount|inv(?:oice)?\\s*tot[a-z0-9]*|net\\s*amount|net\\s*amt|balance\\s*due|amount\\s*due|final\\s*amount|final\\s*tot[a-z0-9]*|tot[a-z0-9]*\\s*due|tot[a-z0-9]*\\s*charges|gross\\s*amount|taxable\\s*amount)[\"']?[ \\t:=.\\-|~_#]*(?:\\r?\\n[ \\t]*)?(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Payment / Transfer action pattern (e.g. "Payment of 220 completed", "Paid 450", "Paid to ... ₹500", "Amount Paid: 250")
    private static final Pattern PAYMENT_ACTION_PATTERN = Pattern.compile(
            "(?i)[\"‘“']?(?:payment\\s+(?:of|amount)|amount\\s+paid|paid\\s+to|paid|sent|debited|transferred|upi)[ \\t:=.\\-|'\"‘“]*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Standard total & amount labels on the same line (e.g. "Total: 450.00", "Amount: 850.00")
    private static final Pattern STANDARD_TOTAL_SAME_LINE_PATTERN = Pattern.compile(
            "(?i)(?<!(?:sub|tax|round[\\s-]*off)\\s*)[\"']?(?:tot[a-z0-9]*|amount|amt|sub\\s*tot[a-z0-9]*|mrp)[\"']?(?!\\s*(?:qty|quantity|items?|count|pcs|pieces))[ \\t:=.\\-|~_#]+(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Standalone total label on its own line followed by amount on next line
    private static final Pattern STANDALONE_TOTAL_LABEL_NEXT_LINE_PATTERN = Pattern.compile(
            "(?im)^[ \\t]*[\"']?(?:grand\\s*tot[a-z0-9]*|tot[a-z0-9]*|amount|amt|sub\\s*tot[a-z0-9]*|net\\s*payab[a-z0-9]*|amount\\s*payab[a-z0-9]*)[\"']?[ \\t:=.\\-|~_#]*\\r?\\n[ \\t]*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Standalone line containing only an amount (very common in UPI / banking screenshots like Google Pay, PhonePe)
    private static final Pattern STANDALONE_LINE_AMOUNT_PATTERN = Pattern.compile(
            "^\\s*[\"']?(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?[\"']?\\s*$",
            Pattern.MULTILINE
    );

    // Currency prefixed standalone amounts (e.g. ₹1,250.00, Rs. 450, $25)
    private static final Pattern CURRENCY_PREFIXED_PATTERN = Pattern.compile(
            "(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    );

    // Currency suffixed amounts (e.g. 450/- or 1200 INR)
    private static final Pattern CURRENCY_SUFFIXED_PATTERN = Pattern.compile(
            "\\b([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=])",
            Pattern.CASE_INSENSITIVE
    );

    // Table line item amount: matches numbers at the end of item rows (e.g. "ITEM ... 1 450.00 450.00")
    private static final Pattern TABLE_ROW_AMOUNT_PATTERN = Pattern.compile(
            "(?m)(?:.*[ \\t]+)?([0-9]{1,3}(?:,[0-9]{2,3})*\\.[0-9]{2}|[0-9]+\\.[0-9]{2})[ \\t]*$"
    );

    // Table column header line detector (to prevent confusing table headers with total amounts)
    private static final Pattern TABLE_HEADER_ROW_PATTERN = Pattern.compile(
            "(?i)\\b(?:qty|quantity|particulars|description|unit\\s*price|rate|pack|hsn|disc|batch|exp|sr\\.?|sr\\s*no)\\b"
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
                .replaceAll("^(?i)to[\\s:]+", "")
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
            String rawVal = highPriorityMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, true)) {
                foundHighPriority = parsed;
            }
        }
        if (foundHighPriority != null) {
            return foundHighPriority;
        }

        // 2. Explicit payment action pattern ("Payment of 220 completed", "Paid 450", "Paid to ... ₹500")
        Matcher paymentMatcher = PAYMENT_ACTION_PATTERN.matcher(rawText);
        BigDecimal foundPayment = null;
        while (paymentMatcher.find()) {
            String rawVal = paymentMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, true)) {
                foundPayment = parsed;
            }
        }
        if (foundPayment != null) {
            return foundPayment;
        }

        // 3a. Standard total labels on the same line (Total: 450.00, Amount: 850)
        // Skip lines that look like table column headers (e.g. QTY RATE AMOUNT)
        Matcher sameLineMatcher = STANDARD_TOTAL_SAME_LINE_PATTERN.matcher(rawText);
        BigDecimal foundSameLine = null;
        while (sameLineMatcher.find()) {
            String fullMatchLine = getLineContaining(rawText, sameLineMatcher.start());
            if (isTableHeaderLine(fullMatchLine)) {
                continue;
            }
            String rawVal = sameLineMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                foundSameLine = parsed;
            }
        }
        if (foundSameLine != null) {
            return foundSameLine;
        }

        // 3b. Standalone total label on its own line followed by amount on next line
        Matcher nextLineMatcher = STANDALONE_TOTAL_LABEL_NEXT_LINE_PATTERN.matcher(rawText);
        BigDecimal foundNextLine = null;
        while (nextLineMatcher.find()) {
            String rawVal = nextLineMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                foundNextLine = parsed;
            }
        }
        if (foundNextLine != null) {
            return foundNextLine;
        }

        // 4. Standalone line with an amount in payment/receipt apps (e.g. Google Pay, PhonePe UPI screenshots)
        BigDecimal standaloneAmount = extractStandaloneLineAmount(rawText);
        if (standaloneAmount != null) {
            return standaloneAmount;
        }

        // 5. Currency-prefixed amounts (e.g. ₹1,250.00, Rs. 450, $25)
        Matcher currencyMatcher = CURRENCY_PREFIXED_PATTERN.matcher(rawText);
        BigDecimal maxCurrencyAmount = null;
        while (currencyMatcher.find()) {
            String rawVal = currencyMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                if (maxCurrencyAmount == null || parsed.compareTo(maxCurrencyAmount) > 0) {
                    maxCurrencyAmount = parsed;
                }
            }
        }
        if (maxCurrencyAmount != null) {
            return maxCurrencyAmount;
        }

        // 6. Currency-suffixed amounts (e.g. 450/-, 1200 INR)
        Matcher suffixMatcher = CURRENCY_SUFFIXED_PATTERN.matcher(rawText);
        BigDecimal maxSuffixAmount = null;
        while (suffixMatcher.find()) {
            String rawVal = suffixMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                if (maxSuffixAmount == null || parsed.compareTo(maxSuffixAmount) > 0) {
                    maxSuffixAmount = parsed;
                }
            }
        }
        if (maxSuffixAmount != null) {
            return maxSuffixAmount;
        }

        // 7. Line item table column amounts (pick the largest valid item line amount)
        Matcher tableRowMatcher = TABLE_ROW_AMOUNT_PATTERN.matcher(rawText);
        BigDecimal maxTableRowAmount = null;
        while (tableRowMatcher.find()) {
            String fullLine = getLineContaining(rawText, tableRowMatcher.start());
            if (isTableHeaderLine(fullLine)) {
                continue;
            }
            String rawVal = tableRowMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                if (maxTableRowAmount == null || parsed.compareTo(maxTableRowAmount) > 0) {
                    maxTableRowAmount = parsed;
                }
            }
        }
        if (maxTableRowAmount != null) {
            return maxTableRowAmount;
        }

        // 8. Fallback: search for standard decimal amounts and pick the largest valid monetary amount
        Matcher fallbackMatcher = FALLBACK_AMOUNT_PATTERN.matcher(rawText);
        BigDecimal maxFallback = null;
        while (fallbackMatcher.find()) {
            String fullLine = getLineContaining(rawText, fallbackMatcher.start());
            if (isTableHeaderLine(fullLine)) {
                continue;
            }
            String rawVal = fallbackMatcher.group(1);
            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                if (maxFallback == null || parsed.compareTo(maxFallback) > 0) {
                    maxFallback = parsed;
                }
            }
        }

        return maxFallback;
    }

    private boolean isTableHeaderLine(String line) {
        if (line == null || line.isBlank()) return false;
        return TABLE_HEADER_ROW_PATTERN.matcher(line).find();
    }

    private String getLineContaining(String text, int charIndex) {
        if (text == null || charIndex < 0 || charIndex >= text.length()) return "";
        int start = text.lastIndexOf('\n', charIndex);
        int end = text.indexOf('\n', charIndex);
        if (start < 0) start = 0; else start += 1;
        if (end < 0) end = text.length();
        return text.substring(start, end);
    }

    private BigDecimal extractStandaloneLineAmount(String rawText) {
        String lower = rawText.toLowerCase(Locale.ROOT);
        boolean isPaymentContext = lower.contains("completed") || lower.contains("successful")
                || lower.contains("payment") || lower.contains("paid") || lower.contains("upi")
                || lower.contains("transfer") || lower.contains("debited") || lower.contains("to ")
                || lower.contains("receipt") || lower.contains("invoice");

        if (!isPaymentContext) {
            return null;
        }

        Matcher matcher = STANDALONE_LINE_AMOUNT_PATTERN.matcher(rawText);
        BigDecimal bestCandidate = null;

        while (matcher.find()) {
            String rawVal = matcher.group(1);
            if (rawVal == null || rawVal.isBlank()) continue;

            BigDecimal parsed = parseAmount(rawVal);
            if (parsed != null && isValidMonetaryAmount(parsed, rawVal, rawText, false)) {
                // If it's a prominent standalone amount (e.g. 20 or 220), prefer positive amounts <= 1,000,000
                if (parsed.compareTo(BigDecimal.valueOf(1_000_000)) <= 0) {
                    bestCandidate = parsed;
                    break;
                }
            }
        }

        return bestCandidate;
    }

    private boolean isValidMonetaryAmount(BigDecimal amount, String rawVal, String rawText, boolean isExplicitTotal) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        long valLong = amount.longValue();

        // Reject 10-digit phone numbers
        if (valLong >= 6000000000L && valLong <= 9999999999L) {
            return false;
        }

        // Reject 11+ digit reference numbers / UPI transaction IDs (e.g. 627439970934)
        if (valLong >= 10000000000L) {
            return false;
        }

        // If explicit total or payment keyword matched, it is verified as a real total amount
        if (isExplicitTotal) {
            return true;
        }

        // Reject 6-digit pin codes if pure integer (no decimal dot, no commas, length 6)
        if (rawVal != null && !rawVal.contains(".") && !rawVal.contains(",") && valLong >= 100000L && valLong <= 999999L) {
            return false;
        }

        // Reject 4-digit years like 2015 to 2035 if integer with no decimal dot
        if (rawVal != null && !rawVal.contains(".") && valLong >= 2015 && valLong <= 2035) {
            return false;
        }

        // Reject bank account fragments if rawText indicates bank account (e.g. "Bank 8755" or "A/c 8755")
        if (rawVal != null && !rawVal.contains(".")) {
            String intStr = String.valueOf(valLong);
            Pattern bankAccPattern = Pattern.compile("(?i)(?:bank|a\\/?c|account|card|ending\\s*in)\\s*(?:no\\.?)?\\s*[:#]?[ \\t]*" + Pattern.quote(intStr));
            if (bankAccPattern.matcher(rawText).find()) {
                return false;
            }
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

        // 3. Check Text date format (e.g. 21 Aug 2026 or 1 Oct 2026)
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
