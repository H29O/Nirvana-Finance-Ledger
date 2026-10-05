package com.heet.backend.service;

import com.heet.backend.dto.request.UpdateReceiptOcrRequest;
import com.heet.backend.dto.response.ReceiptOcrResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Receipt;
import com.heet.backend.entity.ReceiptOcrData;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.enums.OcrStatus;
import com.heet.backend.exception.ReceiptNotFoundException;
import com.heet.backend.exception.ReceiptOcrNotFoundException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.ReceiptOcrDataRepository;
import com.heet.backend.repository.ReceiptRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import com.heet.backend.service.ocr.CategoryClassifier;
import com.heet.backend.service.ocr.OcrResult;
import com.heet.backend.service.ocr.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ReceiptOcrService {

    private final ReceiptOcrDataRepository receiptOcrDataRepository;
    private final ReceiptRepository receiptRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryClassifier categoryClassifier;
    private final OcrService ocrService;
    private final TransactionRepository transactionRepository;

    public ReceiptOcrService(
            ReceiptOcrDataRepository receiptOcrDataRepository,
            ReceiptRepository receiptRepository,
            CategoryRepository categoryRepository,
            CategoryClassifier categoryClassifier,
            OcrService ocrService,
            TransactionRepository transactionRepository) {
        this.receiptOcrDataRepository = receiptOcrDataRepository;
        this.receiptRepository = receiptRepository;
        this.categoryRepository = categoryRepository;
        this.categoryClassifier = categoryClassifier;
        this.ocrService = ocrService;
        this.transactionRepository = transactionRepository;
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
        if (!file.exists()) {
            File relativeFile = new File(System.getProperty("user.dir"), receipt.getStoragePath());
            if (relativeFile.exists()) {
                file = relativeFile;
            }
        }

        log.info("Starting OCR processing for receipt id {} (file: '{}')", receiptId, file.getAbsolutePath());
        OcrResult result = ocrService.extractTextAndData(file, receipt.getContentType());

        if (result.isSuccess()) {
            log.info("OCR completed successfully for receipt id {}: merchant='{}', amount='{}', date='{}'",
                    receiptId, result.getDetectedMerchant(), result.getDetectedAmount(), result.getDetectedDate());
            ocrData.setStatus(OcrStatus.COMPLETED);
            ocrData.setRawText(result.getRawText());
            ocrData.setDetectedMerchant(result.getDetectedMerchant());
            ocrData.setDetectedAmount(result.getDetectedAmount());
            ocrData.setDetectedDate(result.getDetectedDate());
            ocrData.setFailureReason(null);
            ocrData.setProcessedAt(LocalDateTime.now());

            // Suggest category strictly from the authenticated user's own categories
            List<Category> userCategories = categoryRepository.findByUserId(userId);
            CategoryClassifier.CategorySuggestion suggestion = categoryClassifier.suggestCategory(
                    result.getRawText(),
                    result.getDetectedMerchant(),
                    userCategories
            );

            if (suggestion != null) {
                ocrData.setSuggestedCategoryId(suggestion.categoryId());
                ocrData.setSuggestedCategoryName(suggestion.categoryName());
                ocrData.setConfidenceScore(suggestion.confidence());
            } else {
                ocrData.setSuggestedCategoryId(null);
                ocrData.setSuggestedCategoryName(null);
                ocrData.setConfidenceScore(null);
            }
        } else {
            log.warn("OCR failed for receipt id {}: reason='{}'", receiptId, result.getFailureReason());
            ocrData.setStatus(OcrStatus.FAILED);
            ocrData.setRawText(null);
            ocrData.setDetectedMerchant(null);
            ocrData.setDetectedAmount(null);
            ocrData.setDetectedDate(null);
            ocrData.setSuggestedCategoryId(null);
            ocrData.setSuggestedCategoryName(null);
            ocrData.setConfidenceScore(null);
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

    @Transactional
    public ReceiptOcrResponse updateOcrData(Long receiptId, UpdateReceiptOcrRequest request) {
        User user = SecurityUtils.getCurrentUser();
        Long userId = user.getId();

        Receipt receipt = receiptRepository.findByIdAndUserId(receiptId, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(receiptId));

        ReceiptOcrData ocrData = receiptOcrDataRepository.findByReceiptIdAndUserId(receiptId, userId)
                .orElseGet(() -> {
                    ReceiptOcrData newData = new ReceiptOcrData();
                    newData.setReceipt(receipt);
                    newData.setUser(user);
                    return newData;
                });

        ocrData.setStatus(OcrStatus.COMPLETED);
        ocrData.setFailureReason(null);
        ocrData.setDetectedMerchant(request.getMerchant() != null ? request.getMerchant().trim() : null);
        ocrData.setDetectedAmount(request.getAmount());
        ocrData.setDetectedDate(request.getDate());

        if (request.getCategoryId() != null) {
            categoryRepository.findByIdAndUserId(request.getCategoryId(), userId).ifPresent(cat -> {
                ocrData.setSuggestedCategoryId(cat.getId());
                ocrData.setSuggestedCategoryName(cat.getName());
            });
        } else if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            ocrData.setSuggestedCategoryName(request.getCategoryName().trim());
            ocrData.setSuggestedCategoryId(null);
        } else {
            ocrData.setSuggestedCategoryId(null);
            ocrData.setSuggestedCategoryName(null);
        }

        ocrData.setProcessedAt(LocalDateTime.now());
        ReceiptOcrData savedOcrData = receiptOcrDataRepository.save(ocrData);

        // If receipt is already linked to a transaction, keep the transaction synchronized
        if (receipt.getTransaction() != null && transactionRepository != null) {
            Transaction transaction = receipt.getTransaction();
            if (request.getMerchant() != null && !request.getMerchant().isBlank()) {
                transaction.setMerchant(request.getMerchant().trim());
            }
            if (request.getAmount() != null) {
                transaction.setAmount(request.getAmount());
            }
            if (request.getDate() != null) {
                transaction.setTransactionDate(request.getDate().atStartOfDay());
            }
            if (request.getCategoryId() != null) {
                categoryRepository.findByIdAndUserId(request.getCategoryId(), userId).ifPresent(transaction::setCategory);
            }
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
        }

        log.info("Updated receipt OCR data for receipt id {}: merchant='{}', amount='{}', date='{}'",
                receiptId, savedOcrData.getDetectedMerchant(), savedOcrData.getDetectedAmount(), savedOcrData.getDetectedDate());

        return mapToResponse(savedOcrData);
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
                .suggestedCategoryId(ocrData.getSuggestedCategoryId())
                .suggestedCategoryName(ocrData.getSuggestedCategoryName())
                .confidenceScore(ocrData.getConfidenceScore())
                .failureReason(ocrData.getFailureReason())
                .processedAt(ocrData.getProcessedAt())
                .createdAt(ocrData.getCreatedAt())
                .updatedAt(ocrData.getUpdatedAt())
                .build();
    }
}
