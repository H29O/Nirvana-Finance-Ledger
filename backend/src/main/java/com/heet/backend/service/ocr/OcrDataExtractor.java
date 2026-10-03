package com.heet.backend.service.ocr;

import com.heet.backend.dto.response.OcrBlockDto;
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
            "store #", "store no", "cin:", "fssai", "dl no", "dl.no", "d.l.no", "tin no", "tel:", "tel no", "phone", "mob:", "ph:",
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

    // Tolerant regex for Grand Total and Net Payable variations (handles OCR typos e.g. Grand I Tot al, Grane Total, Grand! Tote uf, etc.)
    private static final Pattern GRAND_TOTAL_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:gran[de]|g)\\s*['\"!|lI1:_.-]*\\s*tot\\s*a?l\\s*[a-z0-9]*|" +
            "net\\s*(?:payab[a-z0-9]*|amount|amt)|" +
            "(?:amount|tot\\s*a?l)\\s*payab[a-z0-9]*|" +
            "payab[a-z0-9]*\\s*amount|" +
            "balance\\s*due|" +
            "final\\s*(?:amount|tot\\s*a?l)"
    );

    // Bill Total, Invoice Total, Total Amount, Amount (e.g. "amount": 850.00)
    private static final Pattern BILL_INVOICE_TOTAL_KEYWORD_PATTERN = Pattern.compile(
            "(?i)bill\\s*tot\\s*a?l\\s*[a-z0-9]*|" +
            "inv(?:oice)?\\s*tot\\s*a?l\\s*[a-z0-9]*|" +
            "tot\\s*a?l\\s*(?:amount|amt)|" +
            "bill\\s*amount|" +
            "gross\\s*amount|" +
            "taxable\\s*amount|" +
            "[\"']?amount[\"']?\\s*[:=]|" +
            "\\bamount\\b|\\bamt\\b"
    );

    // Payment action pattern (e.g. Paid via UPI, Payment of 220, Paid to ..., Amount Paid)
    private static final Pattern PAYMENT_ACTION_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:paid|ald)\\s*(?:via|by)?\\s*upi\\s*(?:total)?|" +
            "payment\\s+(?:of|amount)|" +
            "amount\\s+paid|" +
            "paid\\s+to|" +
            "debited|transferred"
    );

    // Subtotal keyword variations (e.g. Sub Total, Suk Toa, Suki Tokay, Sut' Tot a!)
    private static final Pattern SUBTOTAL_KEYWORD_PATTERN = Pattern.compile(
            "(?i)sub\\s*tot\\s*a?l|suk[il]?\\s*to(?:t|k)a[yl]|sut['’]?\\s*tot\\s*a?!"
    );

    // Standard total keyword on its own line or with amount (excluding 'total qty', 'total items', etc.)
    private static final Pattern STANDARD_TOTAL_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?<!(?:sub|tax|round[\\s-]*off)\\s*)\\btot\\s*a?l\\b(?!\\s*(?:qty|quantity|items?|count|pcs|pieces))"
    );

    // Tax keywords
    private static final Pattern TAX_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(?:tax|cgst|sgst|igst|vat|gst|service\\s*tax|cess)\\b"
    );

    // Discount keywords
    private static final Pattern DISCOUNT_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(?:discount|disc|off|saving|coupon|promo)\\b"
    );

    // Item line detector: identifies item rows by food/retail terms, quantities or table formatting
    private static final Pattern ITEM_LINE_PATTERN = Pattern.compile(
            "(?i)\\b(?:qty|misal|pav|softdrink|softurink|thali|tikka|soda|coffee|ticket|popcorn|burger|pizza|diner|petrol|diesel|course|item)\\b|" +
            "^\\s*\\d+\\s*[=xX.-]|\\b\\d+\\s+@\\s+|\\b\\d+\\s+[0-9]{2,4}\\.[0-9]{2}\\b"
    );

    // Table header detector
    private static final Pattern TABLE_HEADER_ROW_PATTERN = Pattern.compile(
            "(?i)\\b(?:qty|quantity|particulars|description|unit\\s*price|rate|pack|hsn|batch|exp|sr\\.?|sr\\s*no)\\b"
    );

    // High-priority explicit total labels with inline amount (strict fallback)
    private static final Pattern HIGH_PRIORITY_TOTAL_PATTERN = Pattern.compile(
            "(?i)[\"']?(?:grand\\s*tot[a-z0-9]*|net\\s*payab[a-z0-9]*|tot[a-z0-9]*\\s*payab[a-z0-9]*|amount\\s*payab[a-z0-9]*|payab[a-z0-9]*\\s*amount|tot[a-z0-9]*\\s*amount|tot[a-z0-9]*\\s*amt|bill\\s*tot[a-z0-9]*|bill\\s*amount|inv(?:oice)?\\s*tot[a-z0-9]*|net\\s*amount|net\\s*amt|balance\\s*due|amount\\s*due|final\\s*amount|final\\s*tot[a-z0-9]*|tot[a-z0-9]*\\s*due|tot[a-z0-9]*\\s*charges|gross\\s*amount|taxable\\s*amount)[\"']?[ \\t:=.\\-|~_#]*(?:\\r?\\n[ \\t]*)?(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Payment action pattern
    private static final Pattern PAYMENT_ACTION_PATTERN = Pattern.compile(
            "(?i)[\"‘“']?(?:payment\\s+(?:of|amount)|amount\\s+paid|paid\\s+to|paid|sent|debited|transferred|upi)[ \\t:=.\\-|'\"‘“]*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Standard total & amount labels on the same line
    private static final Pattern STANDARD_TOTAL_SAME_LINE_PATTERN = Pattern.compile(
            "(?i)(?<!(?:sub|tax|round[\\s-]*off)\\s*)[\"']?(?:tot[a-z0-9]*|amount|amt|sub\\s*tot[a-z0-9]*|mrp)[\"']?(?!\\s*(?:qty|quantity|items?|count|pcs|pieces))[ \\t:=.\\-|~_#]+(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Standalone total label on its own line followed by amount on next line
    private static final Pattern STANDALONE_TOTAL_LABEL_NEXT_LINE_PATTERN = Pattern.compile(
            "(?im)^[ \\t]*[\"']?(?:grand\\s*tot[a-z0-9]*|tot[a-z0-9]*|amount|amt|sub\\s*tot[a-z0-9]*|net\\s*payab[a-z0-9]*|amount\\s*payab[a-z0-9]*)[\"']?[ \\t:=.\\-|~_#]*\\r?\\n[ \\t]*(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?"
    );

    // Standalone line containing only an amount (e.g. Google Pay / PhonePe)
    private static final Pattern STANDALONE_LINE_AMOUNT_PATTERN = Pattern.compile(
            "^\\s*[\"']?(?:(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*)?([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=]?)?[\"']?\\s*$",
            Pattern.MULTILINE
    );

    // Currency prefixed standalone amounts
    private static final Pattern CURRENCY_PREFIXED_PATTERN = Pattern.compile(
            "(?:inr|usd|rs\\.?|eur|gbp|\\$|₹)[ \\t]*([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)"
    );

    // Currency suffixed amounts
    private static final Pattern CURRENCY_SUFFIXED_PATTERN = Pattern.compile(
            "\\b([0-9]{1,3}(?:[ \\t]*,[ \\t]*[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)[ \\t]*(?:inr|rs\\.?|\\/[-=])",
            Pattern.CASE_INSENSITIVE
    );

    // Table line item amount
    private static final Pattern TABLE_ROW_AMOUNT_PATTERN = Pattern.compile(
            "(?m)(?:.*[ \\t]+)?([0-9]{1,3}(?:,[0-9]{2,3})*\\.[0-9]{2}|[0-9]+\\.[0-9]{2})[ \\t]*$"
    );

    // Fallback general monetary pattern
    private static final Pattern FALLBACK_AMOUNT_PATTERN = Pattern.compile(
            "\\b([0-9]{1,3}(?:,[0-9]{2,3})*\\.[0-9]{2}|[0-9]+\\.[0-9]{2})\\b"
    );

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile(
            "\\b(\\d{4})[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])\\b"
    );

    private static final Pattern DMY_DATE_PATTERN = Pattern.compile(
            "\\b(0?[1-9]|[12]\\d|3[01])[-/.](0?[1-9]|1[0-2])[-/.](20\\d{2}|\\d{2})\\b"
    );

    private static final Pattern TEXT_DATE_PATTERN = Pattern.compile(
            "(?i)\\b(0?[1-9]|[12]\\d|3[01])\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+(\\d{4})\\b"
    );

    public String extractMerchant(String rawText, List<OcrBlockDto> blocks) {
        if (blocks != null && !blocks.isEmpty()) {
            String spatialMerchant = extractMerchantFromBlocks(rawText, blocks);
            if (spatialMerchant != null) {
                return spatialMerchant;
            }
        }
        return extractMerchant(rawText);
    }

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
        String origLower = original.toLowerCase(Locale.ROOT);
        if (origLower.contains("store #") || origLower.contains("store no") || origLower.contains("store-") || normalized.matches(".*\\bstore\\s+\\d+.*")) {
            return true;
        }

        for (String nonMerchant : NON_MERCHANT_SUBSTRINGS) {
            if (normalized.contains(nonMerchant)) {
                return true;
            }
        }

        if (original.matches(".*\\b\\d{2}[-/.]\\d{2}[-/.]\\d{2,4}\\b.*") ||
            original.matches(".*\\b\\d{4}[-/.]\\d{2}[-/.]\\d{2}\\b.*")) {
            return true;
        }

        if (original.matches(".*\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:am|pm)?\\b.*") && original.length() < 25) {
            return true;
        }

        if (normalized.matches(".*\\b\\d{10}\\b.*") || normalized.matches(".*\\b\\d{12}\\b.*")) {
            return true;
        }

        int letterCount = 0;
        int digitCount = 0;
        for (char c : original.toCharArray()) {
            if (Character.isLetter(c)) letterCount++;
            else if (Character.isDigit(c)) digitCount++;
        }
        if (letterCount < 3) {
            return true;
        }
        if (digitCount > letterCount) {
            return true;
        }

        return false;
    }

    private int calculateMerchantScore(String original, String normalized, int lineIndex) {
        int score = 0;

        score += (12 - lineIndex) * 2;

        for (String kw : BUSINESS_KEYWORDS) {
            if (normalized.contains(kw)) {
                score += 35;
                break;
            }
        }

        if (original.matches(".*\\b(LTD|PVT|INC|CORP|CO)\\b.*") || original.matches(".*\\b(Pvt|Ltd|Inc|Corp|Co)\\b.*")) {
            score += 25;
        }

        if (original.equals(original.toUpperCase(Locale.ROOT)) && original.length() > 5) {
            score += 15;
        }

        if (original.length() >= 5 && original.length() <= 40) {
            score += 10;
        } else if (original.length() > 60) {
            score -= 15;
        }

        return score;
    }

    private String cleanMerchantName(String raw) {
        if (raw == null) return null;

        String cleaned = raw.replaceAll("^[^a-zA-Z0-9]+", "")
                .replaceAll("[^a-zA-Z0-9.)]+$", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Strip leading "To " (from UPI payment receipts like "To Store Name")
        cleaned = cleaned.replaceAll("(?i)^to\\s+", "").trim();

        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 80).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    public BigDecimal extractAmount(String rawText, List<OcrBlockDto> blocks) {
        if (blocks != null && !blocks.isEmpty()) {
            BigDecimal spatialAmount = extractReceiptAwareAmountFromBlocks(rawText, blocks);
            if (spatialAmount != null) {
                return spatialAmount;
            }
        }
        return extractAmount(rawText);
    }

    /**
     * Primary receipt-aware amount extractor.
     * Evaluates candidate monetary values across the receipt, considers proximity to total
     * keywords, vertical layout, arithmetic reconciliation, and OCR corruption recovery.
     */
    public BigDecimal extractAmount(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        // 1. Run receipt-aware candidate extraction with structural scoring & reconciliation
        BigDecimal receiptAwareAmount = extractReceiptAwareAmount(rawText);
        if (receiptAwareAmount != null) {
            return receiptAwareAmount;
        }

        // 2. High-priority explicit total labels fallback
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

        // 3. Payment action pattern
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

        // 4. Standard total labels on the same line
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

        // 5. Standalone total label on its own line followed by amount on next line
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

        // 6. Standalone line with an amount in payment/receipt apps
        BigDecimal standaloneAmount = extractStandaloneLineAmount(rawText);
        if (standaloneAmount != null) {
            return standaloneAmount;
        }

        // 7. Currency-prefixed amounts
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

        // 8. Currency-suffixed amounts
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

        // 9. Line item table column amounts
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

        // 10. Fallback: search for standard decimal amounts
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

    public static class AmountCandidate {
        private final BigDecimal amount;
        private double score;
        private final int lineIndex;
        private final String sourceLine;

        public AmountCandidate(BigDecimal amount, double score, int lineIndex, String sourceLine) {
            this.amount = amount;
            this.score = score;
            this.lineIndex = lineIndex;
            this.sourceLine = sourceLine;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public double getScore() {
            return score;
        }

        public void addScore(double delta) {
            this.score += delta;
        }

        public int getLineIndex() {
            return lineIndex;
        }

        public String getSourceLine() {
            return sourceLine;
        }
    }

    public AmountCandidate extractBestAmountCandidate(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        return evaluateReceiptAwareCandidate(rawText);
    }

    private BigDecimal extractReceiptAwareAmount(String rawText) {
        AmountCandidate cand = evaluateReceiptAwareCandidate(rawText);
        return cand != null ? cand.getAmount() : null;
    }

    private AmountCandidate evaluateReceiptAwareCandidate(String rawText) {
        String[] lines = rawText.split("\\r?\\n");
        int totalLines = lines.length;
        if (totalLines == 0) return null;

        List<BigDecimal> detectedItemAmounts = new ArrayList<>();
        BigDecimal detectedSubtotal = null;
        BigDecimal detectedTax = null;
        BigDecimal detectedDiscount = null;

        // Pass A: Identify line categories and collect item prices, subtotal, tax, discount
        for (int i = 0; i < totalLines; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || isTableHeaderLine(line)) continue;

            boolean isGrandTotal = GRAND_TOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isBillTotal = BILL_INVOICE_TOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isPayment = PAYMENT_ACTION_KEYWORD_PATTERN.matcher(line).find();
            boolean isSubtotal = SUBTOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isTotal = STANDARD_TOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isTax = TAX_KEYWORD_PATTERN.matcher(line).find();
            boolean isDiscount = DISCOUNT_KEYWORD_PATTERN.matcher(line).find();
            boolean isItem = ITEM_LINE_PATTERN.matcher(line).find();

            if (isItem && !isGrandTotal && !isBillTotal && !isPayment && !isSubtotal && !isTotal && !isTax && !isDiscount) {
                List<BigDecimal> lineNums = extractNumbersFromLine(line, rawText);
                if (!lineNums.isEmpty()) {
                    // In table line: usually the last number is the line item total
                    detectedItemAmounts.add(lineNums.get(lineNums.size() - 1));
                }
            } else if (isSubtotal && !isGrandTotal) {
                List<BigDecimal> lineNums = extractNumbersFromLine(line, rawText);
                if (!lineNums.isEmpty()) {
                    detectedSubtotal = lineNums.get(lineNums.size() - 1);
                }
            } else if (isTax && !isGrandTotal && !isTotal) {
                List<BigDecimal> lineNums = extractNumbersFromLine(line, rawText);
                if (!lineNums.isEmpty()) {
                    detectedTax = lineNums.get(lineNums.size() - 1);
                }
            } else if (isDiscount && !isGrandTotal && !isTotal) {
                List<BigDecimal> lineNums = extractNumbersFromLine(line, rawText);
                if (!lineNums.isEmpty()) {
                    detectedDiscount = lineNums.get(lineNums.size() - 1);
                }
            }
        }

        BigDecimal itemsSum = BigDecimal.ZERO;
        for (BigDecimal item : detectedItemAmounts) {
            itemsSum = itemsSum.add(item);
        }

        // Pass B: Score all candidate monetary values
        List<AmountCandidate> candidates = new ArrayList<>();
        Map<BigDecimal, Integer> frequencyMap = new HashMap<>();

        for (int i = 0; i < totalLines; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || isTableHeaderLine(line)) continue;

            String lowerLine = line.toLowerCase(Locale.ROOT);
            if (lowerLine.contains("share & split") || lowerLine.contains("split expense") ||
                lowerLine.contains("having issues") || lowerLine.contains("upi transaction id") ||
                lowerLine.contains("utr:") || lowerLine.contains("ref no")) {
                continue;
            }

            double relativePos = (double) i / Math.max(1, totalLines - 1);

            boolean isGrandTotal = GRAND_TOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isBillTotal = BILL_INVOICE_TOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isPayment = PAYMENT_ACTION_KEYWORD_PATTERN.matcher(line).find();
            boolean isSubtotal = SUBTOTAL_KEYWORD_PATTERN.matcher(line).find();
            boolean isTotal = STANDARD_TOTAL_KEYWORD_PATTERN.matcher(line).find() && !isGrandTotal && !isBillTotal && !isSubtotal;
            boolean isItem = ITEM_LINE_PATTERN.matcher(line).find() && !isGrandTotal && !isBillTotal && !isPayment && !isSubtotal && !isTotal;
            boolean isDiscount = DISCOUNT_KEYWORD_PATTERN.matcher(line).find();
            boolean isTax = TAX_KEYWORD_PATTERN.matcher(line).find();

            // Also check previous line for multiline labels (e.g. Grand Total on line i-1, amount on line i)
            boolean prevIsGrandTotal = i > 0 && GRAND_TOTAL_KEYWORD_PATTERN.matcher(lines[i - 1]).find();
            boolean prevIsBillTotal = i > 0 && BILL_INVOICE_TOTAL_KEYWORD_PATTERN.matcher(lines[i - 1]).find();
            boolean prevIsTotal = i > 0 && STANDARD_TOTAL_KEYWORD_PATTERN.matcher(lines[i - 1]).find();

            List<BigDecimal> lineAmounts = extractNumbersFromLine(line, rawText);

            for (BigDecimal val : lineAmounts) {
                if (val.compareTo(BigDecimal.ZERO) <= 0 || val.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
                    continue;
                }

                frequencyMap.put(val, frequencyMap.getOrDefault(val, 0) + 1);

                double score = 0.0;

                // 1. Keyword scoring
                if (isGrandTotal) {
                    score += 130.0;
                } else if (prevIsGrandTotal) {
                    score += 110.0;
                } else if (isBillTotal) {
                    score += 100.0;
                } else if (prevIsBillTotal) {
                    score += 85.0;
                } else if (isPayment) {
                    score += 90.0;
                } else if (isTotal) {
                    score += 75.0;
                } else if (prevIsTotal) {
                    score += 60.0;
                } else if (isSubtotal) {
                    score += 45.0;
                }

                // 2. Vertical position scoring: receipts place totals at the bottom
                if (relativePos >= 0.70) {
                    score += 35.0;
                } else if (relativePos >= 0.50) {
                    score += 15.0;
                } else if (relativePos < 0.25 && !isPayment) {
                    score -= 25.0;
                }

                // 3. Item row penalty
                if (isItem && !isGrandTotal && !isTotal) {
                    score -= 60.0;
                }

                // 4. Tax/Discount penalty (these are secondary components, not final grand total)
                if (isDiscount && !isGrandTotal && !isTotal) {
                    score -= 40.0;
                }
                if (isTax && !isGrandTotal && !isTotal) {
                    score -= 30.0;
                }

                // 5. Arithmetic reconciliation bonus
                if (itemsSum.compareTo(BigDecimal.ZERO) > 0 && itemsSum.setScale(2, RoundingMode.HALF_UP).equals(val.setScale(2, RoundingMode.HALF_UP))) {
                    score += 60.0;
                }
                if (detectedSubtotal != null && detectedTax != null) {
                    BigDecimal subPlusTax = detectedSubtotal.add(detectedTax).setScale(2, RoundingMode.HALF_UP);
                    if (subPlusTax.equals(val.setScale(2, RoundingMode.HALF_UP))) {
                        score += 60.0;
                    }
                }
                if (detectedSubtotal != null && detectedDiscount != null) {
                    BigDecimal subMinusDisc = detectedSubtotal.subtract(detectedDiscount).setScale(2, RoundingMode.HALF_UP);
                    if (subMinusDisc.equals(val.setScale(2, RoundingMode.HALF_UP))) {
                        score += 60.0;
                    }
                }

                candidates.add(new AmountCandidate(val, score, i, line));
            }
        }

        // Apply multi-mention consensus bonus
        for (AmountCandidate c : candidates) {
            int freq = frequencyMap.getOrDefault(c.amount, 1);
            if (freq >= 2) {
                c.score += 35.0;
            }
        }

        // Pick highest-scoring candidate with a positive score
        candidates.sort((c1, c2) -> Double.compare(c2.score, c1.score));

        if (!candidates.isEmpty() && candidates.get(0).score > 40.0) {
            return candidates.get(0);
        }

        return null;
    }

    private List<BigDecimal> extractNumbersFromLine(String line, String rawText) {
        List<BigDecimal> results = new ArrayList<>();
        if (line == null || line.isBlank()) return results;

        // Tokenize line and clean tokens
        String[] tokens = line.split("\\s+");
        for (String token : tokens) {
            String clean = normalizeAmountToken(token);
            if (clean == null || clean.isBlank()) continue;

            BigDecimal parsed = parseAmount(clean);
            if (parsed != null && isValidMonetaryAmount(parsed, clean, rawText, false)) {
                results.add(parsed);
            }
        }

        return results;
    }

    private String normalizeAmountToken(String token) {
        if (token == null) return null;
        String s = token.trim();

        // Strip leading non-digit characters except currency or OCR noise (e.g. "₹", "x", "T", "%", "&", "*", "?", "Rs.")
        s = s.replaceAll("^(?:[\"\'`~!@#$%^&*()_=+xXtT₹\\[\\]{}|;:]|inr|usd|rs\\.?|eur|gbp|\\$)+[ \\t:=]*", "");
        // Strip trailing non-digit chars (including trailing dots/commas like "2." or punctuation)
        s = s.replaceAll("[ \\t:=/\\\\\\-\"')\\]}.;,]+$", "");

        if (s.isEmpty()) return null;

        // Comma as decimal: e.g. "150,00" -> "150.00"
        if (s.matches("^\\d{1,5},\\d{2}$")) {
            s = s.replace(',', '.');
        }

        // Alphanumeric OCR character confusion near total: e.g. "LSO.00" -> "150.00", "l50.00" -> "150.00"
        if (s.matches("(?i)^[LIl|1][S5s0Oo][0Oo]\\.[0Oo]{2}$")) {
            s = s.replaceAll("(?i)[LIl|]", "1")
                 .replaceAll("(?i)S", "5")
                 .replaceAll("(?i)[Oo]", "0");
        }

        s = s.replace(",", "");
        return s;
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

        // Reject 11+ digit reference numbers / UPI transaction IDs
        if (valLong >= 10000000000L) {
            return false;
        }

        // If explicit total or payment keyword matched, it is verified as a real total amount
        if (isExplicitTotal) {
            return true;
        }

        // Reject 6-digit pin codes if pure integer
        if (rawVal != null && !rawVal.contains(".") && !rawVal.contains(",") && valLong >= 100000L && valLong <= 999999L) {
            return false;
        }

        // Reject 4-digit years like 2015 to 2035 if integer with no decimal dot
        if (rawVal != null && !rawVal.contains(".") && valLong >= 2015 && valLong <= 2035) {
            return false;
        }

        // Reject bank account fragments if rawText indicates bank account
        if (rawVal != null && !rawVal.contains(".")) {
            String intStr = String.valueOf(valLong);
            Pattern bankAccPattern = Pattern.compile("(?i)(?:bank|a\\/?c|account|card|ending\\s*in)\\s*(?:no\\.?)?\\s*[:#]?[ \\t]*" + Pattern.quote(intStr));
            if (bankAccPattern.matcher(rawText).find()) {
                return false;
            }
        }

        // Reject small standalone integers <= 10 (e.g. editor line numbers 1, 2, 4 or bullet points) unless explicit currency or total keyword
        if (rawVal != null && !rawVal.contains(".") && valLong <= 10) {
            boolean hasCurrency = rawVal.contains("₹") || rawVal.contains("$") || rawVal.toLowerCase(Locale.ROOT).contains("rs");
            if (!hasCurrency && !isExplicitTotal) {
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

    public LocalDate extractDate(String rawText, List<OcrBlockDto> blocks) {
        if (blocks != null && !blocks.isEmpty()) {
            LocalDate spatialDate = extractDateFromBlocks(rawText, blocks);
            if (spatialDate != null) {
                return spatialDate;
            }
        }
        return extractDate(rawText);
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

        // 2. Check DMY format (DD-MM-YYYY or DD-MM-YY)
        Matcher dmyMatcher = DMY_DATE_PATTERN.matcher(rawText);
        if (dmyMatcher.find()) {
            int day = Integer.parseInt(dmyMatcher.group(1));
            int month = Integer.parseInt(dmyMatcher.group(2));
            int year = Integer.parseInt(dmyMatcher.group(3));
            if (year < 100) {
                year += 2000;
            }
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

    private BigDecimal extractReceiptAwareAmountFromBlocks(String rawText, List<OcrBlockDto> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;

        double maxY = blocks.stream().mapToDouble(OcrBlockDto::getCenterY).max().orElse(1.0);
        if (maxY <= 0) maxY = 1.0;

        // Group blocks by line index
        Map<Integer, List<OcrBlockDto>> lineMap = new TreeMap<>();
        for (OcrBlockDto b : blocks) {
            lineMap.computeIfAbsent(b.getLineIndex(), k -> new ArrayList<>()).add(b);
        }

        Map<Integer, String> lineTextMap = new HashMap<>();
        Map<Integer, Boolean> isGrandTotalMap = new HashMap<>();
        Map<Integer, Boolean> isBillTotalMap = new HashMap<>();
        Map<Integer, Boolean> isPaymentMap = new HashMap<>();
        Map<Integer, Boolean> isSubtotalMap = new HashMap<>();
        Map<Integer, Boolean> isTotalMap = new HashMap<>();
        Map<Integer, Boolean> isItemMap = new HashMap<>();
        Map<Integer, Boolean> isTaxMap = new HashMap<>();
        Map<Integer, Boolean> isDiscountMap = new HashMap<>();

        List<BigDecimal> detectedItemAmounts = new ArrayList<>();
        BigDecimal detectedSubtotal = null;
        BigDecimal detectedTax = null;
        BigDecimal detectedDiscount = null;

        for (Map.Entry<Integer, List<OcrBlockDto>> entry : lineMap.entrySet()) {
            int lineIdx = entry.getKey();
            List<OcrBlockDto> lineBlocks = entry.getValue();
            lineBlocks.sort(Comparator.comparingDouble(OcrBlockDto::getCenterX));

            StringBuilder sb = new StringBuilder();
            for (OcrBlockDto b : lineBlocks) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(b.getText());
            }
            String lineStr = sb.toString();
            lineTextMap.put(lineIdx, lineStr);

            boolean isGrandTotal = GRAND_TOTAL_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isBillTotal = BILL_INVOICE_TOTAL_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isPayment = PAYMENT_ACTION_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isSubtotal = SUBTOTAL_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isTotal = STANDARD_TOTAL_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isTax = TAX_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isDiscount = DISCOUNT_KEYWORD_PATTERN.matcher(lineStr).find();
            boolean isItem = ITEM_LINE_PATTERN.matcher(lineStr).find();

            isGrandTotalMap.put(lineIdx, isGrandTotal);
            isBillTotalMap.put(lineIdx, isBillTotal);
            isPaymentMap.put(lineIdx, isPayment);
            isSubtotalMap.put(lineIdx, isSubtotal);
            isTotalMap.put(lineIdx, isTotal);
            isItemMap.put(lineIdx, isItem);
            isTaxMap.put(lineIdx, isTax);
            isDiscountMap.put(lineIdx, isDiscount);

            if (isItem && !isGrandTotal && !isBillTotal && !isPayment && !isSubtotal && !isTotal && !isTax && !isDiscount) {
                List<BigDecimal> nums = extractNumbersFromLine(lineStr, rawText);
                if (!nums.isEmpty()) {
                    detectedItemAmounts.add(nums.get(nums.size() - 1));
                }
            } else if (isSubtotal && !isGrandTotal) {
                List<BigDecimal> nums = extractNumbersFromLine(lineStr, rawText);
                if (!nums.isEmpty()) {
                    detectedSubtotal = nums.get(nums.size() - 1);
                }
            } else if (isTax && !isGrandTotal && !isTotal) {
                List<BigDecimal> nums = extractNumbersFromLine(lineStr, rawText);
                if (!nums.isEmpty()) {
                    detectedTax = nums.get(nums.size() - 1);
                }
            } else if (isDiscount && !isGrandTotal && !isTotal) {
                List<BigDecimal> nums = extractNumbersFromLine(lineStr, rawText);
                if (!nums.isEmpty()) {
                    detectedDiscount = nums.get(nums.size() - 1);
                }
            }
        }

        BigDecimal itemsSum = BigDecimal.ZERO;
        for (BigDecimal item : detectedItemAmounts) {
            itemsSum = itemsSum.add(item);
        }

        List<AmountCandidate> candidates = new ArrayList<>();
        Map<BigDecimal, Integer> frequencyMap = new HashMap<>();

        for (Map.Entry<Integer, List<OcrBlockDto>> entry : lineMap.entrySet()) {
            int lineIdx = entry.getKey();
            String lineStr = lineTextMap.getOrDefault(lineIdx, "");
            String lowerLine = lineStr.toLowerCase(Locale.ROOT);

            if (lowerLine.contains("share & split") || lowerLine.contains("split expense") ||
                lowerLine.contains("having issues") || lowerLine.contains("upi transaction id") ||
                lowerLine.contains("utr:") || lowerLine.contains("ref no")) {
                continue;
            }

            boolean isGrandTotal = isGrandTotalMap.getOrDefault(lineIdx, false);
            boolean isBillTotal = isBillTotalMap.getOrDefault(lineIdx, false);
            boolean isPayment = isPaymentMap.getOrDefault(lineIdx, false);
            boolean isSubtotal = isSubtotalMap.getOrDefault(lineIdx, false);
            boolean isTotal = isTotalMap.getOrDefault(lineIdx, false) && !isGrandTotal && !isBillTotal && !isSubtotal;
            boolean isItem = isItemMap.getOrDefault(lineIdx, false) && !isGrandTotal && !isBillTotal && !isPayment && !isSubtotal && !isTotal;
            boolean isDiscount = isDiscountMap.getOrDefault(lineIdx, false);
            boolean isTax = isTaxMap.getOrDefault(lineIdx, false);

            boolean prevIsGrandTotal = isGrandTotalMap.getOrDefault(lineIdx - 1, false);
            boolean prevIsBillTotal = isBillTotalMap.getOrDefault(lineIdx - 1, false);
            boolean prevIsTotal = isTotalMap.getOrDefault(lineIdx - 1, false);

            for (OcrBlockDto block : entry.getValue()) {
                List<BigDecimal> blockNums = extractNumbersFromLine(block.getText(), rawText);
                for (BigDecimal val : blockNums) {
                    if (val.compareTo(BigDecimal.ZERO) <= 0 || val.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
                        continue;
                    }

                    frequencyMap.put(val, frequencyMap.getOrDefault(val, 0) + 1);

                    double score = 0.0;
                    double relativeY = block.getCenterY() / maxY;

                    if (isGrandTotal) {
                        score += 140.0;
                    } else if (prevIsGrandTotal) {
                        score += 110.0;
                    } else if (isBillTotal) {
                        score += 100.0;
                    } else if (prevIsBillTotal) {
                        score += 85.0;
                    } else if (isPayment) {
                        score += 90.0;
                    } else if (isTotal) {
                        score += 75.0;
                    } else if (prevIsTotal) {
                        score += 60.0;
                    } else if (isSubtotal) {
                        score += 45.0;
                    }

                    // Spatial Y position: totals reside near bottom
                    if (relativeY >= 0.70) {
                        score += 35.0;
                    } else if (relativeY >= 0.50) {
                        score += 15.0;
                    } else if (relativeY < 0.25 && !isPayment) {
                        score -= 30.0;
                    }

                    // Penalties
                    if (isItem && !isGrandTotal && !isTotal) {
                        score -= 60.0;
                    }
                    if (isDiscount && !isGrandTotal && !isTotal) {
                        score -= 40.0;
                    }
                    if (isTax && !isGrandTotal && !isTotal) {
                        score -= 30.0;
                    }

                    // Arithmetic reconciliation
                    if (itemsSum.compareTo(BigDecimal.ZERO) > 0 && itemsSum.setScale(2, RoundingMode.HALF_UP).equals(val.setScale(2, RoundingMode.HALF_UP))) {
                        score += 60.0;
                    }
                    if (detectedSubtotal != null && detectedTax != null) {
                        BigDecimal subPlusTax = detectedSubtotal.add(detectedTax).setScale(2, RoundingMode.HALF_UP);
                        if (subPlusTax.equals(val.setScale(2, RoundingMode.HALF_UP))) {
                            score += 60.0;
                        }
                    }
                    if (detectedSubtotal != null && detectedDiscount != null) {
                        BigDecimal subMinusDisc = detectedSubtotal.subtract(detectedDiscount).setScale(2, RoundingMode.HALF_UP);
                        if (subMinusDisc.equals(val.setScale(2, RoundingMode.HALF_UP))) {
                            score += 60.0;
                        }
                    }

                    // Confidence weighting
                    if (block.getConfidence() > 0) {
                        score += (block.getConfidence() - 0.5) * 15.0;
                    }

                    candidates.add(new AmountCandidate(val, score, lineIdx, lineStr));
                }
            }
        }

        // Multi-mention consensus
        for (AmountCandidate c : candidates) {
            int freq = frequencyMap.getOrDefault(c.getAmount(), 1);
            if (freq >= 2) {
                c.addScore(35.0);
            }
        }

        candidates.sort((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()));
        if (!candidates.isEmpty() && candidates.get(0).getScore() > 40.0) {
            return candidates.get(0).getAmount();
        }

        return null;
    }

    private String extractMerchantFromBlocks(String rawText, List<OcrBlockDto> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;

        Map<Integer, List<OcrBlockDto>> lineMap = new TreeMap<>();
        for (OcrBlockDto b : blocks) {
            lineMap.computeIfAbsent(b.getLineIndex(), k -> new ArrayList<>()).add(b);
        }

        String bestCandidate = null;
        int bestScore = -100;
        int linesToCheck = Math.min(lineMap.size(), 6);

        int count = 0;
        for (Map.Entry<Integer, List<OcrBlockDto>> entry : lineMap.entrySet()) {
            if (count++ >= linesToCheck) break;

            List<OcrBlockDto> lineBlocks = entry.getValue();
            lineBlocks.sort(Comparator.comparingDouble(OcrBlockDto::getCenterX));

            StringBuilder sb = new StringBuilder();
            for (OcrBlockDto b : lineBlocks) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(b.getText());
            }
            String line = sb.toString().trim();
            if (line.length() < 3) continue;

            String normalized = line.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (isNonMerchantLine(normalized, line)) continue;

            int score = calculateMerchantScore(line, normalized, entry.getKey());
            double avgConf = lineBlocks.stream().mapToDouble(OcrBlockDto::getConfidence).average().orElse(0.0);
            if (avgConf > 0.85) score += 15;

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = cleanMerchantName(line);
            }
        }

        return bestCandidate;
    }

    private LocalDate extractDateFromBlocks(String rawText, List<OcrBlockDto> blocks) {
        if (blocks == null || blocks.isEmpty()) return null;

        Map<Integer, List<OcrBlockDto>> lineMap = new TreeMap<>();
        for (OcrBlockDto b : blocks) {
            lineMap.computeIfAbsent(b.getLineIndex(), k -> new ArrayList<>()).add(b);
        }

        for (List<OcrBlockDto> lineBlocks : lineMap.values()) {
            lineBlocks.sort(Comparator.comparingDouble(OcrBlockDto::getCenterX));
            StringBuilder sb = new StringBuilder();
            for (OcrBlockDto b : lineBlocks) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(b.getText());
            }
            LocalDate parsed = extractDate(sb.toString());
            if (parsed != null) {
                return parsed;
            }
        }

        return extractDate(rawText);
    }
}
