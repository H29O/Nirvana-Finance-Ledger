package com.heet.backend.repository;

import com.heet.backend.entity.Transaction;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByTransactionType(TransactionType type);

    List<Transaction> findByPaymentMethod(PaymentMethod paymentMethod);

    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    boolean existsByCategoryId(Long categoryId);
}
