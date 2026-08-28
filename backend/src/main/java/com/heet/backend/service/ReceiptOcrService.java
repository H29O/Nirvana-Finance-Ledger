package com.heet.backend.service;

import com.heet.backend.dto.response.ReceiptOcrResponse;
import com.heet.backend.entity.Receipt;
import com.heet.backend.entity.ReceiptOcrData;
import com.heet.backend.entity.User;
import com.heet.backend.enums.OcrStatus;
import com.heet.backend.exception.ReceiptNotFoundException;
import com.heet.backend.exception.ReceiptOcrNotFoundException;
import com.heet.backend.repository.ReceiptOcrDataRepository;
import com.heet.backend.repository.ReceiptRepository;
import com.heet.backend.security.SecurityUtils;
import com.heet.backend.service.ocr.OcrResult;
import com.heet.backend.service.ocr.OcrService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;

@Service
public class ReceiptOcrService {

    private final ReceiptOcrDataRepository receiptOcrDataRepository;
    private final ReceiptRepository receiptRepository;
    private final OcrService ocrService;

    public ReceiptOcrService(
            ReceiptOcrDataRepository receiptOcrDataRepository,
            ReceiptRepository receiptRepository,
            OcrService ocrService) {
        this.receiptOcrDataRepository = receiptOcrDataRepository;
        this.receiptRepository = receiptRepository;
        this.ocrService = ocrService;
    }

    @Transactional
    public ReceiptOcrResponse processOcr(Long receiptId) {
        User user = SecurityUtils.getCurrentUser();
        Long userId = user.getId();

        Receipt receipt = receiptRepository.findByIdAndUserId(receiptId, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(receiptId));

        ReceiptOcrData ocrData = receiptOcrDataRepository.findByReceiptIdAndUserId(receiptId, userId)
                .orElseGet(() -> {
                    ReceiptOcrData newData = new ReceiptOcrData();
                    newData.setReceipt(receipt);
                    newData.setUser(user);
                    newData.setStatus(OcrStatus.PENDING);
                    return newData;
                });

        ocrData.setStatus(OcrStatus.PROCESSING);
        ocrData.setFailureReason(null);
        ocrData = receiptOcrDataRepository.save(ocrData);

        File file = new File(receipt.getStoragePath());
        OcrResult result = ocrService.extractTextAndData(file, receipt.getContentType());

        if (result.isSuccess()) {
            ocrData.setStatus(OcrStatus.COMPLETED);
            ocrData.setRawText(result.getRawText());
            ocrData.setDetectedMerchant(result.getDetectedMerchant());
            ocrData.setDetectedAmount(result.getDetectedAmount());
            ocrData.setDetectedDate(result.getDetectedDate());
            ocrData.setFailureReason(null);
            ocrData.setProcessedAt(LocalDateTime.now());
        } else {
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setRawText(null);
            ocrData.setDetectedMerchant(null);
            ocrData.setDetectedAmount(null);
            ocrData.setDetectedDate(null);
            ocrData.setFailureReason(result.getFailureReason());
            ocrData.setProcessedAt(LocalDateTime.now());
        }

        ReceiptOcrData savedOcrData = receiptOcrDataRepository.save(ocrData);
        return mapToResponse(savedOcrData);
    }

    @Transactional(readOnly = true)
    public ReceiptOcrResponse getOcrData(Long receiptId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // Verify receipt ownership
        receiptRepository.findByIdAndUserId(receiptId, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(receiptId));

        ReceiptOcrData ocrData = receiptOcrDataRepository.findByReceiptIdAndUserId(receiptId, userId)
                .orElseThrow(() -> new ReceiptOcrNotFoundException(receiptId));

        return mapToResponse(ocrData);
    }

    private ReceiptOcrResponse mapToResponse(ReceiptOcrData ocrData) {
        return ReceiptOcrResponse.builder()
                .id(ocrData.getId())
                .receiptId(ocrData.getReceipt().getId())
                .status(ocrData.getStatus())
                .rawText(ocrData.getRawText())
                .detectedMerchant(ocrData.getDetectedMerchant())
                .detectedAmount(ocrData.getDetectedAmount())
                .detectedDate(ocrData.getDetectedDate())
                .failureReason(ocrData.getFailureReason())
                .processedAt(ocrData.getProcessedAt())
                .createdAt(ocrData.getCreatedAt())
                .updatedAt(ocrData.getUpdatedAt())
                .build();
    }
}
