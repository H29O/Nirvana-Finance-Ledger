package com.heet.backend.service.ocr;

import com.heet.backend.dto.response.OcrBlockDto;
import com.heet.backend.dto.response.PaddleOcrResponseDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Primary
public class PaddleOcrService implements OcrService {

    @Getter
    private final String paddleOcrUrl;
    @Getter
    private final int timeoutMs;
    private final OcrDataExtractor ocrDataExtractor;
    private final RestClient restClient;
    private final OcrProcessManager ocrProcessManager;

    public PaddleOcrService(
            String paddleOcrUrl,
            int timeoutMs,
            OcrDataExtractor ocrDataExtractor) {
        this(paddleOcrUrl, timeoutMs, ocrDataExtractor, new OcrProcessManager(true, paddleOcrUrl, null, null, null, 30000));
    }

    @Autowired
    public PaddleOcrService(
            @Value("${app.ocr.paddle.url:http://localhost:5005}") String paddleOcrUrl,
            @Value("${app.ocr.paddle.timeout-ms:30000}") int timeoutMs,
            OcrDataExtractor ocrDataExtractor,
            @Autowired(required = false) OcrProcessManager ocrProcessManager) {
        this.paddleOcrUrl = (paddleOcrUrl != null && !paddleOcrUrl.isBlank()) ? paddleOcrUrl.trim() : "http://localhost:5005";
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 30000;
        this.ocrDataExtractor = ocrDataExtractor;
        this.ocrProcessManager = ocrProcessManager;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.min(timeoutMs, 5000));
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(this.paddleOcrUrl)
                .requestFactory(requestFactory)
                .build();

        log.info("Initialized PaddleOCR client service [url='{}', timeoutMs={}]", this.paddleOcrUrl, this.timeoutMs);
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

        // Ensure OCR process is running if auto-start is enabled and service is currently down
        if (ocrProcessManager != null && ocrProcessManager.isEnabled() && !ocrProcessManager.isServiceReady()) {
            log.info("PaddleOCR service is unreachable; attempting to ensure process is started...");
            ocrProcessManager.ensureStarted();
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            log.info("Sending receipt '{}' to PaddleOCR service at {}/ocr", file.getName(), paddleOcrUrl);

            PaddleOcrResponseDto response = restClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PaddleOcrResponseDto.class);

            if (response == null) {
                log.warn("PaddleOCR service returned empty response for file '{}'", file.getName());
                return OcrResult.failure("PaddleOCR service returned an empty response");
            }

            if (!response.isSuccess()) {
                String errorMsg = response.getError() != null ? response.getError() : "Unknown PaddleOCR failure";
                log.warn("PaddleOCR inference reported failure on file '{}': {}", file.getName(), errorMsg);
                return OcrResult.failure(errorMsg);
            }

            String rawText = response.getRawText() != null ? response.getRawText().trim() : "";
            List<OcrBlockDto> blocks = response.getBlocks() != null ? response.getBlocks() : Collections.emptyList();

            String merchant = ocrDataExtractor.extractMerchant(rawText, blocks);
            BigDecimal amount = ocrDataExtractor.extractAmount(rawText, blocks);
            LocalDate date = ocrDataExtractor.extractDate(rawText, blocks);

            log.info("PaddleOCR successfully processed file '{}': merchant='{}', amount={}, date={}",
                    file.getName(), merchant, amount, date);

            return OcrResult.success(rawText, merchant, amount, date);

        } catch (ResourceAccessException ex) {
            log.error("PaddleOCR service is unreachable at '{}': {}", paddleOcrUrl, ex.getMessage());
            return OcrResult.failure("PaddleOCR service is currently unavailable or unreachable at " + paddleOcrUrl +
                    ". Please ensure the local OCR microservice is running.");
        } catch (RestClientResponseException ex) {
            log.error("PaddleOCR service returned HTTP {} for file '{}': {}",
                    ex.getStatusCode(), file.getName(), ex.getResponseBodyAsString());
            return OcrResult.failure("PaddleOCR service returned HTTP " + ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Unexpected error during PaddleOCR processing for file '{}': {}", file.getAbsolutePath(), ex.getMessage(), ex);
            return OcrResult.failure("OCR engine failed: " + ex.getMessage());
        }
    }

    public boolean isServiceAvailable() {
        if (ocrProcessManager != null && ocrProcessManager.isEnabled() && !ocrProcessManager.isServiceReady()) {
            ocrProcessManager.ensureStarted();
        }
        try {
            String res = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);
            return res != null && res.contains("UP");
        } catch (Exception ex) {
            return false;
        }
    }
}
