package com.heet.backend.controller;

import com.heet.backend.dto.response.AnalyticsOverviewResponse;
import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MonthlyTrendResponse;
import com.heet.backend.dto.response.PeriodComparisonResponse;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AnalyticsOverviewResponse response = analyticsService.getOverview(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category-breakdown")
    public ResponseEntity<List<CategoryBreakdownResponse>> getCategoryBreakdown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type) {

        List<CategoryBreakdownResponse> response = analyticsService.getCategoryBreakdown(startDate, endDate, type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly-trend")
    public ResponseEntity<List<MonthlyTrendResponse>> getMonthlyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<MonthlyTrendResponse> response = analyticsService.getMonthlyTrend(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/period-comparison")
    public ResponseEntity<PeriodComparisonResponse> getPeriodComparison(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        PeriodComparisonResponse response = analyticsService.getPeriodComparison(startDate, endDate);
        return ResponseEntity.ok(response);
    }
}
