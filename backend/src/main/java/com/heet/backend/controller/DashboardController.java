package com.heet.backend.controller;

import com.heet.backend.dto.response.ExpenseByCategoryResponse;
import com.heet.backend.dto.response.IncomeExpenseResponse;
import com.heet.backend.dto.response.MonthlySummaryResponse;
import com.heet.backend.dto.response.TopCategoryResponse;
import com.heet.backend.service.DashboardService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth = resolveYearMonth(year, month);

        MonthlySummaryResponse response = dashboardService.getMonthlySummary(
                yearMonth.getYear(),
                yearMonth.getMonthValue());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/expense-by-category")
    public ResponseEntity<ExpenseByCategoryResponse> getExpenseByCategory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth = resolveYearMonth(year, month);

        ExpenseByCategoryResponse response = dashboardService.getExpenseByCategory(
                yearMonth.getYear(),
                yearMonth.getMonthValue());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/income-vs-expense")
    public ResponseEntity<IncomeExpenseResponse> getIncomeVsExpense(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth yearMonth = resolveYearMonth(year, month);

        IncomeExpenseResponse response = dashboardService.getIncomeVsExpense(
                yearMonth.getYear(),
                yearMonth.getMonthValue());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-categories")
    public ResponseEntity<List<TopCategoryResponse>> getTopCategories(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "5") int limit) {

        YearMonth yearMonth = resolveYearMonth(year, month);

        List<TopCategoryResponse> response = dashboardService.getTopCategories(
                yearMonth.getYear(),
                yearMonth.getMonthValue(),
                limit);

        return ResponseEntity.ok(response);
    }

    private YearMonth resolveYearMonth(Integer year, Integer month) {

        int targetYear = year != null
                ? year
                : LocalDate.now().getYear();

        int targetMonth = month != null
                ? month
                : LocalDate.now().getMonthValue();

        if (targetYear < 2000) {
            throw new IllegalArgumentException(
                    "Year must be greater than or equal to 2000");
        }

        if (targetMonth < 1 || targetMonth > 12) {
            throw new IllegalArgumentException(
                    "Month must be between 1 and 12");
        }

        return YearMonth.of(targetYear, targetMonth);
    }
}