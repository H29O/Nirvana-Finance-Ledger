package com.heet.backend.service.ocr;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ReceiptOcrInspectionTest {

    @Test
    void testPaddleOcrServiceHealth() {
        OcrDataExtractor extractor = new OcrDataExtractor();
        PaddleOcrService ocrService = new PaddleOcrService("http://127.0.0.1:5005", 30000, extractor);
        org.junit.jupiter.api.Assumptions.assumeTrue(ocrService.isServiceAvailable(), "PaddleOCR service is not running on port 5005");
        assertTrue(ocrService.isServiceAvailable(), "PaddleOCR service should be available on port 5005");
    }

    @Test
    void testEndToEndPaddleOcrOnUpiReceipt() {
        File file = new File("../uploads/receipts/3/upi_durga_receipt.png");
        if (!file.exists()) {
            file = new File("uploads/receipts/3/upi_durga_receipt.png");
        }
        if (!file.exists()) {
            System.out.println("Receipt file not found: " + file.getAbsolutePath());
            return;
        }

        OcrDataExtractor extractor = new OcrDataExtractor();
        PaddleOcrService ocrService = new PaddleOcrService("http://127.0.0.1:5005", 30000, extractor);
        org.junit.jupiter.api.Assumptions.assumeTrue(ocrService.isServiceAvailable(), "PaddleOCR service is not running on port 5005");

        OcrResult result = ocrService.extractTextAndData(file, "image/png");
        System.out.println("=== END TO END PADDLEOCR RESULT (UPI DURGA) ===");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Failure reason: " + result.getFailureReason());
        System.out.println("Merchant: " + result.getDetectedMerchant());
        System.out.println("Amount: " + result.getDetectedAmount());
        System.out.println("Date: " + result.getDetectedDate());
        System.out.println("Raw text:\n" + result.getRawText());

        assertTrue(result.isSuccess(), "OCR extraction should succeed");
        assertNotNull(result.getRawText(), "Raw text should not be null");
        assertEquals(0, new java.math.BigDecimal("20.00").compareTo(result.getDetectedAmount()), "Detected amount should be 20.00");
        assertNotNull(result.getDetectedMerchant(), "Detected merchant should not be null");
        assertTrue(result.getDetectedMerchant().toUpperCase().contains("DURGA"), "Detected merchant should contain DURGA");
    }

    @Test
    void testRealPhotographedReceipt_Extracts150() {
        File file = new File("../uploads/receipts/2/1c3d1042-bf32-47bc-833d-181fa20e019a.jpeg");
        if (!file.exists()) {
            file = new File("uploads/receipts/2/1c3d1042-bf32-47bc-833d-181fa20e019a.jpeg");
        }
        if (!file.exists()) {
            System.out.println("Receipt file not found: " + file.getAbsolutePath());
            return;
        }

        OcrDataExtractor extractor = new OcrDataExtractor();
        PaddleOcrService ocrService = new PaddleOcrService("http://127.0.0.1:5005", 30000, extractor);
        org.junit.jupiter.api.Assumptions.assumeTrue(ocrService.isServiceAvailable(), "PaddleOCR service is not running on port 5005");

        OcrResult result = ocrService.extractTextAndData(file, "image/jpeg");
        System.out.println("=== REAL PHOTOGRAPHED RECEIPT PADDLEOCR RESULT ===");
        System.out.println("SUCCESS: " + result.isSuccess());
        System.out.println("DETECTED MERCHANT: " + result.getDetectedMerchant());
        System.out.println("DETECTED AMOUNT: " + result.getDetectedAmount());
        System.out.println("DETECTED DATE: " + result.getDetectedDate());
        System.out.println("RAW TEXT:\n" + result.getRawText());

        assertTrue(result.isSuccess(), "OCR should succeed on photographed receipt");
        assertNotNull(result.getDetectedAmount(), "Detected amount should not be null");
        assertEquals(0, new java.math.BigDecimal("150.00").compareTo(result.getDetectedAmount()),
                "Detected amount should be exactly 150.00 instead of 130.6");
        assertNotNull(result.getDetectedMerchant(), "Merchant should be detected");
        assertNotNull(result.getDetectedDate(), "Date should be detected");
        assertEquals(java.time.LocalDate.of(2026, 10, 2), result.getDetectedDate());
    }

    @Test
    void testAllReceiptsInUploads() {
        OcrDataExtractor extractor = new OcrDataExtractor();
        PaddleOcrService ocrService = new PaddleOcrService("http://127.0.0.1:5005", 30000, extractor);

        File uploadsDir = new File("../uploads/receipts");
        if (!uploadsDir.exists()) {
            uploadsDir = new File("uploads/receipts");
        }
        if (!uploadsDir.exists()) {
            System.out.println("No uploads dir found");
            return;
        }

        try (Stream<Path> paths = Files.walk(uploadsDir.toPath())) {
            List<File> files = paths.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> f.getName().endsWith(".png") || f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg"))
                    .toList();

            for (File file : files) {
                System.out.println("\n==================================================");
                System.out.println("FILE: " + file.getName());
                OcrResult result = ocrService.extractTextAndData(file, file.getName().endsWith(".png") ? "image/png" : "image/jpeg");
                System.out.println("SUCCESS: " + result.isSuccess());
                System.out.println("DETECTED MERCHANT: " + result.getDetectedMerchant());
                System.out.println("DETECTED AMOUNT: " + result.getDetectedAmount());
                System.out.println("DETECTED DATE: " + result.getDetectedDate());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
