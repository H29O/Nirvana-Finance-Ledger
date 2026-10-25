package com.heet.backend.repository;

import com.heet.backend.entity.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {

    List<LoanEvent> findByLoanRecordIdOrderByEventDateAscIdAsc(Long loanRecordId);

    List<LoanEvent> findByLoanRecordIdAndUserIdOrderByEventDateAscIdAsc(Long loanRecordId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM LoanEvent e WHERE e.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
