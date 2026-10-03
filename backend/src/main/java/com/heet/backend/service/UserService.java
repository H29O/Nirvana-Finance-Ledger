package com.heet.backend.service;

import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.entity.User;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.repository.*;
import com.heet.backend.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReceiptOcrDataRepository receiptOcrDataRepository;
    private final ReceiptRepository receiptRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final ReceiptStorageService receiptStorageService;

    public UserService(
            UserRepository userRepository,
            ReceiptOcrDataRepository receiptOcrDataRepository,
            ReceiptRepository receiptRepository,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            LoanRecordRepository loanRecordRepository,
            EmailVerificationRepository emailVerificationRepository,
            ReceiptStorageService receiptStorageService) {
        this.userRepository = userRepository;
        this.receiptOcrDataRepository = receiptOcrDataRepository;
        this.receiptRepository = receiptRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.receiptStorageService = receiptStorageService;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(InvalidCredentialsException::new);
        return mapToUserResponse(user);
    }

    /**
     * Permanently deletes the authenticated user's account and all associated
     * data from PostgreSQL in a single atomic transaction respecting all foreign key constraints.
     *
     * @param principal authenticated user principal
     */
    @Transactional
    public void deleteAccount(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InvalidCredentialsException();
        }

        Long userId = principal.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        log.info("Starting atomic permanent account deletion for user id={}", userId);

        // Step 1: Delete all OCR data for user (foreign keys reference receipt and user)
        receiptOcrDataRepository.deleteByUserId(userId);

        // Step 2: Delete all receipts for user (foreign keys reference transaction and user)
        receiptRepository.deleteByUserId(userId);

        // Delete physical receipt storage files on disk
        try {
            receiptStorageService.deleteUserStorage(userId);
        } catch (Exception ex) {
            log.warn("Failed to delete physical receipt files on disk for user id {}: {}", userId, ex.getMessage());
        }

        // Step 3: Delete all transactions for user (foreign keys reference category and user)
        transactionRepository.deleteByUserId(userId);

        // Step 4: Delete all categories for user (foreign keys reference user)
        categoryRepository.deleteByUserId(userId);

        // Step 5: Delete all loan and repayment records for user (foreign keys reference user)
        loanRecordRepository.deleteByUserId(userId);

        // Step 6: Delete all email verification records for user (foreign keys reference user)
        emailVerificationRepository.deleteByUserId(userId);

        // Step 7: Delete user account entity
        userRepository.delete(user);

        log.info("Successfully deleted user id={} and all associated data permanently.", userId);
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
