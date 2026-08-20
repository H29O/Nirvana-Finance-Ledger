package com.heet.backend.repository;

import com.heet.backend.entity.LoanRecord;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanRecordRepository extends JpaRepository<LoanRecord, Long> {

    Page<LoanRecord> findByUserId(Long userId, Pageable pageable);

    Page<LoanRecord> findByUserIdAndType(Long userId, LoanType type, Pageable pageable);

    Page<LoanRecord> findByUserIdAndStatus(Long userId, LoanStatus status, Pageable pageable);

    Page<LoanRecord> findByUserIdAndTypeAndStatus(Long userId, LoanType type, LoanStatus status, Pageable pageable);

    Optional<LoanRecord> findByIdAndUserId(Long id, Long userId);
}
