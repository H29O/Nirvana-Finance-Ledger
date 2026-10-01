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
    void testEndToEndTesseractServiceOnReceiptWithAutoDiscovery() {
        File file = new File("../uploads/receipts/3/upi_durga_receipt.png");
        if (!file.exists()) {
            file = new File("uploads/receipts/3/upi_durga_receipt.png");
        }
        if (!file.exists()) {
            System.out.println("Receipt file not found: " + file.getAbsolutePath());
            return;
        }

        ImagePreprocessor preprocessor = new ImagePreprocessor();
        OcrDataExtractor extractor = new OcrDataExtractor();

        TesseractOcrService ocrService = new TesseractOcrService(
                "",
                "",
                "eng",
                extractor,
                preprocessor
        );

        assertNotNull(ocrService.getResolvedDatapath(), "Datapath should be auto-detected");
        assertNotNull(ocrService.getResolvedLibPath(), "Libpath should be auto-detected");

        net.sourceforge.tess4j.Tesseract rawTess = new net.sourceforge.tess4j.Tesseract();
        rawTess.setDatapath(ocrService.getResolvedDatapath());
        rawTess.setVariable("user_defined_dpi", "300");
        rawTess.setPageSegMode(3);
        try {
            String directRaw = rawTess.doOCR(file);
            System.out.println("=== DIRECT RAW FILE OCR WITH 300 DPI ===");
            System.out.println(directRaw);
        } catch (Exception ignored) {}

        OcrResult result = ocrService.extractTextAndData(file, "image/png");
        System.out.println("=== END TO END RESULT ===");
        System.out.println("Success: " + result.isSuccess());
        System.out.println("Failure reason: " + result.getFailureReason());
        System.out.println("Merchant: " + result.getDetectedMerchant());
        System.out.println("Amount: " + result.getDetectedAmount());
        System.out.println("Date: " + result.getDetectedDate());
        System.out.println("Raw text:\n" + result.getRawText());

        assertTrue(result.isSuccess(), "OCR extraction should succeed");
        assertNotNull(result.getRawText(), "Raw text should not be null");
        assertEquals(new java.math.BigDecimal("20.00"), result.getDetectedAmount(), "Detected amount should be 20.00");
        assertNotNull(result.getDetectedMerchant(), "Detected merchant should not be null");
        assertTrue(result.getDetectedMerchant().contains("DURGA"), "Detected merchant should contain DURGA");
    }

    @Test
    void testAllReceiptsInUploads() {
        ImagePreprocessor preprocessor = new ImagePreprocessor();
        OcrDataExtractor extractor = new OcrDataExtractor();
        TesseractOcrService ocrService = new TesseractOcrService("", "", "eng", extractor, preprocessor);

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
                    .filter(f -> f.getName().endsWith(".png") || f.getName().endsWith(".jpg"))
                    .toList();

            for (File file : files) {
                System.out.println("\n==================================================");
                System.out.println("FILE: " + file.getName());
                OcrResult result = ocrService.extractTextAndData(file, "image/png");
                System.out.println("SUCCESS: " + result.isSuccess());
                System.out.println("DETECTED MERCHANT: " + result.getDetectedMerchant());
                System.out.println("DETECTED AMOUNT: " + result.getDetectedAmount());
                System.out.println("DETECTED DATE: " + result.getDetectedDate());
                System.out.println("RAW TEXT:\n" + result.getRawText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
