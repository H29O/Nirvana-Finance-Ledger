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
    void extractMerchant_WithNoisyTaxHeaderAndGst_ExtractsPharmacy() {
        String receiptText = """
                *STAX INVOICE* + GST No: 27AAAAA0000A1Z5
                JAN SEWA PHARMACY
                Plot 42, Civil Lines Road
                Rx: Azithromycin
                Net Payable: 450.00
                """;

        String merchant = extractor.extractMerchant(receiptText);
        assertEquals("JAN SEWA PHARMACY", merchant);
    }

    @Test
    void extractMerchant_WithGstinAndInvoiceNo_ExtractsBusinessName() {
        String receiptText = """
                GSTIN: 07ABCDE1234F1Z5
                INVOICE NO: INV-2026-9812
                DATE: 2026-08-30
                APOLLO PHARMACEUTICALS LTD
                Road No 3, Bangalore
                Total Amount: 1,250.00
                """;

        String merchant = extractor.extractMerchant(receiptText);
        assertEquals("APOLLO PHARMACEUTICALS LTD", merchant);
    }

    @Test
    void extractMerchant_MigliorPharmaceuticals_ExtractsCleanMerchantName() {
        String receiptText = """
                TAX INVOICE
                MIGLIOR PHARMACEUTICALS INDIA PVT.LTD
                Plot No. 44, Industrial Area, Sector 5
                GSTIN: 27AABCM1234F1Z9
                DL.NO: 20B/21B-4589
                """;

        String merchant = extractor.extractMerchant(receiptText);
        assertEquals("MIGLIOR PHARMACEUTICALS INDIA PVT.LTD", merchant);
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
    void extractAmount_IndianFormats_IntegerAndCommas() {
        String receipt1 = "Apollo Pharmacy\nItem 1: 200.00\nItem 2: 250.00\nNet Payable: ₹450";
        assertEquals(new BigDecimal("450.00"), extractor.extractAmount(receipt1));

        String receipt2 = "DMart Supermarket\nBill Total: ₹1,250.00";
        assertEquals(new BigDecimal("1250.00"), extractor.extractAmount(receipt2));

        String receipt3 = "Electricity Bill\nAmount Due: ₹1,250";
        assertEquals(new BigDecimal("1250.00"), extractor.extractAmount(receipt3));

        String receipt4 = "Jewellery Invoice\nGrand Total: ₹1,25,000.00";
        assertEquals(new BigDecimal("125000.00"), extractor.extractAmount(receipt4));

        String receipt5 = "Kirana Store\nNet Amount: 450\nThank You";
        assertEquals(new BigDecimal("450.00"), extractor.extractAmount(receipt5));

        String receipt6 = "Restaurant Bill\nFood: 750.00\nBill Total: 850.00";
        assertEquals(new BigDecimal("850.00"), extractor.extractAmount(receipt6));

        String receipt7 = "Medical Bill\nMRP: 500.00\nDiscount: 50.00\nAmount Payable : 450.00 /=";
        assertEquals(new BigDecimal("450.00"), extractor.extractAmount(receipt7));
    }

    @Test
    void extractAmount_PrioritizesNetPayableOverIndividualLineItems() {
        String receipt = """
                JAN SEWA PHARMACY
                Item A: 120.00
                Item B: 85.00
                Item C: 245.00
                Subtotal: 450.00
                Round off: 0.00
                Net Payable: 450.00
                Thank you visit again
                """;
        assertEquals(new BigDecimal("450.00"), extractor.extractAmount(receipt));
    }

    @Test
    void extractAmount_TableRowColumnAmount_ExtractsAmountFromItemRow() {
        String receipt = """
                MIGLIOR PHARMACEUTICALS INDIA PVT.LTD
                Plot 44, Ind Area
                QTY  RATE    AMOUNT
                1    450.00  450.00
                """;
        assertEquals(new BigDecimal("450.00"), extractor.extractAmount(receipt));
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
