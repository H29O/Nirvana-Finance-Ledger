package com.heet.backend.service.wrapped;

import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MoneyWrappedResponse.CategoryDistribution;
import com.heet.backend.dto.response.MoneyWrappedResponse.DebtStatistics;
import com.heet.backend.dto.response.MoneyWrappedResponse.LargestTransaction;
import com.heet.backend.dto.response.MoneyWrappedResponse.MerchantAnalysis;
import com.heet.backend.dto.response.MoneyWrappedResponse.MerchantSummary;
import com.heet.backend.dto.response.MoneyWrappedResponse.MonthOverMonth;
import com.heet.backend.dto.response.MoneyWrappedResponse.PaymentMethodBreakdown;
import com.heet.backend.dto.response.MoneyWrappedResponse.PeakSpendingDay;
import com.heet.backend.dto.response.MoneyWrappedResponse.SmallPurchases;
import com.heet.backend.dto.response.MoneyWrappedResponse.SpendingOverview;
import com.heet.backend.dto.response.MoneyWrappedResponse.SpendingSpikes;
import com.heet.backend.dto.response.MoneyWrappedResponse.WrappedInsight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoneyWrappedInsightEngineTest {

    private MoneyWrappedInsightEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MoneyWrappedInsightEngine();
    }

    @Test
    void generateInsights_NoActivity_ReturnsSingleNoActivityInsight() {
        SpendingOverview overview = SpendingOverview.builder()
                .totalIncome(BigDecimal.ZERO)
                .totalExpense(BigDecimal.ZERO)
                .netSavings(BigDecimal.ZERO)
                .savingsRate(0.0)
                .totalTransactions(0L)
                .build();

        List<WrappedInsight> insights = engine.generateInsights(
                overview, null, null, null, null, null, null, null);

        assertNotNull(insights);
        assertEquals(1, insights.size());
        assertEquals("NO_ACTIVITY", insights.get(0).getCode());
        assertEquals(100, insights.get(0).getPriorityScore());
        assertEquals("INFO", insights.get(0).getImpact());
    }

    @Test
    void generateInsights_HighSavingsRateAndDominantCategory_RanksCorrectly() {
        SpendingOverview overview = SpendingOverview.builder()
                .totalIncome(BigDecimal.valueOf(5000))
                .totalExpense(BigDecimal.valueOf(2000))
                .netSavings(BigDecimal.valueOf(3000))
                .savingsRate(60.0)
                .totalTransactions(20L)
                .build();

        CategoryDistribution categoryDistribution = CategoryDistribution.builder()
                .topCategory(CategoryBreakdownResponse.builder()
                        .categoryId(1L)
                        .categoryName("Food")
                        .amount(BigDecimal.valueOf(1000))
                        .percentage(50.0)
                        .transactionCount(10L)
                        .build())
                .totalCategoriesUsed(3)
                .categories(List.of())
                .build();

        PaymentMethodBreakdown paymentMethodBreakdown = PaymentMethodBreakdown.builder()
                .digitalExpense(BigDecimal.valueOf(1800))
                .digitalPercentage(90.0)
                .cashExpense(BigDecimal.valueOf(200))
                .cashPercentage(10.0)
                .dominantMethod("DIGITAL")
                .build();

        List<WrappedInsight> insights = engine.generateInsights(
                overview, paymentMethodBreakdown, categoryDistribution, null, null, null, null, null);

        assertNotNull(insights);
        assertTrue(insights.size() >= 2);
        // High savings rate (score 95) should be first, followed by category dominance (80) and cashless habit (50)
        assertEquals("HIGH_SAVINGS_RATE", insights.get(0).getCode());
        assertEquals("CATEGORY_DOMINANCE", insights.get(1).getCode());
        assertEquals("CASHLESS_HABIT", insights.get(2).getCode());
    }

    @Test
    void generateInsights_CapsAtMax5Insights() {
        SpendingOverview overview = SpendingOverview.builder()
                .totalIncome(BigDecimal.valueOf(10000))
                .totalExpense(BigDecimal.valueOf(6000))
                .netSavings(BigDecimal.valueOf(4000))
                .savingsRate(40.0) // rule: HIGH_SAVINGS_RATE (95)
                .totalTransactions(40L)
                .build();

        MonthOverMonth monthOverMonth = MonthOverMonth.builder()
                .previousMonthExpense(BigDecimal.valueOf(8000))
                .expenseChangeAmount(BigDecimal.valueOf(-2000))
                .expenseChangePercentage(-25.0) // rule: SPENDING_REDUCED (85)
                .trend("DECREASED")
                .build();

        CategoryDistribution categoryDistribution = CategoryDistribution.builder()
                .topCategory(CategoryBreakdownResponse.builder()
                        .categoryName("Shopping")
                        .amount(BigDecimal.valueOf(3000))
                        .percentage(50.0) // rule: CATEGORY_DOMINANCE (80)
                        .transactionCount(10L)
                        .build())
                .build();

        SpendingSpikes spikes = SpendingSpikes.builder()
                .largestSingleTransaction(LargestTransaction.builder()
                        .id(1L)
                        .merchant("Apple")
                        .amount(BigDecimal.valueOf(2000)) // 33.3% -> rule: LARGE_SINGLE_EXPENSE (75)
                        .transactionDate(LocalDateTime.now())
                        .build())
                .peakSpendingDay(PeakSpendingDay.builder()
                        .date(LocalDate.of(2026, 8, 15))
                        .totalAmount(BigDecimal.valueOf(2500)) // 41.6% -> rule: PEAK_DAY_SURGE (68)
                        .transactionCount(3L)
                        .build())
                .build();

        DebtStatistics debt = DebtStatistics.builder()
                .activeBorrowedTotal(BigDecimal.valueOf(500)) // rule: ACTIVE_BORROWED_EXPOSURE (72)
                .activeBorrowedCount(1L)
                .build();

        SmallPurchases small = SmallPurchases.builder()
                .threshold(BigDecimal.valueOf(20))
                .totalAmount(BigDecimal.valueOf(80))
                .transactionCount(6L) // rule: MICRO_PURCHASES_ACCUMULATION (65)
                .percentageOfTotalExpense(1.33)
                .build();

        MerchantAnalysis merchantAnalysis = MerchantAnalysis.builder()
                .uniqueMerchantsCount(5L)
                .topMerchantBySpend(MerchantSummary.builder()
                        .merchant("Amazon")
                        .totalSpent(BigDecimal.valueOf(2200)) // rule: MERCHANT_CONCENTRATION (60)
                        .transactionCount(5L)
                        .build())
                .build();

        PaymentMethodBreakdown payment = PaymentMethodBreakdown.builder()
                .digitalPercentage(95.0) // rule: CASHLESS_HABIT (50)
                .cashPercentage(5.0)
                .build();

        List<WrappedInsight> insights = engine.generateInsights(
                overview, payment, categoryDistribution, merchantAnalysis, spikes, small, debt, monthOverMonth);

        assertNotNull(insights);
        assertEquals(5, insights.size(), "Must cap at top 5 insights");
        // Verify strict descending order
        for (int i = 0; i < insights.size() - 1; i++) {
            assertTrue(insights.get(i).getPriorityScore() >= insights.get(i + 1).getPriorityScore());
        }
    }
}
