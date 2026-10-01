package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.response.ExpenseByCategoryResponse;
import com.heet.backend.dto.response.IncomeExpenseResponse;
import com.heet.backend.dto.response.MonthlySummaryResponse;
import com.heet.backend.dto.response.TopCategoryResponse;
import com.heet.backend.entity.User;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.heet.backend.dto.projection.LoanSummaryProjection;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.repository.LoanRecordRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LoanRecordRepository loanRecordRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");

        UserPrincipal principal = new UserPrincipal(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMonthlySummary_CalculatesCorrectly() {
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(4000));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(eq(1L), any(), any()))
                .thenReturn(25L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of());

        MonthlySummaryResponse response = dashboardService.getMonthlySummary(2026, 8);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(10000), response.getTotalIncome());
        assertEquals(BigDecimal.valueOf(4000), response.getTotalExpense());
        assertEquals(BigDecimal.valueOf(6000), response.getNetSavings());
        assertEquals(BigDecimal.ZERO, response.getTotalLent());
        assertEquals(BigDecimal.ZERO, response.getTotalDebt());
        assertEquals(25L, response.getTotalTransactions());
    }

    @Test
    void getMonthlySummary_WithActiveLendingAndBorrowing_CalculatesTotalAvailableMoneyCorrectly() {
        // Base income = 50,000, expense = 0
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(50000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByUserIdAndTransactionDateBetween(eq(1L), any(), any()))
                .thenReturn(1L);

        // 1. User lends 5,000 (totalLent = 5,000) -> Total = 45,000
        LoanSummaryProjection lentProjection = createLoanSummary(LoanType.LENT, BigDecimal.valueOf(5000), 1L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of(lentProjection));

        MonthlySummaryResponse response1 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(45000), response1.getNetSavings());
        assertEquals(BigDecimal.valueOf(5000), response1.getTotalLent());
        assertEquals(BigDecimal.ZERO, response1.getTotalDebt());

        // 2. Receive 2,000 repayment (remaining lent = 3,000) -> Total = 47,000
        LoanSummaryProjection partialRepaidLent = createLoanSummary(LoanType.LENT, BigDecimal.valueOf(3000), 1L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of(partialRepaidLent));

        MonthlySummaryResponse response2 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(47000), response2.getNetSavings());

        // 3. Receive remaining 3,000 (settled, no active loans) -> Total = 50,000
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of());

        MonthlySummaryResponse response3 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(50000), response3.getNetSavings());

        // 4. User borrows 5,000 (totalBorrowed = 5,000) -> Total = 55,000
        LoanSummaryProjection borrowedProjection = createLoanSummary(LoanType.BORROWED, BigDecimal.valueOf(5000), 1L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of(borrowedProjection));

        MonthlySummaryResponse response4 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(55000), response4.getNetSavings());
        assertEquals(BigDecimal.valueOf(5000), response4.getTotalDebt());

        // 5. User repays 2,000 (remaining borrowed = 3,000) -> Total = 53,000
        LoanSummaryProjection partialRepaidBorrowed = createLoanSummary(LoanType.BORROWED, BigDecimal.valueOf(3000), 1L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of(partialRepaidBorrowed));

        MonthlySummaryResponse response5 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(53000), response5.getNetSavings());

        // 6. User repays remaining 3,000 (settled) -> Total = 50,000
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of());

        MonthlySummaryResponse response6 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(50000), response6.getNetSavings());

        // 7. Combination: Normal transactions + active lending + active borrowing + partial repayments
        // Income 60,000, Expense 10,000 (base 50,000) + Borrowed 3,000 - Lent 7,000 -> Total = 46,000
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(60000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000));
        LoanSummaryProjection comboLent = createLoanSummary(LoanType.LENT, BigDecimal.valueOf(7000), 1L);
        LoanSummaryProjection comboBorrowed = createLoanSummary(LoanType.BORROWED, BigDecimal.valueOf(3000), 1L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of(comboLent, comboBorrowed));

        MonthlySummaryResponse response7 = dashboardService.getMonthlySummary(2026, 8);
        assertEquals(BigDecimal.valueOf(46000), response7.getNetSavings());
        assertEquals(BigDecimal.valueOf(7000), response7.getTotalLent());
        assertEquals(BigDecimal.valueOf(3000), response7.getTotalDebt());
    }

    @Test
    void getIncomeVsExpense_CalculatesBalance() {
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(6000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(4000));
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of());

        IncomeExpenseResponse response = dashboardService.getIncomeVsExpense(2026, 8);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(6000), response.getTotalIncome());
        assertEquals(BigDecimal.valueOf(4000), response.getTotalExpense());
        assertEquals(BigDecimal.valueOf(2000), response.getBalance());
    }

    private LoanSummaryProjection createLoanSummary(LoanType type, BigDecimal amount, Long count) {
        return new LoanSummaryProjection() {
            @Override public LoanType getType() { return type; }
            @Override public BigDecimal getTotalRemainingAmount() { return amount; }
            @Override public Long getCount() { return count; }
        };
    }

    @Test
    void getExpenseByCategory_ReturnsBreakdown() {
        CategoryExpenseProjection projection = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return 1L; }
            @Override public String getCategoryName() { return "Groceries"; }
            @Override public String getColor() { return "#FF0000"; }
            @Override public String getIcon() { return "cart"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(2000); }
            @Override public Long getTransactionCount() { return 10L; }
        };

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(4000));
        when(transactionRepository.findCategoryExpenseBreakdownByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(projection));

        ExpenseByCategoryResponse response = dashboardService.getExpenseByCategory(2026, 8);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(4000), response.getTotalExpense());
        assertEquals(1, response.getCategories().size());
        assertEquals("Groceries", response.getCategories().get(0).getCategoryName());
    }

    @Test
    void getTopCategories_ReturnsTopItems() {
        CategoryExpenseProjection projection = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return 2L; }
            @Override public String getCategoryName() { return "Rent"; }
            @Override public String getColor() { return "#00FF00"; }
            @Override public String getIcon() { return "home"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(3000); }
            @Override public Long getTransactionCount() { return 1L; }
        };

        when(transactionRepository.findTopExpenseCategoriesByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(projection));

        List<TopCategoryResponse> response = dashboardService.getTopCategories(2026, 8, 3);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Rent", response.get(0).getCategoryName());
    }
}
