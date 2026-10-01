package com.heet.backend.service.ocr;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Service
public class TesseractOcrService implements OcrService {

    @Getter
    private final String resolvedDatapath;
    @Getter
    private final String resolvedLibPath;
    @Getter
    private final String language;
    private final OcrDataExtractor ocrDataExtractor;
    private final ImagePreprocessor imagePreprocessor;

    public TesseractOcrService(
            String datapath,
            String language,
            OcrDataExtractor ocrDataExtractor,
            ImagePreprocessor imagePreprocessor) {
        this(datapath, null, language, ocrDataExtractor, imagePreprocessor);
    }

    @Autowired
    public TesseractOcrService(
            @Value("${app.ocr.tesseract.datapath:}") String configuredDatapath,
            @Value("${app.ocr.tesseract.libpath:}") String configuredLibpath,
            @Value("${app.ocr.tesseract.language:eng}") String language,
            OcrDataExtractor ocrDataExtractor,
            ImagePreprocessor imagePreprocessor) {
        this.language = (language != null && !language.isBlank()) ? language.trim() : "eng";
        this.ocrDataExtractor = ocrDataExtractor;
        this.imagePreprocessor = imagePreprocessor;

        this.resolvedLibPath = resolveLibPath(configuredLibpath);
        configureJnaLibraryPath(this.resolvedLibPath);

        this.resolvedDatapath = resolveDatapath(configuredDatapath, this.language);

        if (this.resolvedDatapath != null) {
            log.info("Initialized Tesseract OCR service [datapath='{}', libpath='{}', language='{}']",
                    this.resolvedDatapath, this.resolvedLibPath, this.language);
        } else {
            log.warn("Tesseract tessdata directory could not be resolved automatically. " +
                    "Configured='{}', libpath='{}'. Please verify TESSDATA_PREFIX or app.ocr.tesseract.datapath.",
                    configuredDatapath, this.resolvedLibPath);
        }
    }

    @Override
    public OcrResult extractTextAndData(File file, String contentType) {
        if (file == null || !file.exists()) {
            log.warn("OCR failed: Receipt file not found on disk: {}", file != null ? file.getAbsolutePath() : "null");
            return OcrResult.failure("Receipt file not found on disk");
        }

        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return OcrResult.failure("PDF OCR is currently not supported. Please upload an image (JPEG/PNG) to perform OCR.");
        }

        if (resolvedDatapath == null) {
            log.error("OCR extraction failed: Tesseract tessdata directory is not configured or could not be found.");
            return OcrResult.failure("OCR engine failed: tessdata directory not found");
        }

