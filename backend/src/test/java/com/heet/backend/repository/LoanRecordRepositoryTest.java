package com.heet.backend.repository;

import com.heet.backend.dto.projection.LoanSummaryProjection;
import com.heet.backend.entity.LoanRecord;
import com.heet.backend.entity.User;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LoanRecordRepositoryTest {

    @Autowired
    private LoanRecordRepository loanRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setName("Loan Test User 1");
        user1.setEmail("loan_user1_" + System.nanoTime() + "@example.com");
        user1.setPasswordHash("hash123");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("Loan Test User 2");
        user2.setEmail("loan_user2_" + System.nanoTime() + "@example.com");
        user2.setPasswordHash("hash123");
        user2 = userRepository.save(user2);
    }

    private LoanRecord createLoanRecord(
            User user,
            LoanType type,
            String personName,
            BigDecimal originalAmount,
            BigDecimal remainingAmount,
            LoanStatus status) {

        LoanRecord record = new LoanRecord();
        record.setUser(user);
        record.setType(type);
        record.setPersonName(personName);
        record.setOriginalAmount(originalAmount);
        record.setRemainingAmount(remainingAmount);
        record.setStatus(status);
        record.setDueDate(LocalDate.of(2026, 12, 31));
        return loanRecordRepository.save(record);
    }

    @Test
    void findActiveLoanSummaryByUserId_AggregatesAndEnforcesUserIsolation() {
        // User 1 active LENT
        createLoanRecord(user1, LoanType.LENT, "Bob", new BigDecimal("500.00"), new BigDecimal("300.00"), LoanStatus.ACTIVE);
        createLoanRecord(user1, LoanType.LENT, "Charlie", new BigDecimal("200.00"), new BigDecimal("200.00"), LoanStatus.ACTIVE);

        // User 1 settled LENT (must be excluded)
        createLoanRecord(user1, LoanType.LENT, "Settled Person", new BigDecimal("100.00"), BigDecimal.ZERO, LoanStatus.SETTLED);

        // User 1 active BORROWED
        createLoanRecord(user1, LoanType.BORROWED, "Bank", new BigDecimal("1000.00"), new BigDecimal("750.00"), LoanStatus.ACTIVE);

        // User 2 active LENT (must be excluded due to user isolation)
        createLoanRecord(user2, LoanType.LENT, "User2 Borrower", new BigDecimal("800.00"), new BigDecimal("800.00"), LoanStatus.ACTIVE);

        List<LoanSummaryProjection> summary = loanRecordRepository.findActiveLoanSummaryByUserId(user1.getId(), LoanStatus.ACTIVE);

        assertNotNull(summary);
        assertEquals(2, summary.size());

        for (LoanSummaryProjection item : summary) {
            if (item.getType() == LoanType.LENT) {
                assertEquals(0, new BigDecimal("500.00").compareTo(item.getTotalRemainingAmount()));
                assertEquals(2L, item.getCount());
            } else if (item.getType() == LoanType.BORROWED) {
                assertEquals(0, new BigDecimal("750.00").compareTo(item.getTotalRemainingAmount()));
                assertEquals(1L, item.getCount());
            }
        }
    }

    @Test
    void findByUserIdAndTypeAndStatus_FiltersAndPaginates() {
        createLoanRecord(user1, LoanType.LENT, "Alice", new BigDecimal("300.00"), new BigDecimal("300.00"), LoanStatus.ACTIVE);
        createLoanRecord(user1, LoanType.LENT, "Dave", new BigDecimal("400.00"), new BigDecimal("400.00"), LoanStatus.ACTIVE);
        createLoanRecord(user1, LoanType.BORROWED, "Eve", new BigDecimal("100.00"), new BigDecimal("100.00"), LoanStatus.ACTIVE);

        Page<LoanRecord> lentActivePage = loanRecordRepository.findByUserIdAndTypeAndStatus(
                user1.getId(), LoanType.LENT, LoanStatus.ACTIVE, PageRequest.of(0, 10));

        assertNotNull(lentActivePage);
        assertEquals(2, lentActivePage.getTotalElements());

        Page<LoanRecord> borrowedActivePage = loanRecordRepository.findByUserIdAndTypeAndStatus(
                user1.getId(), LoanType.BORROWED, LoanStatus.ACTIVE, PageRequest.of(0, 10));

        assertNotNull(borrowedActivePage);
        assertEquals(1, borrowedActivePage.getTotalElements());
    }

    @Test
    void findByIdAndUserId_EnforcesUserOwnership() {
        LoanRecord user1Record = createLoanRecord(user1, LoanType.LENT, "Frank", new BigDecimal("250.00"), new BigDecimal("250.00"), LoanStatus.ACTIVE);

        Optional<LoanRecord> foundByUser1 = loanRecordRepository.findByIdAndUserId(user1Record.getId(), user1.getId());
        assertTrue(foundByUser1.isPresent());

        // Attempting to query user1's record with user2's ID
        Optional<LoanRecord> foundByUser2 = loanRecordRepository.findByIdAndUserId(user1Record.getId(), user2.getId());
        assertFalse(foundByUser2.isPresent());
    }
}
