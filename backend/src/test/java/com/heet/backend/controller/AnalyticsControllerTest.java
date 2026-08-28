package com.heet.backend.controller;

import com.heet.backend.dto.response.AnalyticsOverviewResponse;
import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MonthlyTrendResponse;
import com.heet.backend.dto.response.PeriodComparisonResponse;
import com.heet.backend.dto.response.PeriodSummaryResponse;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    private AnalyticsOverviewResponse overviewResponse;
    private CategoryBreakdownResponse categoryBreakdownResponse;
    private MonthlyTrendResponse monthlyTrendResponse;
    private PeriodComparisonResponse periodComparisonResponse;

    @BeforeEach
    void setUp() {
        overviewResponse = AnalyticsOverviewResponse.builder()
                .totalIncome(BigDecimal.valueOf(5000))
                .totalExpense(BigDecimal.valueOf(2000))
                .netSavings(BigDecimal.valueOf(3000))
                .transactionCount(10L)
                .build();

        categoryBreakdownResponse = CategoryBreakdownResponse.builder()
                .categoryId(1L)
                .categoryName("Food")
                .amount(BigDecimal.valueOf(800))
                .percentage(40.0)
                .transactionCount(4L)
                .build();

        monthlyTrendResponse = MonthlyTrendResponse.builder()
                .year(2026)
                .month(1)
                .income(BigDecimal.valueOf(5000))
                .expense(BigDecimal.valueOf(2000))
                .netSavings(BigDecimal.valueOf(3000))
                .build();

        periodComparisonResponse = PeriodComparisonResponse.builder()
                .currentPeriod(PeriodSummaryResponse.builder()
                        .income(BigDecimal.valueOf(5000))
                        .expense(BigDecimal.valueOf(2000))
                        .netSavings(BigDecimal.valueOf(3000))
                        .build())
                .previousPeriod(PeriodSummaryResponse.builder()
                        .income(BigDecimal.valueOf(4000))
                        .expense(BigDecimal.valueOf(1500))
                        .netSavings(BigDecimal.valueOf(2500))
                        .build())
                .incomeChangePercentage(25.0)
                .expenseChangePercentage(33.33)
                .savingsChangePercentage(20.0)
                .build();
    }

    @Test
    void getOverview_Returns200OK() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(analyticsService.getOverview(start, end)).thenReturn(overviewResponse);

        ResponseEntity<AnalyticsOverviewResponse> response = analyticsController.getOverview(start, end);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BigDecimal.valueOf(5000), response.getBody().getTotalIncome());
        assertEquals(10L, response.getBody().getTransactionCount());
    }

    @Test
    void getCategoryBreakdown_Returns200OK() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(analyticsService.getCategoryBreakdown(start, end, TransactionType.EXPENSE))
                .thenReturn(List.of(categoryBreakdownResponse));

        ResponseEntity<List<CategoryBreakdownResponse>> response =
                analyticsController.getCategoryBreakdown(start, end, TransactionType.EXPENSE);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Food", response.getBody().get(0).getCategoryName());
        assertEquals(40.0, response.getBody().get(0).getPercentage());
    }

    @Test
    void getMonthlyTrend_Returns200OK() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 31);

        when(analyticsService.getMonthlyTrend(start, end))
                .thenReturn(List.of(monthlyTrendResponse));

        ResponseEntity<List<MonthlyTrendResponse>> response =
                analyticsController.getMonthlyTrend(start, end);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(2026, response.getBody().get(0).getYear());
        assertEquals(1, response.getBody().get(0).getMonth());
    }

    @Test
    void getPeriodComparison_Returns200OK() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);

        when(analyticsService.getPeriodComparison(start, end))
                .thenReturn(periodComparisonResponse);

        ResponseEntity<PeriodComparisonResponse> response =
                analyticsController.getPeriodComparison(start, end);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(25.0, response.getBody().getIncomeChangePercentage());
        assertEquals(BigDecimal.valueOf(5000), response.getBody().getCurrentPeriod().getIncome());
    }
}
