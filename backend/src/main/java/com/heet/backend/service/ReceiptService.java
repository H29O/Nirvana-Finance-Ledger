package com.heet.backend.service;

import com.heet.backend.dto.response.ReceiptFileResource;
import com.heet.backend.dto.response.ReceiptResponse;
import com.heet.backend.entity.Receipt;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.exception.InvalidReceiptException;
import com.heet.backend.exception.ReceiptNotFoundException;
import com.heet.backend.exception.TransactionNotFoundException;
import com.heet.backend.repository.ReceiptRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ReceiptService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg",
            ".jpeg",
            ".png",
            ".pdf"
    );

    private final ReceiptRepository receiptRepository;
    private final TransactionRepository transactionRepository;
    private final ReceiptStorageService receiptStorageService;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            TransactionRepository transactionRepository,
            ReceiptStorageService receiptStorageService) {
        this.receiptRepository = receiptRepository;
        this.transactionRepository = transactionRepository;
        this.receiptStorageService = receiptStorageService;
    }

    @Transactional
    public ReceiptResponse uploadReceipt(MultipartFile file, Long transactionId) {
        validateFile(file);

        User user = SecurityUtils.getCurrentUser();
        Long userId = user.getId();

        Transaction transaction = null;
        if (transactionId != null) {
            transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                    .orElseThrow(() -> new TransactionNotFoundException(transactionId));

            if (receiptRepository.existsByTransactionId(transactionId)) {
                throw new InvalidReceiptException("Transaction already has a receipt linked");
            }
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt");
        String extension = extractExtension(originalFilename);
        String storedFileName = UUID.randomUUID().toString() + extension;

        String storagePath = receiptStorageService.storeFile(userId, file, storedFileName);

        Receipt receipt = new Receipt();
        receipt.setUser(user);
        receipt.setTransaction(transaction);
        receipt.setOriginalFileName(originalFilename);
        receipt.setStoredFileName(storedFileName);
        receipt.setContentType(file.getContentType());
        receipt.setFileSize(file.getSize());
        receipt.setStoragePath(storagePath);
        receipt.setUploadedAt(LocalDateTime.now());

        Receipt savedReceipt = receiptRepository.save(receipt);
        return mapToResponse(savedReceipt);
    }

    @Transactional(readOnly = true)
    public Page<ReceiptResponse> getReceipts(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return receiptRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptMetadata(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Receipt receipt = receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(id));
        return mapToResponse(receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptFileResource getReceiptFile(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Receipt receipt = receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(id));

        Resource resource = receiptStorageService.loadFileAsResource(receipt.getStoragePath());
        return new ReceiptFileResource(resource, receipt.getContentType(), receipt.getOriginalFileName());
    }

    @Transactional
    public ReceiptResponse linkTransaction(Long id, Long transactionId) {
        Long userId = SecurityUtils.getCurrentUserId();

        Receipt receipt = receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(id));

        Transaction transaction = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        if (receiptRepository.existsByTransactionIdAndIdNot(transactionId, id)) {
            throw new InvalidReceiptException("Transaction already has a receipt linked");
        }

        receipt.setTransaction(transaction);
        Receipt savedReceipt = receiptRepository.save(receipt);
        return mapToResponse(savedReceipt);
    }

    @Transactional
    public ReceiptResponse unlinkTransaction(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Receipt receipt = receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(id));

        receipt.setTransaction(null);
        Receipt savedReceipt = receiptRepository.save(receipt);
        return mapToResponse(savedReceipt);
    }

    @Transactional
    public void deleteReceipt(Long id) {
        Long userId = SecurityUtils.getCurrentUserId();

        Receipt receipt = receiptRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ReceiptNotFoundException(id));

        receiptStorageService.deleteFile(receipt.getStoragePath());
        receiptRepository.delete(receipt);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidReceiptException("Cannot upload empty file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidReceiptException("File size exceeds maximum limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidReceiptException("Only JPEG, PNG, and PDF files are supported");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new InvalidReceiptException("Filename contains invalid path sequence");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidReceiptException("Invalid file extension. Only .jpg, .jpeg, .png, and .pdf are allowed");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private ReceiptResponse mapToResponse(Receipt receipt) {
        Long transactionId = receipt.getTransaction() != null ? receipt.getTransaction().getId() : null;

        return ReceiptResponse.builder()
                .id(receipt.getId())
                .originalFileName(receipt.getOriginalFileName())
                .contentType(receipt.getContentType())
                .fileSize(receipt.getFileSize())
                .uploadedAt(receipt.getUploadedAt())
                .transactionId(transactionId)
                .build();
    }
}
