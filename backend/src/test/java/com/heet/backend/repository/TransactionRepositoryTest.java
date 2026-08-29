package com.heet.backend.repository;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.projection.DailySpendProjection;
import com.heet.backend.dto.projection.MerchantSpendProjection;
import com.heet.backend.dto.projection.MonthlyTypeSumProjection;
import com.heet.backend.dto.projection.PaymentMethodSumProjection;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.enums.EntrySource;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User user1;
    private User user2;
    private Category foodCategory;
    private Category utilCategory;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setName("Repo Test User 1");
        user1.setEmail("repo_user1_" + System.nanoTime() + "@example.com");
        user1.setPasswordHash("hash123");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("Repo Test User 2");
        user2.setEmail("repo_user2_" + System.nanoTime() + "@example.com");
        user2.setPasswordHash("hash123");
        user2 = userRepository.save(user2);

        foodCategory = new Category();
        foodCategory.setName("FoodCat_" + System.nanoTime());
        foodCategory.setColor("#FF5733");
        foodCategory.setIcon("utensils");
        foodCategory.setUser(user1);
        foodCategory = categoryRepository.save(foodCategory);

        utilCategory = new Category();
        utilCategory.setName("UtilCat_" + System.nanoTime());
        utilCategory.setColor("#33C1FF");
        utilCategory.setIcon("bolt");
        utilCategory.setUser(user1);
        utilCategory = categoryRepository.save(utilCategory);
    }

    private Transaction createTransaction(
            User user,
            BigDecimal amount,
            TransactionType type,
            PaymentMethod method,
            String merchant,
            Category category,
            LocalDateTime date) {

        Transaction t = new Transaction();
        t.setUser(user);
        t.setAmount(amount);
        t.setTransactionType(type);
        t.setPaymentMethod(method);
        t.setEntrySource(EntrySource.MANUAL);
        t.setMerchant(merchant);
        t.setCategory(category);
        t.setTransactionDate(date);
        t.setCreatedAt(date);
        t.setUpdatedAt(date);
        return transactionRepository.save(t);
    }

    @Test
    void sumAmountAndCount_DateRangeIncomeExpense_AggregatesAndEnforcesUserIsolation() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        // User 1 transactions
        createTransaction(user1, new BigDecimal("1000.00"), TransactionType.INCOME, PaymentMethod.DIGITAL, "Employer", null, LocalDateTime.of(2026, 8, 5, 10, 0));
        createTransaction(user1, new BigDecimal("250.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Grocery", foodCategory, LocalDateTime.of(2026, 8, 10, 12, 0));
        createTransaction(user1, new BigDecimal("50.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Cafe", foodCategory, LocalDateTime.of(2026, 8, 15, 14, 0));

        // User 1 transaction outside date range
        createTransaction(user1, new BigDecimal("500.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Past Store", foodCategory, LocalDateTime.of(2026, 7, 25, 10, 0));

        // User 2 transaction in same date range
        createTransaction(user2, new BigDecimal("999.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "User2 Store", null, LocalDateTime.of(2026, 8, 12, 10, 0));

        BigDecimal expenseSum = transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                user1.getId(), TransactionType.EXPENSE, start, end);
        BigDecimal incomeSum = transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                user1.getId(), TransactionType.INCOME, start, end);
        Long count = transactionRepository.countByUserIdAndTransactionDateBetween(user1.getId(), start, end);

        assertNotNull(expenseSum);
        assertEquals(0, new BigDecimal("300.00").compareTo(expenseSum));

        assertNotNull(incomeSum);
        assertEquals(0, new BigDecimal("1000.00").compareTo(incomeSum));

        assertEquals(3L, count);
    }

    @Test
    void findCategoryExpenseBreakdownByUserId_AggregatesAndSorts() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        createTransaction(user1, new BigDecimal("400.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Supermarket", foodCategory, LocalDateTime.of(2026, 8, 5, 10, 0));
        createTransaction(user1, new BigDecimal("200.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Diner", foodCategory, LocalDateTime.of(2026, 8, 6, 12, 0));
        createTransaction(user1, new BigDecimal("150.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Power Grid", utilCategory, LocalDateTime.of(2026, 8, 10, 10, 0));

        // User 2 expense (must be ignored)
        createTransaction(user2, new BigDecimal("800.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "User2 Spend", foodCategory, LocalDateTime.of(2026, 8, 10, 10, 0));

        List<CategoryExpenseProjection> breakdown = transactionRepository.findCategoryExpenseBreakdownByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end);

        assertNotNull(breakdown);
        assertEquals(2, breakdown.size());

        // Top category: Food (600.00, count 2)
        assertEquals(foodCategory.getName(), breakdown.get(0).getCategoryName());
        assertEquals(0, new BigDecimal("600.00").compareTo(breakdown.get(0).getTotalAmount()));
        assertEquals(2L, breakdown.get(0).getTransactionCount());

        // Second category: Utilities (150.00, count 1)
        assertEquals(utilCategory.getName(), breakdown.get(1).getCategoryName());
        assertEquals(0, new BigDecimal("150.00").compareTo(breakdown.get(1).getTotalAmount()));
        assertEquals(1L, breakdown.get(1).getTransactionCount());
    }

    @Test
    void findTopExpenseCategoriesByUserId_RespectsPageableLimit() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        createTransaction(user1, new BigDecimal("500.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Store A", foodCategory, LocalDateTime.of(2026, 8, 5, 10, 0));
        createTransaction(user1, new BigDecimal("300.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Store B", utilCategory, LocalDateTime.of(2026, 8, 10, 10, 0));

        List<CategoryExpenseProjection> top1 = transactionRepository.findTopExpenseCategoriesByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end, PageRequest.of(0, 1));

        assertNotNull(top1);
        assertEquals(1, top1.size());
        assertEquals(foodCategory.getName(), top1.get(0).getCategoryName());
    }

    @Test
    void findMonthlyTypeSumsByUserId_AggregatesMonthsAndTypes() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 3, 31, 23, 59, 59);

        // Jan 2026: Income 2000, Expense 500
        createTransaction(user1, new BigDecimal("2000.00"), TransactionType.INCOME, PaymentMethod.DIGITAL, "Salary", null, LocalDateTime.of(2026, 1, 15, 10, 0));
        createTransaction(user1, new BigDecimal("500.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Shop", foodCategory, LocalDateTime.of(2026, 1, 20, 12, 0));

        // Feb 2026: Expense 800
        createTransaction(user1, new BigDecimal("800.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Shop", foodCategory, LocalDateTime.of(2026, 2, 10, 12, 0));

        List<MonthlyTypeSumProjection> sums = transactionRepository.findMonthlyTypeSumsByUserId(user1.getId(), start, end);

        assertNotNull(sums);
        assertEquals(3, sums.size());
    }

    @Test
    void findPaymentMethodSumsByUserId_AggregatesCorrectly() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        createTransaction(user1, new BigDecimal("120.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Cash Store", null, LocalDateTime.of(2026, 8, 5, 10, 0));
        createTransaction(user1, new BigDecimal("80.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Cash Cafe", null, LocalDateTime.of(2026, 8, 6, 10, 0));
        createTransaction(user1, new BigDecimal("450.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Online Store", null, LocalDateTime.of(2026, 8, 10, 10, 0));

        List<PaymentMethodSumProjection> sums = transactionRepository.findPaymentMethodSumsByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end);

        assertNotNull(sums);
        assertEquals(2, sums.size());
    }

    @Test
    void findTopMerchantsAndCountDistinct_IgnoresBlankAndNullMerchants() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        createTransaction(user1, new BigDecimal("300.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Amazon", null, LocalDateTime.of(2026, 8, 5, 10, 0));
        createTransaction(user1, new BigDecimal("150.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Amazon", null, LocalDateTime.of(2026, 8, 6, 10, 0));
        createTransaction(user1, new BigDecimal("100.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Starbucks", null, LocalDateTime.of(2026, 8, 7, 10, 0));
        createTransaction(user1, new BigDecimal("50.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "   ", null, LocalDateTime.of(2026, 8, 8, 10, 0));
        createTransaction(user1, new BigDecimal("50.00"), TransactionType.EXPENSE, PaymentMethod.CASH, null, null, LocalDateTime.of(2026, 8, 9, 10, 0));

        List<MerchantSpendProjection> topMerchants = transactionRepository.findTopMerchantsByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end, PageRequest.of(0, 5));

        assertNotNull(topMerchants);
        assertEquals(2, topMerchants.size());
        assertEquals("Amazon", topMerchants.get(0).getMerchant());
        assertEquals(0, new BigDecimal("450.00").compareTo(topMerchants.get(0).getTotalAmount()));
        assertEquals(2L, topMerchants.get(0).getTransactionCount());

        Long distinctCount = transactionRepository.countDistinctMerchantsByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end);
        assertEquals(2L, distinctCount);
    }

    @Test
    void findTopSpendingDaysByUserId_ExtractsDailyAggregates() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        // Aug 15: 2 transactions totaling 500
        createTransaction(user1, new BigDecimal("300.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Shop 1", null, LocalDateTime.of(2026, 8, 15, 10, 0));
        createTransaction(user1, new BigDecimal("200.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Shop 2", null, LocalDateTime.of(2026, 8, 15, 16, 0));

        // Aug 20: 1 transaction of 250
        createTransaction(user1, new BigDecimal("250.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Shop 3", null, LocalDateTime.of(2026, 8, 20, 11, 0));

        List<DailySpendProjection> topDays = transactionRepository.findTopSpendingDaysByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end, PageRequest.of(0, 1));

        assertNotNull(topDays);
        assertEquals(1, topDays.size());
        assertEquals(2026, topDays.get(0).getYear());
        assertEquals(8, topDays.get(0).getMonth());
        assertEquals(15, topDays.get(0).getDay());
        assertEquals(0, new BigDecimal("500.00").compareTo(topDays.get(0).getTotalAmount()));
        assertEquals(2L, topDays.get(0).getTransactionCount());
    }

    @Test
    void findLargestExpenseTransactionsByUserId_FetchesLargestTransactionWithCategory() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);

        createTransaction(user1, new BigDecimal("50.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Small Store", foodCategory, LocalDateTime.of(2026, 8, 5, 10, 0));
        createTransaction(user1, new BigDecimal("1200.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Electronics Hub", utilCategory, LocalDateTime.of(2026, 8, 12, 14, 0));

        List<Transaction> largest = transactionRepository.findLargestExpenseTransactionsByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end, PageRequest.of(0, 1));

        assertNotNull(largest);
        assertEquals(1, largest.size());
        assertEquals(0, new BigDecimal("1200.00").compareTo(largest.get(0).getAmount()));
        assertEquals("Electronics Hub", largest.get(0).getMerchant());
        assertEquals(utilCategory.getName(), largest.get(0).getCategory().getName());
    }

    @Test
    void sumAndCountSmallPurchases_FiltersByThresholdCorrectly() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 31, 23, 59, 59);
        BigDecimal threshold = new BigDecimal("20.00");

        createTransaction(user1, new BigDecimal("5.00"), TransactionType.EXPENSE, PaymentMethod.CASH, "Coffee", null, LocalDateTime.of(2026, 8, 2, 8, 0));
        createTransaction(user1, new BigDecimal("15.50"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Snack", null, LocalDateTime.of(2026, 8, 3, 15, 0));
        createTransaction(user1, new BigDecimal("20.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Lunch", null, LocalDateTime.of(2026, 8, 4, 13, 0));
        createTransaction(user1, new BigDecimal("50.00"), TransactionType.EXPENSE, PaymentMethod.DIGITAL, "Dinner", null, LocalDateTime.of(2026, 8, 5, 20, 0));

        BigDecimal smallSum = transactionRepository.sumSmallPurchasesByUserId(user1.getId(), TransactionType.EXPENSE, start, end, threshold);
        Long smallCount = transactionRepository.countSmallPurchasesByUserId(user1.getId(), TransactionType.EXPENSE, start, end, threshold);

        assertNotNull(smallSum);
        assertEquals(0, new BigDecimal("40.50").compareTo(smallSum));
        assertEquals(3L, smallCount);
    }

    @Test
    void emptyResultBehavior_ReturnsNullOrEmptySafely() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59, 59);

        BigDecimal sum = transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(
                user1.getId(), TransactionType.EXPENSE, start, end);
        assertNull(sum);

        Long count = transactionRepository.countByUserIdAndTransactionDateBetween(user1.getId(), start, end);
        assertEquals(0L, count);

        List<CategoryExpenseProjection> categories = transactionRepository.findCategoryExpenseBreakdownByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end);
        assertTrue(categories.isEmpty());

        List<MerchantSpendProjection> merchants = transactionRepository.findTopMerchantsByUserId(
                user1.getId(), TransactionType.EXPENSE, start, end, PageRequest.of(0, 5));
        assertTrue(merchants.isEmpty());
    }
}