        try {
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath(resolvedDatapath);
            tesseract.setLanguage(language);
            try {
                tesseract.setVariable("user_defined_dpi", "300");
                tesseract.setPageSegMode(3);
            } catch (Throwable t) {
                log.debug("Could not set advanced Tesseract variables: {}", t.getMessage());
            }

            // Pass 1: Direct raw file OCR (preserves native digital pixels, colors, and fonts on phone screenshots)
            String rawText = null;
            try {
                rawText = tesseract.doOCR(file);
            } catch (Exception ex) {
                log.debug("Direct raw OCR attempt failed on '{}': {}", file.getName(), ex.getMessage());
            }

            String merchant = (rawText != null && !rawText.isBlank()) ? ocrDataExtractor.extractMerchant(rawText) : null;
            BigDecimal amount = (rawText != null && !rawText.isBlank()) ? ocrDataExtractor.extractAmount(rawText) : null;
            LocalDate date = (rawText != null && !rawText.isBlank()) ? ocrDataExtractor.extractDate(rawText) : null;

            // Pass 2: If rawText is blank or key fields (amount or merchant) are missing,
            // fall back to image preprocessing (upscaling & contrast optimization for faint paper receipts)
            if (rawText == null || rawText.isBlank() || amount == null || merchant == null) {
                try {
                    BufferedImage preprocessed = imagePreprocessor.preprocess(file);
                    if (preprocessed != null) {
                        String prepText = tesseract.doOCR(preprocessed);
                        if (prepText != null && !prepText.isBlank()) {
                            if (rawText == null || rawText.isBlank()) {
                                rawText = prepText;
                            }
                            if (merchant == null) {
                                merchant = ocrDataExtractor.extractMerchant(prepText);
                            }
                            if (amount == null) {
                                amount = ocrDataExtractor.extractAmount(prepText);
                            }
                            if (date == null) {
                                date = ocrDataExtractor.extractDate(prepText);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Secondary preprocessed OCR pass encountered error: {}", ex.getMessage());
                }
            }

            if (rawText == null || rawText.isBlank()) {
                log.info("No text could be extracted from image '{}'", file.getAbsolutePath());
                return OcrResult.failure("No text could be extracted from image");
            }

            log.info("OCR successfully processed file '{}': merchant='{}', amount={}, date={}",
                    file.getName(), merchant, amount, date);

            return OcrResult.success(rawText.trim(), merchant, amount, date);
        } catch (UnsatisfiedLinkError | NoClassDefFoundError ex) {
            log.error("Tesseract native libraries not available while processing file '{}': {}",
                    file.getAbsolutePath(), ex.getMessage(), ex);
            return OcrResult.failure("OCR native library (Tesseract/Leptonica) is not available or not configured in environment.");
        } catch (Exception ex) {
            log.error("Unexpected error during OCR processing for file '{}': {}", file.getAbsolutePath(), ex.getMessage(), ex);
            return OcrResult.failure("OCR engine failed: " + ex.getMessage());
        }
    }

    private String resolveLibPath(String configuredLibpath) {
        if (configuredLibpath != null && !configuredLibpath.isBlank()) {
            File dir = new File(configuredLibpath.trim());
            if (dir.exists() && dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        }

        String envLib = System.getenv("TESSERACT_LIBPATH");
        if (envLib != null && !envLib.isBlank()) {
            File dir = new File(envLib.trim());
            if (dir.exists() && dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        }

        String sysJna = System.getProperty("jna.library.path");
        if (sysJna != null && !sysJna.isBlank()) {
            for (String part : sysJna.split(File.pathSeparator)) {
                File dir = new File(part.trim());
                if (containsTesseractLib(dir)) {
                    return dir.getAbsolutePath();
                }
            }
        }

        String[] candidatePaths = {
                "/opt/homebrew/lib",                  // macOS Apple Silicon (Homebrew)
                "/usr/local/lib",                     // macOS Intel (Homebrew) / Linux
                "/usr/lib",                           // Linux standard
                "/usr/lib/aarch64-linux-gnu",         // Linux ARM64
                "/usr/lib/x86_64-linux-gnu",         // Linux x86_64
                "C:/Program Files/Tesseract-OCR"      // Windows standard
        };

        for (String candidate : candidatePaths) {
            File dir = new File(candidate);
            if (containsTesseractLib(dir)) {
                return dir.getAbsolutePath();
            }
        }

        File macHomebrew = new File("/opt/homebrew/lib");
        if (macHomebrew.exists() && macHomebrew.isDirectory()) {
            return macHomebrew.getAbsolutePath();
        }

        return null;
    }

    private void configureJnaLibraryPath(String libPath) {
        if (libPath == null || libPath.isBlank()) {
            return;
        }
        try {
            String current = System.getProperty("jna.library.path");
            if (current == null || current.isBlank()) {
                System.setProperty("jna.library.path", libPath);
            } else if (!current.contains(libPath)) {
                System.setProperty("jna.library.path", current + File.pathSeparator + libPath);
            }
            com.sun.jna.NativeLibrary.addSearchPath("tesseract", libPath);
            com.sun.jna.NativeLibrary.addSearchPath("lept", libPath);
        } catch (Throwable t) {
            log.warn("Could not register JNA search path '{}': {}", libPath, t.getMessage());
        }
    }

    private String resolveDatapath(String configuredDatapath, String lang) {
        if (configuredDatapath != null && !configuredDatapath.isBlank()) {
            File dir = new File(configuredDatapath.trim());
            if (isValidTessdataDir(dir, lang)) {
                return dir.getAbsolutePath();
            }
        }

        String envPrefix = System.getenv("TESSDATA_PREFIX");
        if (envPrefix != null && !envPrefix.isBlank()) {
            File dir = new File(envPrefix.trim());
            if (isValidTessdataDir(dir, lang)) {
                return dir.getAbsolutePath();
            }
            File nested = new File(dir, "tessdata");
            if (isValidTessdataDir(nested, lang)) {
                return nested.getAbsolutePath();
            }
        }

        String[] candidatePaths = {
                "/opt/homebrew/share/tessdata",                   // macOS Apple Silicon (Homebrew)
                "/usr/local/share/tessdata",                      // macOS Intel (Homebrew) / Linux
                "/usr/share/tessdata",                            // Linux standard
                "/usr/share/tesseract-ocr/5/tessdata",            // Ubuntu / Debian 5.x
                "/usr/share/tesseract-ocr/4.00/tessdata",         // Ubuntu / Debian 4.x
                "/usr/share/tesseract-ocr/tessdata",              // Arch / other Linux
                "C:/Program Files/Tesseract-OCR/tessdata",        // Windows standard
                "./tessdata",                                     // Relative to working dir
                "../tessdata"
        };

        for (String candidate : candidatePaths) {
            File dir = new File(candidate);
            if (isValidTessdataDir(dir, lang)) {
                return dir.getAbsolutePath();
            }
        }

        for (String candidate : candidatePaths) {
            File dir = new File(candidate);
            if (dir.exists() && dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        }

        return null;
    }

    private boolean containsTesseractLib(File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File[] matches = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.contains("tesseract") || lower.contains("lept");
        });
        return matches != null && matches.length > 0;
    }

    private boolean isValidTessdataDir(File dir, String lang) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        if (lang != null && !lang.isBlank()) {
            File trainedData = new File(dir, lang + ".traineddata");
            if (trainedData.exists() && trainedData.isFile()) {
                return true;
            }
        }
        File[] trained = dir.listFiles((d, name) -> name.endsWith(".traineddata"));
        return trained != null && trained.length > 0;
    }
}
