package com.heet.backend.service;

import com.heet.backend.dto.request.CreateTransactionRequest;
import com.heet.backend.dto.response.TransactionResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.exception.TransactionNotFoundException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heet.backend.repository.ReceiptRepository;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ReceiptRepository receiptRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            ReceiptRepository receiptRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.receiptRepository = receiptRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        User user = SecurityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setEntrySource(request.getEntrySource());
        transaction.setMerchant(request.getMerchant());
        transaction.setNotes(request.getNotes());
        transaction.setTransactionDate(now);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        assignCategoryIfProvided(transaction, request.getCategoryId(), user.getId());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, CreateTransactionRequest request) {
        User user = SecurityUtils.getCurrentUser();
        Transaction transaction = findTransactionByIdAndUser(id, user.getId());

        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setEntrySource(request.getEntrySource());
        transaction.setMerchant(request.getMerchant());
        transaction.setNotes(request.getNotes());
        transaction.setUpdatedAt(LocalDateTime.now());

        assignCategoryIfProvided(transaction, request.getCategoryId(), user.getId());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        User user = SecurityUtils.getCurrentUser();
        Transaction transaction = findTransactionByIdAndUser(id, user.getId());

        // Safely unlink any receipt associated with this transaction before deletion
        receiptRepository.findByTransactionIdAndUserId(id, user.getId())
                .ifPresent(receipt -> {
                    receipt.setTransaction(null);
                    receiptRepository.save(receipt);
                });

        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            TransactionType type,
            PaymentMethod paymentMethod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        Long userId = SecurityUtils.getCurrentUserId();

        Page<Transaction> transactionsPage;

        if (type != null) {
            transactionsPage = transactionRepository.findByUserIdAndTransactionType(userId, type, pageable);
        } else if (paymentMethod != null) {
            transactionsPage = transactionRepository.findByUserIdAndPaymentMethod(userId, paymentMethod, pageable);
        } else if (startDate != null && endDate != null) {
            transactionsPage = transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate, pageable);
        } else {
            transactionsPage = transactionRepository.findByUserId(userId, pageable);
        }

        return transactionsPage.map(this::mapToResponse);
    }

    private void assignCategoryIfProvided(Transaction transaction, Long categoryId, Long userId) {
        if (categoryId != null) {
            Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                    .orElseThrow(() -> new CategoryNotFoundException(categoryId));
            transaction.setCategory(category);
        } else {
            transaction.setCategory(null);
        }
    }

    private Transaction findTransactionByIdAndUser(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        Long categoryId = transaction.getCategory() != null ? transaction.getCategory().getId() : null;
        String categoryName = transaction.getCategory() != null ? transaction.getCategory().getName() : null;

        return TransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .transactionType(transaction.getTransactionType())
                .paymentMethod(transaction.getPaymentMethod())
                .entrySource(transaction.getEntrySource())
                .transactionDate(transaction.getTransactionDate())
                .merchant(transaction.getMerchant())
                .notes(transaction.getNotes())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
