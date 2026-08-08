package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.response.ExpenseByCategoryResponse;
import com.heet.backend.dto.response.IncomeExpenseResponse;
import com.heet.backend.dto.response.MonthlySummaryResponse;
import com.heet.backend.dto.response.TopCategoryResponse;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        BigDecimal totalIncome = getSumByType(TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getSumByType(TransactionType.EXPENSE, startDate, endDate);
        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        Long totalTransactions = transactionRepository.countByTransactionDateBetween(startDate, endDate);
        if (totalTransactions == null) {
            totalTransactions = 0L;
        }

        return MonthlySummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .totalTransactions(totalTransactions)
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseByCategoryResponse getExpenseByCategory(int year, int month) {
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        BigDecimal totalExpense = getSumByType(TransactionType.EXPENSE, startDate, endDate);
        List<CategoryExpenseProjection> projections = transactionRepository
                .findCategoryExpenseBreakdown(TransactionType.EXPENSE, startDate, endDate);

        List<ExpenseByCategoryResponse.CategoryExpenseItem> items = projections.stream()
                .map(proj -> {
                    BigDecimal amount = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
                    double percentage = calculatePercentage(amount, totalExpense);
                    return ExpenseByCategoryResponse.CategoryExpenseItem.builder()
                            .categoryId(proj.getCategoryId())
                            .categoryName(proj.getCategoryName() != null ? proj.getCategoryName() : "Uncategorized")
                            .color(proj.getColor())
                            .icon(proj.getIcon())
                            .totalAmount(amount)
                            .build();
                })
                .collect(Collectors.toList());

        return ExpenseByCategoryResponse.builder()
                .totalExpense(totalExpense)
                .categories(items)
                .build();
    }

    @Transactional(readOnly = true)
    public IncomeExpenseResponse getIncomeVsExpense(int year, int month) {
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        BigDecimal totalIncome = getSumByType(TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getSumByType(TransactionType.EXPENSE, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);
        BigDecimal combined = totalIncome.add(totalExpense);

        double incomePct = calculatePercentage(totalIncome, combined);
        double expensePct = calculatePercentage(totalExpense, combined);

        return IncomeExpenseResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopCategoryResponse> getTopCategories(int year, int month, int limit) {
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        int effectiveLimit = limit > 0 ? limit : 5;
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        BigDecimal totalExpense = getSumByType(TransactionType.EXPENSE, startDate, endDate);
        List<CategoryExpenseProjection> projections = transactionRepository
                .findTopExpenseCategories(TransactionType.EXPENSE, startDate, endDate, pageable);

        return projections.stream()
                .map(proj -> {
                    BigDecimal amount = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
                    double percentage = calculatePercentage(amount, totalExpense);
                    return TopCategoryResponse.builder()
                            .categoryId(proj.getCategoryId())
                            .categoryName(proj.getCategoryName() != null ? proj.getCategoryName() : "Uncategorized")
                            .color(proj.getColor())
                            .icon(proj.getIcon())
                            .totalAmount(amount)
                            .transactionCount(proj.getTransactionCount() != null ? proj.getTransactionCount() : 0L)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private BigDecimal getSumByType(TransactionType type, LocalDateTime start, LocalDateTime end) {
        BigDecimal sum = transactionRepository.sumAmountByTransactionTypeAndDateBetween(type, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private double calculatePercentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0 || part == null) {
            return 0.0;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
    }

    private LocalDateTime getStartDate(int year, int month) {
        return YearMonth.of(year, month).atDay(1).atStartOfDay();
    }

    private LocalDateTime getEndDate(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX);
    }
}
