package com.heet.backend.service;

import com.heet.backend.dto.projection.DailySpendProjection;
import com.heet.backend.dto.projection.LoanSummaryProjection;
import com.heet.backend.dto.projection.MerchantSpendProjection;
import com.heet.backend.dto.projection.PaymentMethodSumProjection;
import com.heet.backend.dto.response.AnalyticsOverviewResponse;
import com.heet.backend.dto.response.CategoryBreakdownResponse;
import com.heet.backend.dto.response.MoneyWrappedResponse;
import com.heet.backend.dto.response.MoneyWrappedResponse.CategoryDistribution;
import com.heet.backend.dto.response.MoneyWrappedResponse.DebtStatistics;
import com.heet.backend.dto.response.MoneyWrappedResponse.LargestTransaction;
import com.heet.backend.dto.response.MoneyWrappedResponse.MerchantAnalysis;
import com.heet.backend.dto.response.MoneyWrappedResponse.MerchantSummary;
import com.heet.backend.dto.response.MoneyWrappedResponse.MonthOverMonth;
import com.heet.backend.dto.response.MoneyWrappedResponse.PaymentMethodBreakdown;
import com.heet.backend.dto.response.MoneyWrappedResponse.PaymentMethodDetail;
import com.heet.backend.dto.response.MoneyWrappedResponse.PeakSpendingDay;
import com.heet.backend.dto.response.MoneyWrappedResponse.PeriodInfo;
import com.heet.backend.dto.response.MoneyWrappedResponse.SmallPurchases;
import com.heet.backend.dto.response.MoneyWrappedResponse.SpendingOverview;
import com.heet.backend.dto.response.MoneyWrappedResponse.SpendingSpikes;
import com.heet.backend.dto.response.MoneyWrappedResponse.WrappedInsight;
import com.heet.backend.entity.Transaction;
import com.heet.backend.enums.LoanStatus;
import com.heet.backend.enums.LoanType;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.LoanRecordRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.SecurityUtils;
import com.heet.backend.service.wrapped.MoneyWrappedInsightEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class MoneyWrappedService {

    public static final BigDecimal SMALL_PURCHASE_THRESHOLD = new BigDecimal("20.00");

    private final TransactionRepository transactionRepository;
    private final LoanRecordRepository loanRecordRepository;
    private final AnalyticsService analyticsService;
    private final MoneyWrappedInsightEngine insightEngine;
    private final String timezoneName;

    private Clock clock;

    public MoneyWrappedService(
            TransactionRepository transactionRepository,
            LoanRecordRepository loanRecordRepository,
            AnalyticsService analyticsService,
            MoneyWrappedInsightEngine insightEngine) {
        this(transactionRepository, loanRecordRepository, analyticsService, insightEngine, "Asia/Kolkata");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MoneyWrappedService(
            TransactionRepository transactionRepository,
            LoanRecordRepository loanRecordRepository,
            AnalyticsService analyticsService,
            MoneyWrappedInsightEngine insightEngine,
            @Value("${app.timezone:Asia/Kolkata}") String timezoneName) {
        this.transactionRepository = transactionRepository;
        this.loanRecordRepository = loanRecordRepository;
        this.analyticsService = analyticsService;
        this.insightEngine = insightEngine;
        this.timezoneName = (timezoneName != null && !timezoneName.isBlank()) ? timezoneName : "Asia/Kolkata";
        this.clock = Clock.system(getZoneId());
    }

    public void setClock(java.time.Clock clock) {
        this.clock = clock;
    }

    public ZoneId getZoneId() {
        try {
            if (timezoneName != null && !timezoneName.isBlank()) {
                return ZoneId.of(timezoneName);
            }
        } catch (Exception ignored) {
        }
        return ZoneId.systemDefault();
    }

    /**
     * Checks if Money Wrapped is released and available for the given year and month.
     * Available ONLY on or after the last calendar day of the month, within the rolling 3-month window.
     */
    public boolean isMoneyWrappedAvailable(int year, int month) {
        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate today = LocalDate.now(clock != null ? clock : Clock.system(getZoneId()));
            YearMonth currentYearMonth = YearMonth.from(today);
            YearMonth earliestAllowed = currentYearMonth.minusMonths(2);

            if (yearMonth.isAfter(currentYearMonth) || yearMonth.isBefore(earliestAllowed)) {
                return false;
            }

            LocalDate endOfMonth = yearMonth.atEndOfMonth();
            return !today.isBefore(endOfMonth);
        } catch (Exception ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public MoneyWrappedResponse getMoneyWrapped(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate today = LocalDate.now(clock != null ? clock : Clock.system(getZoneId()));
        YearMonth currentYearMonth = YearMonth.from(today);
        YearMonth earliestAllowed = currentYearMonth.minusMonths(2);

        if (yearMonth.isAfter(currentYearMonth)) {
            throw new IllegalArgumentException("Money Wrapped is not available for future months");
        }
        if (yearMonth.isBefore(earliestAllowed)) {
            throw new IllegalArgumentException(String.format(
                    "Money Wrapped is only available for the rolling 3-month window (%s to %s)",
                    earliestAllowed, currentYearMonth));
        }

        LocalDate endOfMonth = yearMonth.atEndOfMonth();
        if (today.isBefore(endOfMonth)) {
            String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            throw new IllegalArgumentException(String.format(
                    "Money Wrapped for %s %d is not available until %s",
                    monthName, yearMonth.getYear(), endOfMonth));
        }

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Long userId = SecurityUtils.getCurrentUserId();

        // 1. Period Information
        PeriodInfo periodInfo = PeriodInfo.builder()
                .startDate(startDate)
                .endDate(endDate)
                .build();

        // 2. Spending Overview (reusing AnalyticsService)
        AnalyticsOverviewResponse overviewResponse = analyticsService.getOverview(startDate, endDate);
        BigDecimal totalIncome = overviewResponse.getTotalIncome() != null ? overviewResponse.getTotalIncome() : BigDecimal.ZERO;
        BigDecimal totalExpense = overviewResponse.getTotalExpense() != null ? overviewResponse.getTotalExpense() : BigDecimal.ZERO;
        BigDecimal netSavings = overviewResponse.getNetSavings() != null ? overviewResponse.getNetSavings() : BigDecimal.ZERO;
        Long totalTransactions = overviewResponse.getTransactionCount() != null ? overviewResponse.getTransactionCount() : 0L;

        Double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        SpendingOverview spendingOverview = SpendingOverview.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .netSavings(netSavings)
                .savingsRate(savingsRate)
                .totalTransactions(totalTransactions)
                .build();

        // 3. Payment Method Breakdown (Cash vs Digital & all PaymentMethod enum values)
        PaymentMethodBreakdown paymentMethodBreakdown = buildPaymentMethodBreakdown(userId, startDateTime, endDateTime, totalExpense);

        // 4. Category Distribution (top 5 categories, global percentages via AnalyticsService)
        List<CategoryBreakdownResponse> categoryBreakdowns = analyticsService.getCategoryBreakdown(startDate, endDate, TransactionType.EXPENSE);
        CategoryBreakdownResponse topCategory = categoryBreakdowns.isEmpty() ? null : categoryBreakdowns.get(0);
        List<CategoryBreakdownResponse> top5Categories = categoryBreakdowns.stream().limit(5).toList();
        CategoryDistribution categoryDistribution = CategoryDistribution.builder()
                .topCategory(topCategory)
                .totalCategoriesUsed(categoryBreakdowns.size())
                .categories(top5Categories)
                .build();

        // 5. Merchant Analysis (top 5 merchants, unique count, most frequent)
        MerchantAnalysis merchantAnalysis = buildMerchantAnalysis(userId, startDateTime, endDateTime);

        // 6. Spending Spikes (highest spending day & largest single transaction)
        SpendingSpikes spendingSpikes = buildSpendingSpikes(userId, startDateTime, endDateTime);

        // 7. Small Purchases Accumulation (threshold <= 20.00)
        SmallPurchases smallPurchases = buildSmallPurchases(userId, startDateTime, endDateTime, totalExpense);

        // 8. Debt Statistics (active lending & borrowing exposure)
        DebtStatistics debtStatistics = buildDebtStatistics(userId);

        // 9. Month-over-Month Comparison
        MonthOverMonth monthOverMonth = buildMonthOverMonth(userId, yearMonth, totalExpense);

        // 10. Ranked Deterministic Insights
        List<WrappedInsight> insights = insightEngine.generateInsights(
                spendingOverview,
                paymentMethodBreakdown,
                categoryDistribution,
                merchantAnalysis,
                spendingSpikes,
                smallPurchases,
                debtStatistics,
                monthOverMonth);

        return MoneyWrappedResponse.builder()
                .year(year)
                .month(month)
                .period(periodInfo)
                .spendingOverview(spendingOverview)
                .paymentMethodBreakdown(paymentMethodBreakdown)
                .categoryDistribution(categoryDistribution)
                .merchantAnalysis(merchantAnalysis)
                .spendingSpikes(spendingSpikes)
                .smallPurchases(smallPurchases)
                .debtStatistics(debtStatistics)
                .monthOverMonth(monthOverMonth)
                .insights(insights)
                .build();
    }

    private PaymentMethodBreakdown buildPaymentMethodBreakdown(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            BigDecimal totalExpense) {

        List<PaymentMethodSumProjection> projections = transactionRepository
                .findPaymentMethodSumsByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime);

        Map<PaymentMethod, PaymentMethodSumProjection> map = new EnumMap<>(PaymentMethod.class);
        if (projections != null) {
            for (PaymentMethodSumProjection p : projections) {
                if (p.getPaymentMethod() != null) {
                    map.put(p.getPaymentMethod(), p);
                }
            }
        }

        List<PaymentMethodDetail> methodDetails = new ArrayList<>();
        BigDecimal maxAmount = BigDecimal.ZERO;
        String dominantMethod = null;

        BigDecimal cashExpense = BigDecimal.ZERO;
        BigDecimal digitalExpense = BigDecimal.ZERO;
        Double cashPercentage = 0.0;
        Double digitalPercentage = 0.0;

        for (PaymentMethod method : PaymentMethod.values()) {
            PaymentMethodSumProjection proj = map.get(method);
            BigDecimal amount = (proj != null && proj.getTotalAmount() != null) ? proj.getTotalAmount() : BigDecimal.ZERO;
            Long count = (proj != null && proj.getTransactionCount() != null) ? proj.getTransactionCount() : 0L;

            Double percentage = 0.0;
            if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpense, 2, RoundingMode.HALF_UP)
                        .doubleValue();
            }

            if (amount.compareTo(maxAmount) > 0) {
                maxAmount = amount;
                dominantMethod = method.name();
            }

            if (method == PaymentMethod.CASH) {
                cashExpense = amount;
                cashPercentage = percentage;
            } else if (method == PaymentMethod.DIGITAL) {
                digitalExpense = amount;
                digitalPercentage = percentage;
            }

            methodDetails.add(PaymentMethodDetail.builder()
                    .paymentMethod(method)
                    .totalAmount(amount)
                    .percentage(percentage)
                    .transactionCount(count)
                    .build());
        }

        return PaymentMethodBreakdown.builder()
                .cashExpense(cashExpense)
                .digitalExpense(digitalExpense)
                .cashPercentage(cashPercentage)
                .digitalPercentage(digitalPercentage)
                .dominantMethod(dominantMethod)
                .methodDetails(methodDetails)
                .build();
    }

    private MerchantAnalysis buildMerchantAnalysis(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        List<MerchantSpendProjection> topMerchantsProjections = transactionRepository
                .findTopMerchantsByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime, PageRequest.of(0, 5));

        List<MerchantSummary> topMerchants = topMerchantsProjections.stream()
                .map(p -> MerchantSummary.builder()
                        .merchant(p.getMerchant())
                        .totalSpent(p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO)
                        .transactionCount(p.getTransactionCount() != null ? p.getTransactionCount() : 0L)
                        .build())
                .toList();

        List<MerchantSpendProjection> frequentProjections = transactionRepository
                .findMostFrequentMerchantsByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime, PageRequest.of(0, 1));

        MerchantSummary mostFrequentMerchant = frequentProjections.isEmpty() ? null :
                MerchantSummary.builder()
                        .merchant(frequentProjections.get(0).getMerchant())
                        .totalSpent(frequentProjections.get(0).getTotalAmount() != null ? frequentProjections.get(0).getTotalAmount() : BigDecimal.ZERO)
                        .transactionCount(frequentProjections.get(0).getTransactionCount() != null ? frequentProjections.get(0).getTransactionCount() : 0L)
                        .build();

        MerchantSummary topMerchantBySpend = topMerchants.isEmpty() ? null : topMerchants.get(0);

        Long uniqueMerchantsCount = transactionRepository
                .countDistinctMerchantsByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime);
        if (uniqueMerchantsCount == null) {
            uniqueMerchantsCount = 0L;
        }

        return MerchantAnalysis.builder()
                .topMerchantBySpend(topMerchantBySpend)
                .mostFrequentMerchant(mostFrequentMerchant)
                .uniqueMerchantsCount(uniqueMerchantsCount)
                .topMerchants(topMerchants)
                .build();
    }

    private SpendingSpikes buildSpendingSpikes(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        List<DailySpendProjection> topDays = transactionRepository
                .findTopSpendingDaysByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime, PageRequest.of(0, 1));

        PeakSpendingDay peakSpendingDay = null;
        if (!topDays.isEmpty()) {
            DailySpendProjection dayProj = topDays.get(0);
            if (dayProj.getYear() != null && dayProj.getMonth() != null && dayProj.getDay() != null) {
                peakSpendingDay = PeakSpendingDay.builder()
                        .date(LocalDate.of(dayProj.getYear(), dayProj.getMonth(), dayProj.getDay()))
                        .totalAmount(dayProj.getTotalAmount() != null ? dayProj.getTotalAmount() : BigDecimal.ZERO)
                        .transactionCount(dayProj.getTransactionCount() != null ? dayProj.getTransactionCount() : 0L)
                        .build();
            }
        }

        List<Transaction> largestList = transactionRepository
                .findLargestExpenseTransactionsByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime, PageRequest.of(0, 1));

        LargestTransaction largestSingleTransaction = null;
        if (!largestList.isEmpty()) {
            Transaction t = largestList.get(0);
            largestSingleTransaction = LargestTransaction.builder()
                    .id(t.getId())
                    .merchant(t.getMerchant())
                    .amount(t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
                    .categoryName(t.getCategory() != null ? t.getCategory().getName() : "Uncategorized")
                    .transactionDate(t.getTransactionDate())
                    .build();
        }

        return SpendingSpikes.builder()
                .peakSpendingDay(peakSpendingDay)
                .largestSingleTransaction(largestSingleTransaction)
                .build();
    }

    private SmallPurchases buildSmallPurchases(
            Long userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            BigDecimal totalExpense) {

        BigDecimal smallTotal = transactionRepository
                .sumSmallPurchasesByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime, SMALL_PURCHASE_THRESHOLD);
        if (smallTotal == null) {
            smallTotal = BigDecimal.ZERO;
        }

        Long smallCount = transactionRepository
                .countSmallPurchasesByUserId(userId, TransactionType.EXPENSE, startDateTime, endDateTime, SMALL_PURCHASE_THRESHOLD);
        if (smallCount == null) {
            smallCount = 0L;
        }

        Double smallPercentage = 0.0;
        if (totalExpense.compareTo(BigDecimal.ZERO) > 0) {
            smallPercentage = smallTotal.multiply(BigDecimal.valueOf(100))
                    .divide(totalExpense, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return SmallPurchases.builder()
                .threshold(SMALL_PURCHASE_THRESHOLD)
                .totalAmount(smallTotal)
                .transactionCount(smallCount)
                .percentageOfTotalExpense(smallPercentage)
                .build();
    }

    private DebtStatistics buildDebtStatistics(Long userId) {
        List<LoanSummaryProjection> activeLoans = loanRecordRepository
                .findActiveLoanSummaryByUserId(userId, LoanStatus.ACTIVE);

        BigDecimal activeLentTotal = BigDecimal.ZERO;
        Long activeLentCount = 0L;
        BigDecimal activeBorrowedTotal = BigDecimal.ZERO;
        Long activeBorrowedCount = 0L;

        if (activeLoans != null) {
            for (LoanSummaryProjection loan : activeLoans) {
                if (loan.getType() == LoanType.LENT) {
                    activeLentTotal = loan.getTotalRemainingAmount() != null ? loan.getTotalRemainingAmount() : BigDecimal.ZERO;
                    activeLentCount = loan.getCount() != null ? loan.getCount() : 0L;
                } else if (loan.getType() == LoanType.BORROWED) {
                    activeBorrowedTotal = loan.getTotalRemainingAmount() != null ? loan.getTotalRemainingAmount() : BigDecimal.ZERO;
                    activeBorrowedCount = loan.getCount() != null ? loan.getCount() : 0L;
                }
            }
        }

        BigDecimal netDebtPosition = activeLentTotal.subtract(activeBorrowedTotal);

        return DebtStatistics.builder()
                .activeLentTotal(activeLentTotal)
                .activeBorrowedTotal(activeBorrowedTotal)
                .netDebtPosition(netDebtPosition)
                .activeLentCount(activeLentCount)
                .activeBorrowedCount(activeBorrowedCount)
                .build();
    }

    private MonthOverMonth buildMonthOverMonth(Long userId, YearMonth yearMonth, BigDecimal totalExpense) {
        YearMonth prevMonth = yearMonth.minusMonths(1);
        LocalDateTime prevStart = prevMonth.atDay(1).atStartOfDay();
        LocalDateTime prevEnd = prevMonth.atEndOfMonth().atTime(LocalTime.MAX);

        BigDecimal prevExpense = transactionRepository
                .sumAmountByUserIdAndTransactionTypeAndDateBetween(userId, TransactionType.EXPENSE, prevStart, prevEnd);
        if (prevExpense == null) {
            prevExpense = BigDecimal.ZERO;
        }

        BigDecimal changeAmount = totalExpense.subtract(prevExpense);

        Double changePercentage;
        if (prevExpense.compareTo(BigDecimal.ZERO) == 0) {
            if (totalExpense.compareTo(BigDecimal.ZERO) == 0) {
                changePercentage = 0.0;
            } else {
                changePercentage = totalExpense.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : -100.0;
            }
        } else {
            changePercentage = changeAmount.multiply(BigDecimal.valueOf(100))
                    .divide(prevExpense.abs(), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        String trend = "STABLE";
        if (changeAmount.compareTo(BigDecimal.ZERO) > 0) {
            trend = "INCREASED";
        } else if (changeAmount.compareTo(BigDecimal.ZERO) < 0) {
            trend = "DECREASED";
        }

        return MonthOverMonth.builder()
                .previousMonthExpense(prevExpense)
                .expenseChangeAmount(changeAmount)
                .expenseChangePercentage(changePercentage)
                .trend(trend)
                .build();
    }
}
