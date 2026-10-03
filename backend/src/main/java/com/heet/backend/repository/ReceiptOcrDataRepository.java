package com.heet.backend.repository;

import com.heet.backend.entity.ReceiptOcrData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptOcrDataRepository extends JpaRepository<ReceiptOcrData, Long> {

    Optional<ReceiptOcrData> findByReceiptIdAndUserId(Long receiptId, Long userId);

    Optional<ReceiptOcrData> findByReceiptId(Long receiptId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ReceiptOcrData r WHERE r.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
