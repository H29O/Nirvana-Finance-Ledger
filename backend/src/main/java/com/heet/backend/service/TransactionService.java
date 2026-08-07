package com.heet.backend.service;

import com.heet.backend.dto.request.CreateTransactionRequest;
import com.heet.backend.dto.response.TransactionResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Transaction;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.exception.TransactionNotFoundException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setEntrySource(request.getEntrySource());
        transaction.setMerchant(request.getMerchant());
        transaction.setNotes(request.getNotes());
        transaction.setTransactionDate(now);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);

        assignCategoryIfProvided(transaction, request.getCategoryId());

        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, CreateTransactionRequest request) {
        Transaction transaction = findTransactionById(id);

        transaction.setAmount(request.getAmount());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setEntrySource(request.getEntrySource());
        transaction.setMerchant(request.getMerchant());
        transaction.setNotes(request.getNotes());
        transaction.setUpdatedAt(LocalDateTime.now());

        assignCategoryIfProvided(transaction, request.getCategoryId());

        Transaction savedTransaction = transactionRepository.save(transaction);
        return mapToResponse(savedTransaction);
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Transaction transaction = findTransactionById(id);
        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            TransactionType type,
            PaymentMethod paymentMethod,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        if (type != null) {
            return createPage(transactionRepository.findByTransactionType(type), pageable);
        }

        if (paymentMethod != null) {
            return createPage(transactionRepository.findByPaymentMethod(paymentMethod), pageable);
        }

        if (startDate != null && endDate != null) {
            return createPage(transactionRepository.findByTransactionDateBetween(startDate, endDate), pageable);
        }

        return transactionRepository.findAll(pageable).map(this::mapToResponse);
    }

    private void assignCategoryIfProvided(Transaction transaction, Long categoryId) {
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new CategoryNotFoundException(categoryId));
            transaction.setCategory(category);
        } else {
            transaction.setCategory(null);
        }
    }

    private Transaction findTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private Page<TransactionResponse> createPage(List<Transaction> transactions, Pageable pageable) {
        List<TransactionResponse> responses = transactions.stream()
                .map(this::mapToResponse)
                .toList();

        int start = Math.toIntExact(Math.min(pageable.getOffset(), responses.size()));
        int end = Math.min(start + pageable.getPageSize(), responses.size());

        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
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
