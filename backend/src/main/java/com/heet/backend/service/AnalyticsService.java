package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.projection.MonthlyTypeSumProjection;
import com.heet.backend.dto.response.AnalyticsOverviewResponse;
import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MonthlyTrendResponse;
import com.heet.backend.dto.response.PeriodComparisonResponse;
import com.heet.backend.dto.response.PeriodSummaryResponse;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverview(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        BigDecimal totalIncome = getSumByTypeAndUser(userId, TransactionType.INCOME, startDateTime, endDateTime);
        BigDecimal totalExpense = getSumByTypeAndUser(userId, TransactionType.EXPENSE, startDateTime, endDateTime);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        Long transactionCount = transactionRepository.countByUserIdAndTransactionDateBetween(userId, startDateTime, endDateTime);
        if (transactionCount == null) {
            transactionCount = 0L;
        }

        return AnalyticsOverviewResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .transactionCount(transactionCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownResponse> getCategoryBreakdown(LocalDate startDate, LocalDate endDate, TransactionType type) {
        validateDateRange(startDate, endDate);
        Long userId = SecurityUtils.getCurrentUserId();
        TransactionType targetType = type != null ? type : TransactionType.EXPENSE;
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        BigDecimal totalAmount = getSumByTypeAndUser(userId, targetType, startDateTime, endDateTime);
        List<CategoryExpenseProjection> projections = transactionRepository
                .findCategoryExpenseBreakdownByUserId(userId, targetType, startDateTime, endDateTime);

        return projections.stream()
                .map(proj -> {
                    BigDecimal amount = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
                    Double percentage = 0.0;
                    if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = amount.multiply(BigDecimal.valueOf(100))
                                .divide(totalAmount, 2, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                    return CategoryBreakdownResponse.builder()
                            .categoryId(proj.getCategoryId())
                            .categoryName(proj.getCategoryName() != null ? proj.getCategoryName() : "Uncategorized")
                            .amount(amount)
                            .percentage(percentage)
                            .transactionCount(proj.getTransactionCount() != null ? proj.getTransactionCount() : 0L)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MonthlyTrendResponse> getMonthlyTrend(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<MonthlyTypeSumProjection> sums = transactionRepository.findMonthlyTypeSumsByUserId(
                userId, startDateTime, endDateTime);

        Map<YearMonth, Map<TransactionType, BigDecimal>> monthData = new HashMap<>();
        if (sums != null) {
            for (MonthlyTypeSumProjection sum : sums) {
                if (sum.getYear() != null && sum.getMonth() != null) {
                    YearMonth ym = YearMonth.of(sum.getYear(), sum.getMonth());
                    monthData.computeIfAbsent(ym, k -> new HashMap<>())
                            .put(sum.getTransactionType(), sum.getTotalAmount() != null ? sum.getTotalAmount() : BigDecimal.ZERO);
                }
            }
        }

        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);

        List<MonthlyTrendResponse> results = new ArrayList<>();
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            Map<TransactionType, BigDecimal> typeMap = monthData.getOrDefault(current, Collections.emptyMap());
            BigDecimal income = typeMap.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO);
            BigDecimal expense = typeMap.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO);
            BigDecimal netSavings = income.subtract(expense);

            results.add(MonthlyTrendResponse.builder()
                    .year(current.getYear())
                    .month(current.getMonthValue())
                    .income(income)
                    .expense(expense)
                    .netSavings(netSavings)
                    .build());

            current = current.plusMonths(1);
        }

        return results;
    }

    @Transactional(readOnly = true)
    public PeriodComparisonResponse getPeriodComparison(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        Long userId = SecurityUtils.getCurrentUserId();

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(days - 1);

        LocalDateTime currentStart = startDate.atStartOfDay();
        LocalDateTime currentEnd = endDate.atTime(LocalTime.MAX);
        LocalDateTime prevStart = prevStartDate.atStartOfDay();
        LocalDateTime prevEnd = prevEndDate.atTime(LocalTime.MAX);

        BigDecimal currentIncome = getSumByTypeAndUser(userId, TransactionType.INCOME, currentStart, currentEnd);
        BigDecimal currentExpense = getSumByTypeAndUser(userId, TransactionType.EXPENSE, currentStart, currentEnd);
        BigDecimal currentNetSavings = currentIncome.subtract(currentExpense);

        BigDecimal prevIncome = getSumByTypeAndUser(userId, TransactionType.INCOME, prevStart, prevEnd);
        BigDecimal prevExpense = getSumByTypeAndUser(userId, TransactionType.EXPENSE, prevStart, prevEnd);
        BigDecimal prevNetSavings = prevIncome.subtract(prevExpense);

        Double incomeChangePercentage = calculatePercentageChange(currentIncome, prevIncome);
        Double expenseChangePercentage = calculatePercentageChange(currentExpense, prevExpense);
        Double savingsChangePercentage = calculatePercentageChange(currentNetSavings, prevNetSavings);

        PeriodSummaryResponse currentSummary = PeriodSummaryResponse.builder()
                .income(currentIncome)
                .expense(currentExpense)
                .netSavings(currentNetSavings)
                .build();

        PeriodSummaryResponse prevSummary = PeriodSummaryResponse.builder()
                .income(prevIncome)
                .expense(prevExpense)
                .netSavings(prevNetSavings)
                .build();

        return PeriodComparisonResponse.builder()
                .currentPeriod(currentSummary)
                .previousPeriod(prevSummary)
                .incomeChangePercentage(incomeChangePercentage)
                .expenseChangePercentage(expenseChangePercentage)
                .savingsChangePercentage(savingsChangePercentage)
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    private BigDecimal getSumByTypeAndUser(Long userId, TransactionType type, LocalDateTime start, LocalDateTime end) {
        BigDecimal sum = transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(userId, type, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private Double calculatePercentageChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) {
                return 0.0;
            }
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : -100.0;
        }
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        BigDecimal diff = current.subtract(previous);
        BigDecimal absPrev = previous.abs();
        return diff.multiply(BigDecimal.valueOf(100))
                .divide(absPrev, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
