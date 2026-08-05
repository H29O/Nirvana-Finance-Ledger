package com.heet.backend.service;

import com.heet.backend.dto.request.CreateTransactionRequest;
import com.heet.backend.dto.response.TransactionResponse;
import com.heet.backend.entity.Transaction;
import com.heet.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

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

        Transaction savedTransaction = transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .id(savedTransaction.getId())
                .amount(savedTransaction.getAmount())
                .transactionType(savedTransaction.getTransactionType())
                .paymentMethod(savedTransaction.getPaymentMethod())
                .entrySource(savedTransaction.getEntrySource())
                .transactionDate(savedTransaction.getTransactionDate())
                .merchant(savedTransaction.getMerchant())
                .notes(savedTransaction.getNotes())
                .createdAt(savedTransaction.getCreatedAt())
                .updatedAt(savedTransaction.getUpdatedAt())
                .build();
    }
}
