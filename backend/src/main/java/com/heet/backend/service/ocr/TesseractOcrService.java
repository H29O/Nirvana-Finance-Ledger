package com.heet.backend.service.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
public class TesseractOcrService implements OcrService {

    private final String datapath;
    private final String language;
    private final OcrDataExtractor ocrDataExtractor;
    private final ImagePreprocessor imagePreprocessor;

    public TesseractOcrService(
            @Value("${app.ocr.tesseract.datapath:tessdata}") String datapath,
            @Value("${app.ocr.tesseract.language:eng}") String language,
            OcrDataExtractor ocrDataExtractor,
            ImagePreprocessor imagePreprocessor) {
        this.datapath = datapath;
        this.language = language;
        this.ocrDataExtractor = ocrDataExtractor;
        this.imagePreprocessor = imagePreprocessor;
    }

    @Override
    public OcrResult extractTextAndData(File file, String contentType) {
        if (file == null || !file.exists()) {
            return OcrResult.failure("Receipt file not found on disk");
        }

        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return OcrResult.failure("PDF OCR is currently not supported. Please upload an image (JPEG/PNG) to perform OCR.");
        }

        try {
            ITesseract tesseract = new Tesseract();
            if (datapath != null && !datapath.isBlank()) {
                File dataDir = new File(datapath);
                if (dataDir.exists()) {
                    tesseract.setDatapath(dataDir.getAbsolutePath());
                }
            }
            if (language != null && !language.isBlank()) {
                tesseract.setLanguage(language);
            }

            // Preprocess image for enhanced sharpness and contrast
            BufferedImage preprocessed = imagePreprocessor.preprocess(file);
            String rawText = preprocessed != null ? tesseract.doOCR(preprocessed) : tesseract.doOCR(file);

            if (rawText == null || rawText.isBlank()) {
                // If preprocessed returned nothing, try raw file directly
                rawText = tesseract.doOCR(file);
            }

            if (rawText == null || rawText.isBlank()) {
                return OcrResult.failure("No text could be extracted from image");
            }

            String merchant = ocrDataExtractor.extractMerchant(rawText);
            BigDecimal amount = ocrDataExtractor.extractAmount(rawText);
            LocalDate date = ocrDataExtractor.extractDate(rawText);

            return OcrResult.success(rawText.trim(), merchant, amount, date);
        } catch (TesseractException ex) {
            log.warn("Tesseract OCR extraction failed: {}", ex.getMessage());
            return OcrResult.failure("OCR engine failed: " + ex.getMessage());
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            log.warn("Tesseract native libraries not available: {}", ex.getMessage());
            return OcrResult.failure("OCR native library (Tesseract/Leptonica) is not available or not configured in environment.");
        } catch (Exception ex) {
            log.error("Unexpected error during OCR processing: {}", ex.getMessage(), ex);
            return OcrResult.failure("Unexpected OCR error: " + ex.getMessage());
        }
    }
}
