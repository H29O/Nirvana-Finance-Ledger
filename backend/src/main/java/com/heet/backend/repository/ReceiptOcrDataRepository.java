package com.heet.backend.repository;

import com.heet.backend.entity.ReceiptOcrData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptOcrDataRepository extends JpaRepository<ReceiptOcrData, Long> {

    Optional<ReceiptOcrData> findByReceiptIdAndUserId(Long receiptId, Long userId);

    Optional<ReceiptOcrData> findByReceiptId(Long receiptId);
}
