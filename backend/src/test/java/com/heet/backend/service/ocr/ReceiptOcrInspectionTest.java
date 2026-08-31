package com.heet.backend.service.ocr;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiptOcrInspectionTest {

    @Test
    void testEndToEndTesseractServiceOnPharmacyReceipt() {
        File file = new File("E:/ReumeProject1/uploads/receipts/3/e5d97ad6-0af4-4361-8f2a-ffd7900a2a38.png");
        if (!file.exists()) {
            System.out.println("Receipt file not found");
            return;
        }

        ImagePreprocessor preprocessor = new ImagePreprocessor();
        OcrDataExtractor extractor = new OcrDataExtractor();
        TesseractOcrService ocrService = new TesseractOcrService(
                "C:/Program Files/Tesseract-OCR/tessdata",
                "eng",
                extractor,
                preprocessor
        );

        OcrResult result = ocrService.extractTextAndData(file, "image/png");
        System.out.println("=== END TO END RESULT ===");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Merchant: " + result.getDetectedMerchant());
        System.out.println("Amount: " + result.getDetectedAmount());
        System.out.println("Date: " + result.getDetectedDate());
        System.out.println("Raw text:\n" + result.getRawText());
    }
}
