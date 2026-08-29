package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.projection.MonthlyTypeSumProjection;
import com.heet.backend.dto.response.AnalyticsOverviewResponse;
import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MonthlyTrendResponse;
import com.heet.backend.dto.response.PeriodComparisonResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

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
    void getOverview_CalculatesCorrectly() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(2000));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(eq(1L), any(), any()))
                .thenReturn(15L);

        AnalyticsOverviewResponse response = analyticsService.getOverview(startDate, endDate);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(5000), response.getTotalIncome());
        assertEquals(BigDecimal.valueOf(2000), response.getTotalExpense());
        assertEquals(BigDecimal.valueOf(3000), response.getNetSavings());
        assertEquals(15L, response.getTransactionCount());
    }

    @Test
    void getCategoryBreakdown_DefaultExpense_CalculatesPercentages() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        CategoryExpenseProjection projection1 = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return 1L; }
            @Override public String getCategoryName() { return "Food"; }
            @Override public String getColor() { return "#FF0000"; }
            @Override public String getIcon() { return "food"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(1500); }
            @Override public Long getTransactionCount() { return 5L; }
        };

        CategoryExpenseProjection projection2 = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return 2L; }
            @Override public String getCategoryName() { return "Utilities"; }
            @Override public String getColor() { return "#00FF00"; }
            @Override public String getIcon() { return "bolt"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(500); }
            @Override public Long getTransactionCount() { return 2L; }
        };

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(2000));
        when(transactionRepository.findCategoryExpenseBreakdownByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(projection1, projection2));

        List<CategoryBreakdownResponse> result = analyticsService.getCategoryBreakdown(startDate, endDate, null);

        assertNotNull(result);
        assertEquals(2, result.size());

        CategoryBreakdownResponse item1 = result.get(0);
        assertEquals(1L, item1.getCategoryId());
        assertEquals("Food", item1.getCategoryName());
        assertEquals(BigDecimal.valueOf(1500), item1.getAmount());
        assertEquals(75.0, item1.getPercentage());
        assertEquals(5L, item1.getTransactionCount());

        CategoryBreakdownResponse item2 = result.get(1);
        assertEquals(2L, item2.getCategoryId());
        assertEquals("Utilities", item2.getCategoryName());
        assertEquals(BigDecimal.valueOf(500), item2.getAmount());
        assertEquals(25.0, item2.getPercentage());
        assertEquals(2L, item2.getTransactionCount());
    }

    @Test
    void getCategoryBreakdown_ZeroTotal_HandlesSafely() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findCategoryExpenseBreakdownByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of());

        List<CategoryBreakdownResponse> result = analyticsService.getCategoryBreakdown(startDate, endDate, TransactionType.EXPENSE);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getCategoryBreakdown_UncategorizedCategory_HandlesNullSafely() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        CategoryExpenseProjection projection = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return null; }
            @Override public String getCategoryName() { return null; }
            @Override public String getColor() { return null; }
            @Override public String getIcon() { return null; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(300); }
            @Override public Long getTransactionCount() { return 1L; }
        };

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(300));
        when(transactionRepository.findCategoryExpenseBreakdownByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(projection));

        List<CategoryBreakdownResponse> result = analyticsService.getCategoryBreakdown(startDate, endDate, TransactionType.EXPENSE);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getCategoryId());
        assertEquals("Uncategorized", result.get(0).getCategoryName());
        assertEquals(100.0, result.get(0).getPercentage());
    }

    @Test
    void getMonthlyTrend_AggregatesAndFillsMissingMonths() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        MonthlyTypeSumProjection p1 = new MonthlyTypeSumProjection() {
            @Override public Integer getYear() { return 2026; }
            @Override public Integer getMonth() { return 1; }
            @Override public TransactionType getTransactionType() { return TransactionType.INCOME; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(4000); }
        };

        MonthlyTypeSumProjection p2 = new MonthlyTypeSumProjection() {
            @Override public Integer getYear() { return 2026; }
            @Override public Integer getMonth() { return 1; }
            @Override public TransactionType getTransactionType() { return TransactionType.EXPENSE; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(1500); }
        };

        MonthlyTypeSumProjection p3 = new MonthlyTypeSumProjection() {
            @Override public Integer getYear() { return 2026; }
            @Override public Integer getMonth() { return 3; }
            @Override public TransactionType getTransactionType() { return TransactionType.EXPENSE; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(2000); }
        };

        when(transactionRepository.findMonthlyTypeSumsByUserId(eq(1L), any(), any()))
                .thenReturn(List.of(p1, p2, p3));

        List<MonthlyTrendResponse> trend = analyticsService.getMonthlyTrend(startDate, endDate);

        assertNotNull(trend);
        assertEquals(3, trend.size());

        // Month 1
        assertEquals(2026, trend.get(0).getYear());
        assertEquals(1, trend.get(0).getMonth());
        assertEquals(BigDecimal.valueOf(4000), trend.get(0).getIncome());
        assertEquals(BigDecimal.valueOf(1500), trend.get(0).getExpense());
        assertEquals(BigDecimal.valueOf(2500), trend.get(0).getNetSavings());

        // Month 2 (Empty month gap filled with zero)
        assertEquals(2026, trend.get(1).getYear());
        assertEquals(2, trend.get(1).getMonth());
        assertEquals(BigDecimal.ZERO, trend.get(1).getIncome());
        assertEquals(BigDecimal.ZERO, trend.get(1).getExpense());
        assertEquals(BigDecimal.ZERO, trend.get(1).getNetSavings());

        // Month 3
        assertEquals(2026, trend.get(2).getYear());
        assertEquals(3, trend.get(2).getMonth());
        assertEquals(BigDecimal.ZERO, trend.get(2).getIncome());
        assertEquals(BigDecimal.valueOf(2000), trend.get(2).getExpense());
        assertEquals(BigDecimal.valueOf(-2000), trend.get(2).getNetSavings());
    }

    @Test
    void getPeriodComparison_CalculatesChangesCorrectly() {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 28);

        // Current period: Income 6000, Expense 3000 -> Net 3000
        // Previous period (Jan 4 to Jan 31): Income 4000, Expense 2000 -> Net 2000
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(6000))
                .thenReturn(BigDecimal.valueOf(4000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(3000))
                .thenReturn(BigDecimal.valueOf(2000));

        PeriodComparisonResponse response = analyticsService.getPeriodComparison(startDate, endDate);

        assertNotNull(response);
        assertNotNull(response.getCurrentPeriod());
        assertEquals(BigDecimal.valueOf(6000), response.getCurrentPeriod().getIncome());
        assertEquals(BigDecimal.valueOf(3000), response.getCurrentPeriod().getExpense());
        assertEquals(BigDecimal.valueOf(3000), response.getCurrentPeriod().getNetSavings());

        assertNotNull(response.getPreviousPeriod());
        assertEquals(BigDecimal.valueOf(4000), response.getPreviousPeriod().getIncome());
        assertEquals(BigDecimal.valueOf(2000), response.getPreviousPeriod().getExpense());
        assertEquals(BigDecimal.valueOf(2000), response.getPreviousPeriod().getNetSavings());

        // ((6000 - 4000) / 4000) * 100 = 50.0%
        assertEquals(50.0, response.getIncomeChangePercentage());
        // ((3000 - 2000) / 2000) * 100 = 50.0%
        assertEquals(50.0, response.getExpenseChangePercentage());
        // ((3000 - 2000) / 2000) * 100 = 50.0%
        assertEquals(50.0, response.getSavingsChangePercentage());
    }

    @Test
    void getPeriodComparison_ZeroPreviousValue_HandlesSafely() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO);

        PeriodComparisonResponse response = analyticsService.getPeriodComparison(startDate, endDate);

        assertNotNull(response);
        assertEquals(100.0, response.getIncomeChangePercentage());
        assertEquals(0.0, response.getExpenseChangePercentage());
        assertEquals(100.0, response.getSavingsChangePercentage());
    }

    @Test
    void getPeriodComparison_JanuaryToPreviousDecember_CalculatesCorrectDateBoundaries() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31); // 31 days

        // Current period: Jan 1 - Jan 31 -> Previous period: Dec 1, 2025 - Dec 31, 2025
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.INCOME),
                eq(java.time.LocalDateTime.of(2026, 1, 1, 0, 0)),
                eq(java.time.LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(5000));

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE),
                eq(java.time.LocalDateTime.of(2026, 1, 1, 0, 0)),
                eq(java.time.LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(2000));

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.INCOME),
                eq(java.time.LocalDateTime.of(2025, 12, 1, 0, 0)),
                eq(java.time.LocalDateTime.of(2025, 12, 31, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(4000));

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE),
                eq(java.time.LocalDateTime.of(2025, 12, 1, 0, 0)),
                eq(java.time.LocalDateTime.of(2025, 12, 31, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(1500));

        PeriodComparisonResponse response = analyticsService.getPeriodComparison(startDate, endDate);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(5000), response.getCurrentPeriod().getIncome());
        assertEquals(BigDecimal.valueOf(2000), response.getCurrentPeriod().getExpense());
        assertEquals(BigDecimal.valueOf(4000), response.getPreviousPeriod().getIncome());
        assertEquals(BigDecimal.valueOf(1500), response.getPreviousPeriod().getExpense());
    }

    @Test
    void getPeriodComparison_LeapYearFebruary29_CalculatesCorrectDays() {
        LocalDate startDate = LocalDate.of(2024, 2, 1);
        LocalDate endDate = LocalDate.of(2024, 2, 29); // 29 days in leap year 2024

        // Previous 29 days ending Jan 31, 2024 -> Jan 3, 2024 - Jan 31, 2024
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.INCOME),
                eq(java.time.LocalDateTime.of(2024, 2, 1, 0, 0)),
                eq(java.time.LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(6000));

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE),
                eq(java.time.LocalDateTime.of(2024, 2, 1, 0, 0)),
                eq(java.time.LocalDateTime.of(2024, 2, 29, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(2900));

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.INCOME),
                eq(java.time.LocalDateTime.of(2024, 1, 3, 0, 0)),
                eq(java.time.LocalDateTime.of(2024, 1, 31, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(4500));

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                eq(1L), eq(TransactionType.EXPENSE),
                eq(java.time.LocalDateTime.of(2024, 1, 3, 0, 0)),
                eq(java.time.LocalDateTime.of(2024, 1, 31, 23, 59, 59, 999999999))))
                .thenReturn(BigDecimal.valueOf(2000));

        PeriodComparisonResponse response = analyticsService.getPeriodComparison(startDate, endDate);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(6000), response.getCurrentPeriod().getIncome());
        assertEquals(BigDecimal.valueOf(2900), response.getCurrentPeriod().getExpense());
        assertEquals(BigDecimal.valueOf(4500), response.getPreviousPeriod().getIncome());
        assertEquals(BigDecimal.valueOf(2000), response.getPreviousPeriod().getExpense());
    }

    @Test
    void dateValidation_StartDateAfterEndDate_ThrowsIllegalArgumentException() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 4, 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                analyticsService.getOverview(startDate, endDate));
        assertEquals("Start date cannot be after end date", ex.getMessage());
    }

    @Test
    void dateValidation_NullDates_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                analyticsService.getOverview(null, LocalDate.now()));
        assertEquals("Start date and end date are required", ex.getMessage());
    }
}
