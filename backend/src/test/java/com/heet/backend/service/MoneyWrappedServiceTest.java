package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.projection.DailySpendProjection;
import com.heet.backend.dto.projection.LoanSummaryProjection;
import com.heet.backend.dto.projection.MerchantSpendProjection;
import com.heet.backend.dto.projection.PaymentMethodSumProjection;
import com.heet.backend.dto.response.AnalyticsOverviewResponse;
import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MoneyWrappedResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.LoanRecordRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.UserPrincipal;
import com.heet.backend.service.wrapped.MoneyWrappedInsightEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyWrappedServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LoanRecordRepository loanRecordRepository;

    @Mock
    private AnalyticsService analyticsService;

    @Spy
    private MoneyWrappedInsightEngine insightEngine = new MoneyWrappedInsightEngine();

    @InjectMocks
    private MoneyWrappedService moneyWrappedService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        UserPrincipal principal = new UserPrincipal(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Fixed clock simulating 2026-10-15 (October 2026)
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-10-15T00:00:00Z"),
                java.time.ZoneId.of("UTC")));
    }

    @Test
    void getMoneyWrapped_CalculatesAllSectionsSuccessfully() {
        int year = 2026;
        int month = 8;

        // 1. Analytics overview mock
        when(analyticsService.getOverview(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(AnalyticsOverviewResponse.builder()
                        .totalIncome(BigDecimal.valueOf(5000))
                        .totalExpense(BigDecimal.valueOf(2000))
                        .netSavings(BigDecimal.valueOf(3000))
                        .transactionCount(25L)
                        .build());

        // 2. Payment method breakdown mock
        PaymentMethodSumProjection pCash = new PaymentMethodSumProjection() {
            @Override public PaymentMethod getPaymentMethod() { return PaymentMethod.CASH; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(500); }
            @Override public Long getTransactionCount() { return 5L; }
        };
        PaymentMethodSumProjection pDigital = new PaymentMethodSumProjection() {
            @Override public PaymentMethod getPaymentMethod() { return PaymentMethod.DIGITAL; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(1500); }
            @Override public Long getTransactionCount() { return 20L; }
        };
        when(transactionRepository.findPaymentMethodSumsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(pCash, pDigital));

        // 3. Category breakdown mock
        when(analyticsService.getCategoryBreakdown(any(LocalDate.class), any(LocalDate.class), eq(TransactionType.EXPENSE)))
                .thenReturn(List.of(
                        CategoryBreakdownResponse.builder()
                                .categoryId(1L)
                                .categoryName("Groceries")
                                .amount(BigDecimal.valueOf(1200))
                                .percentage(60.0)
                                .transactionCount(15L)
                                .build(),
                        CategoryBreakdownResponse.builder()
                                .categoryId(2L)
                                .categoryName("Dining")
                                .amount(BigDecimal.valueOf(800))
                                .percentage(40.0)
                                .transactionCount(10L)
                                .build()
                ));

        // 4. Merchant analysis mock
        MerchantSpendProjection topMerchantProj = new MerchantSpendProjection() {
            @Override public String getMerchant() { return "Supermart"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(1000); }
            @Override public Long getTransactionCount() { return 8L; }
        };
        when(transactionRepository.findTopMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(topMerchantProj));
        when(transactionRepository.findMostFrequentMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(topMerchantProj));
        when(transactionRepository.countDistinctMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(4L);

        // 5. Spending spikes mock
        DailySpendProjection dailyProj = new DailySpendProjection() {
            @Override public Integer getYear() { return 2026; }
            @Override public Integer getMonth() { return 8; }
            @Override public Integer getDay() { return 15; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(700); }
            @Override public Long getTransactionCount() { return 3L; }
        };
        when(transactionRepository.findTopSpendingDaysByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(dailyProj));

        Category cat = new Category();
        cat.setName("Electronics");
        Transaction largestTx = new Transaction();
        largestTx.setId(101L);
        largestTx.setMerchant("Gadget World");
        largestTx.setAmount(BigDecimal.valueOf(600));
        largestTx.setCategory(cat);
        largestTx.setTransactionDate(LocalDateTime.of(2026, 8, 15, 12, 0));

        when(transactionRepository.findLargestExpenseTransactionsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(largestTx));

        // 6. Small purchases mock
        when(transactionRepository.sumSmallPurchasesByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(80));
        when(transactionRepository.countSmallPurchasesByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(6L);

        // 7. Active loan statistics mock
        LoanSummaryProjection lentProj = new LoanSummaryProjection() {
            @Override public LoanType getType() { return LoanType.LENT; }
            @Override public BigDecimal getTotalRemainingAmount() { return BigDecimal.valueOf(300); }
            @Override public Long getCount() { return 2L; }
        };
        LoanSummaryProjection borrowedProj = new LoanSummaryProjection() {
            @Override public LoanType getType() { return LoanType.BORROWED; }
            @Override public BigDecimal getTotalRemainingAmount() { return BigDecimal.valueOf(100); }
            @Override public Long getCount() { return 1L; }
        };
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of(lentProj, borrowedProj));

        // 8. Month-over-month mock (previous month expense = 2500)
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(2500));

        MoneyWrappedResponse response = moneyWrappedService.getMoneyWrapped(year, month);

        assertNotNull(response);
        assertEquals(2026, response.getYear());
        assertEquals(8, response.getMonth());

        // Period
        assertEquals(LocalDate.of(2026, 8, 1), response.getPeriod().getStartDate());
        assertEquals(LocalDate.of(2026, 8, 31), response.getPeriod().getEndDate());

        // Overview
        assertEquals(BigDecimal.valueOf(5000), response.getSpendingOverview().getTotalIncome());
        assertEquals(BigDecimal.valueOf(2000), response.getSpendingOverview().getTotalExpense());
        assertEquals(BigDecimal.valueOf(3000), response.getSpendingOverview().getNetSavings());
        assertEquals(60.0, response.getSpendingOverview().getSavingsRate());
        assertEquals(25L, response.getSpendingOverview().getTotalTransactions());

        // Payment methods
        assertEquals(BigDecimal.valueOf(500), response.getPaymentMethodBreakdown().getCashExpense());
        assertEquals(25.0, response.getPaymentMethodBreakdown().getCashPercentage());
        assertEquals(BigDecimal.valueOf(1500), response.getPaymentMethodBreakdown().getDigitalExpense());
        assertEquals(75.0, response.getPaymentMethodBreakdown().getDigitalPercentage());
        assertEquals("DIGITAL", response.getPaymentMethodBreakdown().getDominantMethod());

        // Category distribution
        assertEquals("Groceries", response.getCategoryDistribution().getTopCategory().getCategoryName());
        assertEquals(2, response.getCategoryDistribution().getTotalCategoriesUsed());
        assertEquals(2, response.getCategoryDistribution().getCategories().size());

        // Merchant analysis
        assertEquals("Supermart", response.getMerchantAnalysis().getTopMerchantBySpend().getMerchant());
        assertEquals("Supermart", response.getMerchantAnalysis().getMostFrequentMerchant().getMerchant());
        assertEquals(4L, response.getMerchantAnalysis().getUniqueMerchantsCount());

        // Spikes
        assertEquals(LocalDate.of(2026, 8, 15), response.getSpendingSpikes().getPeakSpendingDay().getDate());
        assertEquals(BigDecimal.valueOf(700), response.getSpendingSpikes().getPeakSpendingDay().getTotalAmount());
        assertEquals("Gadget World", response.getSpendingSpikes().getLargestSingleTransaction().getMerchant());
        assertEquals(BigDecimal.valueOf(600), response.getSpendingSpikes().getLargestSingleTransaction().getAmount());

        // Small purchases
        assertEquals(BigDecimal.valueOf(80), response.getSmallPurchases().getTotalAmount());
        assertEquals(6L, response.getSmallPurchases().getTransactionCount());
        assertEquals(4.0, response.getSmallPurchases().getPercentageOfTotalExpense());

        // Debt statistics
        assertEquals(BigDecimal.valueOf(300), response.getDebtStatistics().getActiveLentTotal());
        assertEquals(BigDecimal.valueOf(100), response.getDebtStatistics().getActiveBorrowedTotal());
        assertEquals(BigDecimal.valueOf(200), response.getDebtStatistics().getNetDebtPosition());

        // Month over month
        assertEquals(BigDecimal.valueOf(2500), response.getMonthOverMonth().getPreviousMonthExpense());
        assertEquals(BigDecimal.valueOf(-500), response.getMonthOverMonth().getExpenseChangeAmount());
        assertEquals(-20.0, response.getMonthOverMonth().getExpenseChangePercentage());
        assertEquals("DECREASED", response.getMonthOverMonth().getTrend());

        // Insights
        assertNotNull(response.getInsights());
        assertFalse(response.getInsights().isEmpty());
        assertTrue(response.getInsights().size() <= 5);
    }

    @Test
    void getMoneyWrapped_EmptyMonth_HandlesZeroStateGracefully() {
        int year = 2026;
        int month = 9;

        when(analyticsService.getOverview(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(AnalyticsOverviewResponse.builder()
                        .totalIncome(BigDecimal.ZERO)
                        .totalExpense(BigDecimal.ZERO)
                        .netSavings(BigDecimal.ZERO)
                        .transactionCount(0L)
                        .build());

        when(transactionRepository.findPaymentMethodSumsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of());
        when(analyticsService.getCategoryBreakdown(any(LocalDate.class), any(LocalDate.class), eq(TransactionType.EXPENSE)))
                .thenReturn(List.of());
        when(transactionRepository.findTopMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.findMostFrequentMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.countDistinctMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(0L);
        when(transactionRepository.findTopSpendingDaysByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.findLargestExpenseTransactionsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(List.of());
        when(transactionRepository.sumSmallPurchasesByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countSmallPurchasesByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any()))
                .thenReturn(0L);
        when(loanRecordRepository.findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE)))
                .thenReturn(List.of());
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        MoneyWrappedResponse response = moneyWrappedService.getMoneyWrapped(year, month);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getSpendingOverview().getTotalExpense());
        assertEquals(0.0, response.getSpendingOverview().getSavingsRate());
        assertEquals(0.0, response.getPaymentMethodBreakdown().getCashPercentage());
        assertNull(response.getCategoryDistribution().getTopCategory());
        assertNull(response.getMerchantAnalysis().getTopMerchantBySpend());
        assertNull(response.getSpendingSpikes().getPeakSpendingDay());
        assertNull(response.getSpendingSpikes().getLargestSingleTransaction());
        assertEquals(0.0, response.getSmallPurchases().getPercentageOfTotalExpense());
        assertEquals(BigDecimal.ZERO, response.getDebtStatistics().getNetDebtPosition());
        assertEquals("STABLE", response.getMonthOverMonth().getTrend());

        // Check fallback insight
        assertEquals(1, response.getInsights().size());
        assertEquals("NO_ACTIVITY", response.getInsights().get(0).getCode());
    }

    @Test
    void getMoneyWrapped_StrictUserIsolationEnforced() {
        int year = 2026;
        int month = 8;

        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        moneyWrappedService.getMoneyWrapped(year, month);

        verify(transactionRepository).findPaymentMethodSumsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any());
        verify(transactionRepository).findTopMerchantsByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any());
        verify(loanRecordRepository).findActiveLoanSummaryByUserId(eq(1L), eq(LoanStatus.ACTIVE));
    }

    @Test
    void getMoneyWrapped_CurrentMonth_BeforeFinalDay_ThrowsIllegalArgumentException() {
        // Current month in clock is October 2026 (2026-10-15)
        // October has 31 days, so October 15 is before the final calendar day
        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 10));

        assertTrue(ex.getMessage().contains("Money Wrapped for October 2026 is not available until 2026-10-31"));
    }

    @Test
    void getMoneyWrapped_CurrentMonth_OnFinalDay_IsAvailable() {
        // Advance clock to October 31, 2026 (the last calendar day of October)
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-10-31T12:00:00Z"),
                java.time.ZoneId.of("UTC")));

        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));

        MoneyWrappedResponse response = moneyWrappedService.getMoneyWrapped(2026, 10);
        assertNotNull(response);
        assertEquals(2026, response.getYear());
        assertEquals(10, response.getMonth());
    }

    @Test
    void getMoneyWrapped_OneMonthAgo_IsAvailable() {
        // 1 month ago is September 2026 (2026, 9)
        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        MoneyWrappedResponse response = moneyWrappedService.getMoneyWrapped(2026, 9);
        assertNotNull(response);
        assertEquals(2026, response.getYear());
        assertEquals(9, response.getMonth());
    }

    @Test
    void getMoneyWrapped_TwoMonthsAgo_IsAvailable() {
        // 2 months ago is August 2026 (2026, 8)
        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        MoneyWrappedResponse response = moneyWrappedService.getMoneyWrapped(2026, 8);
        assertNotNull(response);
        assertEquals(2026, response.getYear());
        assertEquals(8, response.getMonth());
    }

    @Test
    void getMoneyWrapped_ThreeMonthsAgo_ThrowsIllegalArgumentException() {
        // 3 months ago is July 2026 (2026, 7) - excluded
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 7));

        assertTrue(ex.getMessage().contains("Money Wrapped is only available for the rolling 3-month window"));
    }

    @Test
    void getMoneyWrapped_FourOrMoreMonthsAgo_ThrowsIllegalArgumentException() {
        // 4+ months ago is May 2026 (2026, 5) - excluded
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 5));

        assertTrue(ex.getMessage().contains("Money Wrapped is only available for the rolling 3-month window"));
    }

    @Test
    void getMoneyWrapped_FutureMonth_ThrowsIllegalArgumentException() {
        // Future month is November 2026 when clock is in October 2026
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 11));

        assertEquals("Money Wrapped is not available for future months", ex.getMessage());
    }

    @Test
    void getMoneyWrapped_RollingTransitionWhenCalendarMonthChanges() {
        // When clock is in October 2026:
        // August 2026 is available
        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> moneyWrappedService.getMoneyWrapped(2026, 8));

        // Advance clock to November 5, 2026 (2026-11-05)
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-11-05T00:00:00Z"),
                java.time.ZoneId.of("UTC")));

        // In November 2026:
        // October 2026 is 1 month ago and completed -> available!
        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));
        assertDoesNotThrow(() -> moneyWrappedService.getMoneyWrapped(2026, 10));

        // September 2026 is 2 months ago and completed -> available!
        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 9));
        assertDoesNotThrow(() -> moneyWrappedService.getMoneyWrapped(2026, 9));

        // November 2026 is current month before Nov 30 -> unavailable!
        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 11));
        IllegalArgumentException exNov = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 11));
        assertTrue(exNov.getMessage().contains("not available until 2026-11-30"));

        // August 2026 is now 3 months ago and excluded!
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 8));
        assertTrue(ex.getMessage().contains("Money Wrapped is only available for the rolling 3-month window"));
    }

    @Test
    void getMoneyWrapped_UnderlyingTransactionsRemainUntouched() {
        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        // August 2026 is available when clock is 2026-10-15
        moneyWrappedService.getMoneyWrapped(2026, 8);

        // Verify transactionRepository delete is NEVER called
        verify(transactionRepository, never()).delete(any());
        verify(transactionRepository, never()).deleteAll(any());
    }

    // ==========================================
    // CALENDAR-CORRECT AVAILABILITY RULE TESTS
    // ==========================================

    @Test
    void availability_February28_NonLeapYear_IsAvailable() {
        // 2027 is a non-leap year (February has 28 days)
        // On Feb 28, 2027, February Wrapped must be available
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2027-02-28T10:00:00Z"),
                java.time.ZoneId.of("UTC")));

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2027, 2));

        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> moneyWrappedService.getMoneyWrapped(2027, 2));
    }

    @Test
    void availability_February28_LeapYear_IsUnavailable() {
        // 2028 is a leap year (February has 29 days)
        // On Feb 28, 2028, February Wrapped must NOT be available yet
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2028-02-28T10:00:00Z"),
                java.time.ZoneId.of("UTC")));

        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2028, 2));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2028, 2));
        assertTrue(ex.getMessage().contains("Money Wrapped for February 2028 is not available until 2028-02-29"));
    }

    @Test
    void availability_February29_LeapYear_IsAvailable() {
        // 2028 is a leap year (February has 29 days)
        // On Feb 29, 2028, February Wrapped must be available
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2028-02-29T15:00:00Z"),
                java.time.ZoneId.of("UTC")));

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2028, 2));

        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> moneyWrappedService.getMoneyWrapped(2028, 2));
    }

    @Test
    void availability_April29_Unavailable_April30_Available() {
        // April has 30 days
        // April 29 -> unavailable
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-04-29T23:59:00Z"),
                java.time.ZoneId.of("UTC")));

        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 4));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2026, 4));
        assertTrue(ex.getMessage().contains("not available until 2026-04-30"));

        // April 30 -> available
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-04-30T00:01:00Z"),
                java.time.ZoneId.of("UTC")));

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 4));
    }

    @Test
    void availability_June30_Available() {
        // June has 30 days. On June 30, June Wrapped is available
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-06-30T12:00:00Z"),
                java.time.ZoneId.of("UTC")));

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 6));
    }

    @Test
    void availability_October30_Unavailable_October31_Available() {
        // October has 31 days
        // October 30 -> unavailable
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-10-30T23:59:59Z"),
                java.time.ZoneId.of("UTC")));

        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));

        // October 31 -> available
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-10-31T00:00:01Z"),
                java.time.ZoneId.of("UTC")));

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));
    }

    @Test
    void availability_December30_Unavailable_December31_Available() {
        // December has 31 days
        // December 30 -> unavailable
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-12-30T23:59:59Z"),
                java.time.ZoneId.of("UTC")));

        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 12));

        // December 31 -> available
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-12-31T08:00:00Z"),
                java.time.ZoneId.of("UTC")));

        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 12));
    }

    @Test
    void availability_January1_Rollover_PreviousDecemberAvailable_JanuaryUnavailable() {
        // Year transition: January 1, 2027
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2027-01-01T00:05:00Z"),
                java.time.ZoneId.of("UTC")));

        // December 2026 remains completed and available (1 month ago)
        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 12));

        // November 2026 remains completed and available (2 months ago)
        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 11));

        // January 2027 (current month) is unavailable until January 31, 2027
        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2027, 1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                moneyWrappedService.getMoneyWrapped(2027, 1));
        assertTrue(ex.getMessage().contains("Money Wrapped for January 2027 is not available until 2027-01-31"));

        // October 2026 is now 3 months ago and excluded
        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));
    }

    @Test
    void availability_CompletedMonthRemainsAccessibleAfterMonthChanges() {
        // Month transition: November 1, 2026
        moneyWrappedService.setClock(java.time.Clock.fixed(
                java.time.Instant.parse("2026-11-01T00:01:00Z"),
                java.time.ZoneId.of("UTC")));

        // October 2026 is completed and remains available
        assertTrue(moneyWrappedService.isMoneyWrappedAvailable(2026, 10));

        when(analyticsService.getOverview(any(), any()))
                .thenReturn(AnalyticsOverviewResponse.builder().build());
        when(analyticsService.getCategoryBreakdown(any(), any(), any()))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> moneyWrappedService.getMoneyWrapped(2026, 10));

        // November 2026 is unavailable until November 30
        assertFalse(moneyWrappedService.isMoneyWrappedAvailable(2026, 11));
    }
}
