package com.heet.backend.repository;

import com.heet.backend.dto.projection.LoanSummaryProjection;
import com.heet.backend.entity.LoanRecord;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRecordRepository extends JpaRepository<LoanRecord, Long> {

    Page<LoanRecord> findByUserId(Long userId, Pageable pageable);

    Page<LoanRecord> findByUserIdAndType(Long userId, LoanType type, Pageable pageable);

    Page<LoanRecord> findByUserIdAndStatus(Long userId, LoanStatus status, Pageable pageable);

    Page<LoanRecord> findByUserIdAndTypeAndStatus(Long userId, LoanType type, LoanStatus status, Pageable pageable);

    Optional<LoanRecord> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT l.type AS type, SUM(l.remainingAmount) AS totalRemainingAmount, COUNT(l) AS count " +
           "FROM LoanRecord l " +
           "WHERE l.user.id = :userId AND l.status = :status " +
           "GROUP BY l.type")
    List<LoanSummaryProjection> findActiveLoanSummaryByUserId(
            @Param("userId") Long userId,
            @Param("status") LoanStatus status);
}
