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
    void extractAmount_UpiPaymentScreenshots_ExtractsAmountSuccessfully() {
        // Direct Google Pay / PhonePe text layout
        String gpayText = """
                4:28
                To SHREE MAA DURGA SUPER MARKET
                Completed
                1 Oct 2026, 4:16pm
                Bandhan Bank 8755
                Payment of ₹20 completed
                UPI transaction ID: 627439970934
                """;
        assertEquals(new BigDecimal("20.00"), extractor.extractAmount(gpayText));
        assertEquals("SHREE MAA DURGA SUPER MARKET", extractor.extractMerchant(gpayText));

        String phonePeText = """
                Transaction Successful
                Paid to Kwality Dine
                ₹450
                Debited from Bank 1234
                UTR: 123456789012
                """;
        assertEquals(new BigDecimal("450.00"), extractor.extractAmount(phonePeText));

        String standaloneUpi = """
                To Supermarket
                220
                Completed
                UPI ID: 9876543210@upi
                """;
        assertEquals(new BigDecimal("220.00"), extractor.extractAmount(standaloneUpi));
    }

    @Test
    void extractAmount_JsonPayloadScreenshots_ExtractsAmountCorrectly() {
        String postmanText = """
                POST http://localhost:2906/api/transactions
                {
                   "amount": 850.00,
                   "transactionType": "EXPENSE"
                }
                """;
        assertEquals(new BigDecimal("850.00"), extractor.extractAmount(postmanText));
    }

    @Test
    void extractAmount_MultilineTotalLabel_ExtractsAmount() {
        String multiline = """
                Grand Total
                ₹ 1,500.00
                Thank you!
                """;
        assertEquals(new BigDecimal("1500.00"), extractor.extractAmount(multiline));
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
    void testExactDurgaPsm4() {
        String t = "4:28 ©) ott) 5G+ ED\n\n< Oo :\n\nTo SHREE MAA DURGA SUPER MARKET\n\n220\n\n© Completed\n\n1 Oct 2026, 4:16pm\n\nQ) Bandhan Bank 8755 v\n\n‘ Payment of 20 completed\n\nReceiver's bank has confirmed deposit of\nmoney to SHREE MAA DURGA SUPER\nMARKET's bank account\n\nUPI transaction ID\n627439970934\n\nTo: SHREE MAA DURGA SUPER MARKET\n\nane emANIAHA,\n\n@ Having issues? << Share & Split expense\n";
        BigDecimal a = extractor.extractAmount(t);
        System.out.println("EXACT DURGA WITH SPACE AMOUNT: " + a);
        assertEquals(new BigDecimal("20.00"), a);
    }

    @Test
    void testCase1_CurrentFailure_ExtractsGrandTotalOverItems() {
        String receiptText = """
                HOTEL SHREE KRISHNA
                Unlimited Misal 130.00
                Softdrink Rs. 20.00
                Total 150.00
                Grand Total 150.00
                Thank You Visit Again
                """;
        assertEquals(new BigDecimal("150.00"), extractor.extractAmount(receiptText));
    }

    @Test
    void testCase2_ItemLargerThanTotal_DoesNotSimplyPickLargestNumber() {
        String receiptText = """
                ELECTRONICS STORE
                Headphones 800.00
                Store Voucher -600.00
                Tax 50.00
                Total Amount: 250.00
                Paid by Card: 250.00
                """;
        // Even though 800.00 is the largest number, the total amount is 250.00
        assertEquals(new BigDecimal("250.00"), extractor.extractAmount(receiptText));
    }

    @Test
    void testCase3_MultipleItemPrices_ExtractsFinalReceiptTotal() {
        String receiptText = """
                GROCERY MART
                Milk: 30.00
                Bread: 40.00
                Eggs: 60.00
                Butter: 55.00
                Subtotal: 185.00
                Grand Total: 185.00
                """;
        assertEquals(new BigDecimal("185.00"), extractor.extractAmount(receiptText));
    }

    @Test
    void testCase4_Tax_ExtractsGrandTotalWithTax() {
        String receiptText = """
                CAFE NIRVANA
                Subtotal 130
                Tax 20
                Grand Total 150
                """;
        assertEquals(new BigDecimal("150.00"), extractor.extractAmount(receiptText));
    }

    @Test
    void testCase5_Discount_ExtractsGrandTotalWithDiscount() {
        String receiptText = """
                APPAREL STORE
                Subtotal 200
                Discount 50
                Grand Total 150
                """;
        assertEquals(new BigDecimal("150.00"), extractor.extractAmount(receiptText));
    }

    @Test
    void testCase7_PhotographedReceiptOcrCorruption_Extracts150() {
        // Raw OCR text resembling the real photograph with formatting artifacts
        String rawOcr = """
                Tax Invoice
                HOTEL NIRVANA
                Date: 02/10/26
                1 Unlimited Misal 130,00
                1 Softdrink 20.00
                Sub Total 150.00
                Grand I Tot al x 150.00
                """;
        assertEquals(new BigDecimal("150.00"), extractor.extractAmount(rawOcr));
        assertEquals(LocalDate.of(2026, 10, 2), extractor.extractDate(rawOcr));
    }

    @Test
    void testCase6_OcrSpellingCorruption_TotaI_GrandTota1() {
        String receipt1 = """
                RESTAURANT
                Item 1: 100.00
                TotaI: 100.00
                """;
        assertEquals(new BigDecimal("100.00"), extractor.extractAmount(receipt1));

        String receipt2 = """
                CAFE
                Item A: 120.00
                Grand Tota1: 120.00
                """;
        assertEquals(new BigDecimal("120.00"), extractor.extractAmount(receipt2));
    }

    @Test
    void testSpatialBlockExtraction_GrandTotalOnSameLineAcrossBlocks() {
        // Grand total label in one block, amount in another block on the same horizontal line
        com.heet.backend.dto.response.OcrBlockDto labelBlock = com.heet.backend.dto.response.OcrBlockDto.builder()
                .text("Grand Total")
                .confidence(0.99)
                .centerX(150.0)
                .centerY(500.0)
                .lineIndex(8)
                .build();

        com.heet.backend.dto.response.OcrBlockDto amountBlock = com.heet.backend.dto.response.OcrBlockDto.builder()
                .text("₹150.00")
                .confidence(0.98)
                .centerX(800.0)
                .centerY(502.0)
                .lineIndex(8)
                .build();

        com.heet.backend.dto.response.OcrBlockDto itemBlock = com.heet.backend.dto.response.OcrBlockDto.builder()
                .text("Misal 130.00")
                .confidence(0.95)
                .centerX(300.0)
                .centerY(350.0)
                .lineIndex(5)
                .build();

        String rawText = "HOTEL\nMisal 130.00\nGrand Total ₹150.00";
        BigDecimal amount = extractor.extractAmount(rawText, java.util.List.of(itemBlock, labelBlock, amountBlock));
        assertEquals(new BigDecimal("150.00"), amount);
    }

    @Test
    void testCategorySuggestion_FoodAndDining() {
        CategoryClassifier classifier = new CategoryClassifier();
        com.heet.backend.entity.Category diningCat = new com.heet.backend.entity.Category();
        diningCat.setId(1L);
        diningCat.setName("Food & Dining");

        com.heet.backend.entity.Category groceryCat = new com.heet.backend.entity.Category();
        groceryCat.setId(2L);
        groceryCat.setName("Groceries");

        var suggestion = classifier.suggestCategory(
                "Thorat's Barbeque Misal\nUnlimited Misal 130.00\nSoftdrink 20.00\nTotal 150.00",
                "Thorat's Barbeque Misal",
                java.util.List.of(diningCat, groceryCat)
        );

        assertNotNull(suggestion);
        assertEquals("Food & Dining", suggestion.categoryName());
    }

    @Test
    void extractDate_NullOrEmpty_ReturnsNull() {
        assertNull(extractor.extractDate(null));
        assertNull(extractor.extractDate("No dates here"));
    }
}
