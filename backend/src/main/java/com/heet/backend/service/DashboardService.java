package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.response.ExpenseByCategoryResponse;
import com.heet.backend.dto.response.IncomeExpenseResponse;
import com.heet.backend.dto.response.MonthlySummaryResponse;
import com.heet.backend.dto.response.TopCategoryResponse;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import com.heet.backend.dto.projection.LoanSummaryProjection;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.repository.LoanRecordRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final LoanRecordRepository loanRecordRepository;

    public DashboardService(TransactionRepository transactionRepository, LoanRecordRepository loanRecordRepository) {
        this.transactionRepository = transactionRepository;
        this.loanRecordRepository = loanRecordRepository;
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        BigDecimal totalIncome = getSumByTypeAndUser(userId, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getSumByTypeAndUser(userId, TransactionType.EXPENSE, startDate, endDate);

        BigDecimal totalLent = BigDecimal.ZERO;
        BigDecimal totalBorrowed = BigDecimal.ZERO;

        List<LoanSummaryProjection> activeLoans = loanRecordRepository
                .findActiveLoanSummaryByUserId(userId, LoanStatus.ACTIVE);
        if (activeLoans != null) {
            for (LoanSummaryProjection loan : activeLoans) {
                if (loan.getType() == LoanType.LENT && loan.getTotalRemainingAmount() != null) {
                    totalLent = loan.getTotalRemainingAmount();
                } else if (loan.getType() == LoanType.BORROWED && loan.getTotalRemainingAmount() != null) {
                    totalBorrowed = loan.getTotalRemainingAmount();
                }
            }
        }

        // Total available money / net balance:
        // Lend money: - amount
        // Borrow money: + amount
        // Receive lent-money repayment: + amount (reduces outstanding lent)
        // Repay borrowed money: - amount (reduces outstanding borrowed)
        // Net effect = + totalBorrowed - totalLent
        BigDecimal netSavings = totalIncome.subtract(totalExpense).add(totalBorrowed).subtract(totalLent);
        Long totalTransactions = transactionRepository.countByUserIdAndTransactionDateBetween(userId, startDate, endDate);
        if (totalTransactions == null) {
            totalTransactions = 0L;
        }

        return MonthlySummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .totalTransactions(totalTransactions)
                .totalLent(totalLent)
                .totalDebt(totalBorrowed)
                .totalBorrowed(totalBorrowed)
                .build();
    }

    @Transactional(readOnly = true)
    public ExpenseByCategoryResponse getExpenseByCategory(int year, int month) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        BigDecimal totalExpense = getSumByTypeAndUser(userId, TransactionType.EXPENSE, startDate, endDate);
        List<CategoryExpenseProjection> projections = transactionRepository
                .findCategoryExpenseBreakdownByUserId(userId, TransactionType.EXPENSE, startDate, endDate);

        List<ExpenseByCategoryResponse.CategoryExpenseItem> items = projections.stream()
                .map(proj -> {
                    BigDecimal amount = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
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
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        BigDecimal totalIncome = getSumByTypeAndUser(userId, TransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpense = getSumByTypeAndUser(userId, TransactionType.EXPENSE, startDate, endDate);

        BigDecimal totalLent = BigDecimal.ZERO;
        BigDecimal totalBorrowed = BigDecimal.ZERO;

        List<LoanSummaryProjection> activeLoans = loanRecordRepository
                .findActiveLoanSummaryByUserId(userId, LoanStatus.ACTIVE);
        if (activeLoans != null) {
            for (LoanSummaryProjection loan : activeLoans) {
                if (loan.getType() == LoanType.LENT && loan.getTotalRemainingAmount() != null) {
                    totalLent = loan.getTotalRemainingAmount();
                } else if (loan.getType() == LoanType.BORROWED && loan.getTotalRemainingAmount() != null) {
                    totalBorrowed = loan.getTotalRemainingAmount();
                }
            }
        }

        BigDecimal balance = totalIncome.subtract(totalExpense).add(totalBorrowed).subtract(totalLent);

        return IncomeExpenseResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(balance)
                .totalLent(totalLent)
                .totalDebt(totalBorrowed)
                .totalBorrowed(totalBorrowed)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopCategoryResponse> getTopCategories(int year, int month, int limit) {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDateTime startDate = getStartDate(year, month);
        LocalDateTime endDate = getEndDate(year, month);

        int effectiveLimit = limit > 0 ? limit : 5;
        Pageable pageable = PageRequest.of(0, effectiveLimit);

        List<CategoryExpenseProjection> projections = transactionRepository
                .findTopExpenseCategoriesByUserId(userId, TransactionType.EXPENSE, startDate, endDate, pageable);

        return projections.stream()
                .map(proj -> {
                    BigDecimal amount = proj.getTotalAmount() != null ? proj.getTotalAmount() : BigDecimal.ZERO;
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

    private BigDecimal getSumByTypeAndUser(Long userId, TransactionType type, LocalDateTime start, LocalDateTime end) {
        BigDecimal sum = transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(userId, type, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private LocalDateTime getStartDate(int year, int month) {
        return YearMonth.of(year, month).atDay(1).atStartOfDay();
    }

    private LocalDateTime getEndDate(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX);
    }
}
