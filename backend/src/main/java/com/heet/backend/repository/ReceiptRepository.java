package com.heet.backend.repository;

import com.heet.backend.entity.Receipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByIdAndUserId(Long id, Long userId);

    Page<Receipt> findByUserId(Long userId, Pageable pageable);

    Optional<Receipt> findByTransactionIdAndUserId(Long transactionId, Long userId);

    boolean existsByTransactionId(Long transactionId);

    boolean existsByTransactionIdAndIdNot(Long transactionId, Long id);
}
