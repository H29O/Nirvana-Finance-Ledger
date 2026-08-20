package com.heet.backend.service;

import com.heet.backend.dto.projection.CategoryExpenseProjection;
import com.heet.backend.dto.response.ExpenseByCategoryResponse;
import com.heet.backend.dto.response.IncomeExpenseResponse;
import com.heet.backend.dto.response.MonthlySummaryResponse;
import com.heet.backend.dto.response.TopCategoryResponse;
import com.heet.backend.entity.User;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");

        UserPrincipal principal = new UserPrincipal(testUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMonthlySummary_CalculatesCorrectly() {
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(10000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(4000));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(eq(1L), any(), any()))
                .thenReturn(25L);

        MonthlySummaryResponse response = dashboardService.getMonthlySummary(2026, 8);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(10000), response.getTotalIncome());
        assertEquals(BigDecimal.valueOf(4000), response.getTotalExpense());
        assertEquals(BigDecimal.valueOf(6000), response.getNetSavings());
        assertEquals(25L, response.getTotalTransactions());
    }

    @Test
    void getIncomeVsExpense_CalculatesBalance() {
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.INCOME), any(), any()))
                .thenReturn(BigDecimal.valueOf(6000));
        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(4000));

        IncomeExpenseResponse response = dashboardService.getIncomeVsExpense(2026, 8);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(6000), response.getTotalIncome());
        assertEquals(BigDecimal.valueOf(4000), response.getTotalExpense());
        assertEquals(BigDecimal.valueOf(2000), response.getBalance());
    }

    @Test
    void getExpenseByCategory_ReturnsBreakdown() {
        CategoryExpenseProjection projection = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return 1L; }
            @Override public String getCategoryName() { return "Groceries"; }
            @Override public String getColor() { return "#FF0000"; }
            @Override public String getIcon() { return "cart"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(2000); }
            @Override public Long getTransactionCount() { return 10L; }
        };

        when(transactionRepository.sumAmountByUserIdAndTransactionTypeAndDateBetween(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(BigDecimal.valueOf(4000));
        when(transactionRepository.findCategoryExpenseBreakdownByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any()))
                .thenReturn(List.of(projection));

        ExpenseByCategoryResponse response = dashboardService.getExpenseByCategory(2026, 8);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(4000), response.getTotalExpense());
        assertEquals(1, response.getCategories().size());
        assertEquals("Groceries", response.getCategories().get(0).getCategoryName());
    }

    @Test
    void getTopCategories_ReturnsTopItems() {
        CategoryExpenseProjection projection = new CategoryExpenseProjection() {
            @Override public Long getCategoryId() { return 2L; }
            @Override public String getCategoryName() { return "Rent"; }
            @Override public String getColor() { return "#00FF00"; }
            @Override public String getIcon() { return "home"; }
            @Override public BigDecimal getTotalAmount() { return BigDecimal.valueOf(3000); }
            @Override public Long getTransactionCount() { return 1L; }
        };

        when(transactionRepository.findTopExpenseCategoriesByUserId(eq(1L), eq(TransactionType.EXPENSE), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(projection));

        List<TopCategoryResponse> response = dashboardService.getTopCategories(2026, 8, 3);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("Rent", response.get(0).getCategoryName());
    }
}
