package com.heet.backend.dto.response;

import com.heet.backend.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyWrappedResponse {

    private Integer year;
    private Integer month;
    private PeriodInfo period;
    private SpendingOverview spendingOverview;
    private PaymentMethodBreakdown paymentMethodBreakdown;
    private CategoryDistribution categoryDistribution;
    private MerchantAnalysis merchantAnalysis;
    private SpendingSpikes spendingSpikes;
    private SmallPurchases smallPurchases;
    private DebtStatistics debtStatistics;
    private MonthOverMonth monthOverMonth;
    private List<WrappedInsight> insights;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodInfo {
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendingOverview {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netSavings;
        private Double savingsRate;
        private Long totalTransactions;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodBreakdown {
        private BigDecimal cashExpense;
        private BigDecimal digitalExpense;
        private Double cashPercentage;
        private Double digitalPercentage;
        private String dominantMethod;
        private List<PaymentMethodDetail> methodDetails;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodDetail {
        private PaymentMethod paymentMethod;
        private BigDecimal totalAmount;
        private Double percentage;
        private Long transactionCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDistribution {
        private CategoryBreakdownResponse topCategory;
        private Integer totalCategoriesUsed;
        private List<CategoryBreakdownResponse> categories;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantAnalysis {
        private MerchantSummary topMerchantBySpend;
        private MerchantSummary mostFrequentMerchant;
        private Long uniqueMerchantsCount;
        private List<MerchantSummary> topMerchants;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantSummary {
        private String merchant;
        private BigDecimal totalSpent;
        private Long transactionCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpendingSpikes {
        private PeakSpendingDay peakSpendingDay;
        private LargestTransaction largestSingleTransaction;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeakSpendingDay {
        private LocalDate date;
        private BigDecimal totalAmount;
        private Long transactionCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LargestTransaction {
        private Long id;
        private String merchant;
        private BigDecimal amount;
        private String categoryName;
        private LocalDateTime transactionDate;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmallPurchases {
        private BigDecimal threshold;
        private BigDecimal totalAmount;
        private Long transactionCount;
        private Double percentageOfTotalExpense;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebtStatistics {
        private BigDecimal activeLentTotal;
        private BigDecimal activeBorrowedTotal;
        private BigDecimal netDebtPosition;
        private Long activeLentCount;
        private Long activeBorrowedCount;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthOverMonth {
        private BigDecimal previousMonthExpense;
        private BigDecimal expenseChangeAmount;
        private Double expenseChangePercentage;
        private String trend;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WrappedInsight {
        private String code;
        private String title;
        private String message;
        private String impact;
        private Integer priorityScore;
    }
}
