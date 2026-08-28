package com.heet.backend.service.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class OcrDataExtractorTest {

    private OcrDataExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new OcrDataExtractor();
    }

    @Test
    void extractMerchant_WithGenericHeaders_ExtractsActualStoreName() {
        String receiptText = """
                TAX INVOICE
                OFFICIAL RECEIPT
                Kwality Dine Restaurant
                123 Main Street
                Item 1: 10.00
                Total: 10.00
                """;

        String merchant = extractor.extractMerchant(receiptText);
        assertEquals("Kwality Dine Restaurant", merchant);
    }

    @Test
    void extractMerchant_FirstLineIsStore() {
        String receiptText = """
                Starbucks Coffee
                Store #1042
                Coffee: $4.50
                Total: $4.50
                """;

        String merchant = extractor.extractMerchant(receiptText);
        assertEquals("Starbucks Coffee", merchant);
    }

    @Test
    void extractMerchant_NullOrEmpty_ReturnsNull() {
        assertNull(extractor.extractMerchant(null));
        assertNull(extractor.extractMerchant(""));
        assertNull(extractor.extractMerchant("   \n\n  "));
    }

    @Test
    void extractAmount_WithKeywords_ExtractsCorrectAmount() {
        String receipt1 = "Items: 12.00, 15.00\nGrand Total: $27.00\nThank you";
        assertEquals(new BigDecimal("27.00"), extractor.extractAmount(receipt1));

        String receipt2 = "Subtotal: 100.00\nTAX: 18.00\nTotal Amount: INR 118.00";
        assertEquals(new BigDecimal("118.00"), extractor.extractAmount(receipt2));

        String receipt3 = "Item 1: 5.50\nAmount Due: 5.50";
        assertEquals(new BigDecimal("5.50"), extractor.extractAmount(receipt3));
    }

    @Test
    void extractAmount_FallbackWithoutKeywords_PicksLargestAmount() {
        String receipt = "Line 1: 15.50\nLine 2: 45.00\nLine 3: 10.25";
        assertEquals(new BigDecimal("45.00"), extractor.extractAmount(receipt));
    }

    @Test
    void extractAmount_NullOrEmpty_ReturnsNull() {
        assertNull(extractor.extractAmount(null));
        assertNull(extractor.extractAmount("No amounts here"));
    }

    @Test
    void extractDate_IsoFormat_Success() {
        String receipt = "Store ABC\nDate: 2026-08-21\nTotal: 50.00";
        assertEquals(LocalDate.of(2026, 8, 21), extractor.extractDate(receipt));
    }

    @Test
    void extractDate_DmyFormat_Success() {
        String receipt = "Store ABC\nDate: 21/08/2026\nTotal: 50.00";
        assertEquals(LocalDate.of(2026, 8, 21), extractor.extractDate(receipt));
    }

    @Test
    void extractDate_TextFormat_Success() {
        String receipt = "Store ABC\nDate: 21 Aug 2026\nTotal: 50.00";
        assertEquals(LocalDate.of(2026, 8, 21), extractor.extractDate(receipt));
    }

    @Test
    void extractDate_NullOrEmpty_ReturnsNull() {
        assertNull(extractor.extractDate(null));
        assertNull(extractor.extractDate("No dates here"));
    }
}
