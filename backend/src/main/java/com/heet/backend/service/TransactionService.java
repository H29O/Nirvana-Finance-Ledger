package com.heet.backend.service;

import com.heet.backend.dto.request.CreateTransactionRequest;
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

    public Transaction createTransaction(CreateTransactionRequest request) {
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

        return transactionRepository.save(transaction);
    }
}
