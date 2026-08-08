package com.heet.backend.controller;

import com.heet.backend.dto.response.ExpenseByCategoryResponse;
import com.heet.backend.dto.response.IncomeExpenseResponse;
import com.heet.backend.dto.response.MonthlySummaryResponse;
import com.heet.backend.dto.response.TopCategoryResponse;
import com.heet.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private MonthlySummaryResponse summaryResponse;
    private ExpenseByCategoryResponse expenseResponse;
    private IncomeExpenseResponse incomeExpenseResponse;
    private TopCategoryResponse topCategoryResponse;

    @BeforeEach
    void setUp() {
        summaryResponse = MonthlySummaryResponse.builder()
                .totalIncome(BigDecimal.valueOf(5000))
                .totalExpense(BigDecimal.valueOf(2000))
                .netSavings(BigDecimal.valueOf(3000))
                .totalTransactions(15L)
                .build();

        expenseResponse = ExpenseByCategoryResponse.builder()
                .totalExpense(BigDecimal.valueOf(2000))
                .categories(List.of())
                .build();

        incomeExpenseResponse = IncomeExpenseResponse.builder()
                .totalIncome(BigDecimal.valueOf(5000))
                .totalExpense(BigDecimal.valueOf(2000))
                .balance(BigDecimal.valueOf(3000))
                .incomePercentage(71.43)
                .expensePercentage(28.57)
                .build();

        topCategoryResponse = TopCategoryResponse.builder()
                .categoryId(1L)
                .categoryName("Food")
                .totalAmount(BigDecimal.valueOf(500))
                .transactionCount(5L)
                .percentage(25.0)
                .build();
    }

    @Test
    void getMonthlySummary_Returns200OK() {
        when(dashboardService.getMonthlySummary(2026, 8)).thenReturn(summaryResponse);

        ResponseEntity<MonthlySummaryResponse> result = dashboardController.getMonthlySummary(2026, 8);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(BigDecimal.valueOf(5000), result.getBody().getTotalIncome());
    }

    @Test
    void getExpenseByCategory_Returns200OK() {
        when(dashboardService.getExpenseByCategory(2026, 8)).thenReturn(expenseResponse);

        ResponseEntity<ExpenseByCategoryResponse> result = dashboardController.getExpenseByCategory(2026, 8);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(BigDecimal.valueOf(2000), result.getBody().getTotalExpense());
    }

    @Test
    void getIncomeVsExpense_Returns200OK() {
        when(dashboardService.getIncomeVsExpense(2026, 8)).thenReturn(incomeExpenseResponse);

        ResponseEntity<IncomeExpenseResponse> result = dashboardController.getIncomeVsExpense(2026, 8);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(BigDecimal.valueOf(3000), result.getBody().getBalance());
    }

    @Test
    void getTopCategories_Returns200OK() {
        when(dashboardService.getTopCategories(2026, 8, 5)).thenReturn(List.of(topCategoryResponse));

        ResponseEntity<List<TopCategoryResponse>> result = dashboardController.getTopCategories(2026, 8, 5);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().size());
        assertEquals("Food", result.getBody().get(0).getCategoryName());
    }
}
