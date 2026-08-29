package com.heet.backend.service.wrapped;

import com.heet.backend.dto.response.MoneyWrappedResponse;
import com.heet.backend.dto.response.MoneyWrappedResponse.DebtStatistics;
import com.heet.backend.dto.response.MoneyWrappedResponse.MerchantAnalysis;
import com.heet.backend.dto.response.MoneyWrappedResponse.MerchantSummary;
import com.heet.backend.dto.response.MoneyWrappedResponse.MonthOverMonth;
import com.heet.backend.dto.response.MoneyWrappedResponse.PaymentMethodBreakdown;
import com.heet.backend.dto.response.MoneyWrappedResponse.SmallPurchases;
import com.heet.backend.dto.response.MoneyWrappedResponse.SpendingOverview;
import com.heet.backend.dto.response.MoneyWrappedResponse.SpendingSpikes;
import com.heet.backend.dto.response.MoneyWrappedResponse.WrappedInsight;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class MoneyWrappedInsightEngine {

    private static final int MAX_INSIGHTS = 5;

    public List<WrappedInsight> generateInsights(
            SpendingOverview overview,
            PaymentMethodBreakdown paymentMethodBreakdown,
            MoneyWrappedResponse.CategoryDistribution categoryDistribution,
            MerchantAnalysis merchantAnalysis,
            SpendingSpikes spendingSpikes,
            SmallPurchases smallPurchases,
            DebtStatistics debtStatistics,
            MonthOverMonth monthOverMonth) {

        List<WrappedInsight> candidates = new ArrayList<>();

        BigDecimal totalExpense = overview != null && overview.getTotalExpense() != null
                ? overview.getTotalExpense()
                : BigDecimal.ZERO;
        BigDecimal totalIncome = overview != null && overview.getTotalIncome() != null
                ? overview.getTotalIncome()
                : BigDecimal.ZERO;
        Long totalTransactions = overview != null && overview.getTotalTransactions() != null
                ? overview.getTotalTransactions()
                : 0L;

        // Rule 1: No activity fallback
        if (totalExpense.compareTo(BigDecimal.ZERO) == 0 &&
                totalIncome.compareTo(BigDecimal.ZERO) == 0 &&
                totalTransactions == 0L) {
            candidates.add(WrappedInsight.builder()
                    .code("NO_ACTIVITY")
                    .title("A Quiet Month")
                    .message("No transactions were recorded for this month.")
                    .impact("INFO")
                    .priorityScore(100)
                    .build());
            return candidates;
        }

        // Rule 2: Savings Rate & Cashflow
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0 && overview.getSavingsRate() != null) {
            Double savingsRate = overview.getSavingsRate();
            if (savingsRate >= 30.0) {
                candidates.add(WrappedInsight.builder()
                        .code("HIGH_SAVINGS_RATE")
                        .title("Super Saver")
                        .message(String.format("You saved %.1f%% of your income this month! Outstanding financial discipline.", savingsRate))
                        .impact("POSITIVE")
                        .priorityScore(95)
                        .build());
            } else if (savingsRate >= 10.0) {
                candidates.add(WrappedInsight.builder()
                        .code("HEALTHY_SAVINGS_RATE")
                        .title("Solid Savings")
                        .message(String.format("You maintained a positive savings rate of %.1f%% this month.", savingsRate))
                        .impact("POSITIVE")
                        .priorityScore(70)
                        .build());
            }
        }
        if (overview != null && overview.getNetSavings() != null && overview.getNetSavings().compareTo(BigDecimal.ZERO) < 0) {
            candidates.add(WrappedInsight.builder()
                    .code("OVERSPENDING_ALERT")
                    .title("Deficit Month")
                    .message(String.format("Expenses exceeded your income by %s this month.", overview.getNetSavings().abs()))
                    .impact("WARNING")
                    .priorityScore(90)
                    .build());
        }

        // Rule 3: Month-over-Month Spending Trend
        if (monthOverMonth != null && monthOverMonth.getPreviousMonthExpense() != null &&
                monthOverMonth.getPreviousMonthExpense().compareTo(BigDecimal.ZERO) > 0 &&
                monthOverMonth.getExpenseChangePercentage() != null) {
            Double changePct = monthOverMonth.getExpenseChangePercentage();
            if (changePct <= -15.0) {
                candidates.add(WrappedInsight.builder()
                        .code("SPENDING_REDUCED")
                        .title("Spending Cut Down")
                        .message(String.format("Your spending dropped by %.1f%% compared to last month — great job curbing expenses!", Math.abs(changePct)))
                        .impact("POSITIVE")
                        .priorityScore(85)
                        .build());
            } else if (changePct >= 25.0) {
                candidates.add(WrappedInsight.builder()
                        .code("SPENDING_SURGE")
                        .title("Spending Surge")
                        .message(String.format("Your spending rose by %.1f%% compared to last month.", changePct))
                        .impact("WARNING")
                        .priorityScore(85)
                        .build());
            }
        }

        // Rule 4: Dominant Category
        if (categoryDistribution != null && categoryDistribution.getTopCategory() != null &&
                categoryDistribution.getTopCategory().getPercentage() != null &&
                categoryDistribution.getTopCategory().getPercentage() >= 35.0) {
            candidates.add(WrappedInsight.builder()
                    .code("CATEGORY_DOMINANCE")
                    .title(categoryDistribution.getTopCategory().getCategoryName() + " Dominance")
                    .message(String.format("%s took up %.1f%% of your total expenses (%s across %d transactions).",
                            categoryDistribution.getTopCategory().getCategoryName(),
                            categoryDistribution.getTopCategory().getPercentage(),
                            categoryDistribution.getTopCategory().getAmount(),
                            categoryDistribution.getTopCategory().getTransactionCount()))
                    .impact("INFO")
                    .priorityScore(80)
                    .build());
        }

        // Rule 5: Unusually Large Single Expense
        if (spendingSpikes != null && spendingSpikes.getLargestSingleTransaction() != null &&
                totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            MoneyWrappedResponse.LargestTransaction largest = spendingSpikes.getLargestSingleTransaction();
            if (largest.getAmount() != null) {
                double share = largest.getAmount().multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                if (share >= 25.0) {
                    String merchantDetail = largest.getMerchant() != null && !largest.getMerchant().isBlank()
                            ? " at " + largest.getMerchant()
                            : "";
                    candidates.add(WrappedInsight.builder()
                            .code("LARGE_SINGLE_EXPENSE")
                            .title("Major Purchase")
                            .message(String.format("Your largest single expense was %s%s, accounting for %.1f%% of your monthly spend.",
                                    largest.getAmount(), merchantDetail, share))
                            .impact("INFO")
                            .priorityScore(75)
                            .build());
                }
            }
        }

        // Rule 6: Active Debt / Lending Exposure
        if (debtStatistics != null) {
            if (debtStatistics.getActiveBorrowedTotal() != null &&
                    debtStatistics.getActiveBorrowedTotal().compareTo(BigDecimal.ZERO) > 0) {
                candidates.add(WrappedInsight.builder()
                        .code("ACTIVE_BORROWED_EXPOSURE")
                        .title("Outstanding Borrowed Balance")
                        .message(String.format("You have an active borrowed balance of %s across %d loan(s) pending repayment.",
                                debtStatistics.getActiveBorrowedTotal(),
                                debtStatistics.getActiveBorrowedCount() != null ? debtStatistics.getActiveBorrowedCount() : 1L))
                        .impact("WARNING")
                        .priorityScore(72)
                        .build());
            } else if (debtStatistics.getActiveLentTotal() != null &&
                    debtStatistics.getActiveLentTotal().compareTo(BigDecimal.ZERO) > 0) {
                candidates.add(WrappedInsight.builder()
                        .code("ACTIVE_LENT_EXPOSURE")
                        .title("Pending Receivables")
                        .message(String.format("You have %s lent out across %d active loan(s) waiting to be collected.",
                                debtStatistics.getActiveLentTotal(),
                                debtStatistics.getActiveLentCount() != null ? debtStatistics.getActiveLentCount() : 1L))
                        .impact("INFO")
                        .priorityScore(55)
                        .build());
            }
        }

        // Rule 7: Peak Spending Day Surge
        if (spendingSpikes != null && spendingSpikes.getPeakSpendingDay() != null &&
                totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            MoneyWrappedResponse.PeakSpendingDay peakDay = spendingSpikes.getPeakSpendingDay();
            if (peakDay.getTotalAmount() != null && peakDay.getTransactionCount() != null && peakDay.getTransactionCount() >= 2) {
                double dayShare = peakDay.getTotalAmount().multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                if (dayShare >= 30.0) {
                    candidates.add(WrappedInsight.builder()
                            .code("PEAK_DAY_SURGE")
                            .title("Peak Spending Day")
                            .message(String.format("Your biggest spending day was %s with %s spent across %d transactions (%.1f%% of monthly expenses).",
                                    peakDay.getDate(), peakDay.getTotalAmount(), peakDay.getTransactionCount(), dayShare))
                            .impact("INFO")
                            .priorityScore(68)
                            .build());
                }
            }
        }

        // Rule 8: Small Purchases Accumulating (Micro-spending)
        if (smallPurchases != null && smallPurchases.getTransactionCount() != null &&
                smallPurchases.getTransactionCount() >= 5 &&
                smallPurchases.getTotalAmount() != null &&
                smallPurchases.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            candidates.add(WrappedInsight.builder()
                    .code("MICRO_PURCHASES_ACCUMULATION")
                    .title("Small Habits Add Up")
                    .message(String.format("You had %d micro-purchases (under %s) adding up to %s (%.1f%% of total spending).",
                            smallPurchases.getTransactionCount(),
                            smallPurchases.getThreshold(),
                            smallPurchases.getTotalAmount(),
                            smallPurchases.getPercentageOfTotalExpense() != null ? smallPurchases.getPercentageOfTotalExpense() : 0.0))
                    .impact("NEUTRAL")
                    .priorityScore(65)
                    .build());
        }

        // Rule 9: Merchant Concentration
        if (merchantAnalysis != null && merchantAnalysis.getTopMerchantBySpend() != null &&
                totalExpense.compareTo(BigDecimal.ZERO) > 0 &&
                merchantAnalysis.getUniqueMerchantsCount() != null &&
                merchantAnalysis.getUniqueMerchantsCount() >= 3) {
            MerchantSummary topMerchant = merchantAnalysis.getTopMerchantBySpend();
            if (topMerchant.getTotalSpent() != null) {
                double merchantShare = topMerchant.getTotalSpent().multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP)
                        .doubleValue();
                if (merchantShare >= 30.0) {
                    candidates.add(WrappedInsight.builder()
                            .code("MERCHANT_CONCENTRATION")
                            .title("Favorite Spot")
                            .message(String.format("You spent %s at %s across %d transactions (%.1f%% of total spending).",
                                    topMerchant.getTotalSpent(),
                                    topMerchant.getMerchant(),
                                    topMerchant.getTransactionCount() != null ? topMerchant.getTransactionCount() : 1L,
                                    merchantShare))
                            .impact("INFO")
                            .priorityScore(60)
                            .build());
                }
            }
        }

        // Rule 10: Cash vs Digital Habit
        if (paymentMethodBreakdown != null && totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentMethodBreakdown.getDigitalPercentage() != null && paymentMethodBreakdown.getDigitalPercentage() >= 80.0) {
                candidates.add(WrappedInsight.builder()
                        .code("CASHLESS_HABIT")
                        .title("Digital Native")
                        .message(String.format("%.1f%% of your expenses were paid digitally.", paymentMethodBreakdown.getDigitalPercentage()))
                        .impact("INFO")
                        .priorityScore(50)
                        .build());
            } else if (paymentMethodBreakdown.getCashPercentage() != null && paymentMethodBreakdown.getCashPercentage() >= 60.0) {
                candidates.add(WrappedInsight.builder()
                        .code("CASH_PREFERENCE")
                        .title("Cash First")
                        .message(String.format("%.1f%% of your expenses were paid in cash.", paymentMethodBreakdown.getCashPercentage()))
                        .impact("INFO")
                        .priorityScore(50)
                        .build());
            }
        }

        // Sort by priorityScore DESC and limit to MAX_INSIGHTS (5)
        return candidates.stream()
                .sorted(Comparator.comparingInt(WrappedInsight::getPriorityScore).reversed())
                .limit(MAX_INSIGHTS)
                .toList();
    }
}
