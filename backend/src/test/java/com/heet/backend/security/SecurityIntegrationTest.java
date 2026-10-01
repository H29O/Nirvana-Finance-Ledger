package com.heet.backend.security;

import com.heet.backend.entity.Category;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.enums.EntrySource;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.exception.TransactionNotFoundException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.service.CategoryService;
import com.heet.backend.service.DashboardService;
import com.heet.backend.service.TransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.heet.backend.repository.LoanRecordRepository;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityIntegrationTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LoanRecordRepository loanRecordRepository;

    @InjectMocks
    private TransactionService transactionService;

    @InjectMocks
    private CategoryService categoryService;

    @InjectMocks
    private DashboardService dashboardService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = new User();
        userA.setId(1L);
        userA.setName("User A");
        userA.setEmail("userA@example.com");

        userB = new User();
        userB.setId(2L);
        userB.setName("User B");
        userB.setEmail("userB@example.com");
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userACannotAccessUserBTransaction() {
        authenticateAs(userA);

        Transaction transactionB = new Transaction();
        transactionB.setId(100L);
        transactionB.setUser(userB);
        transactionB.setAmount(BigDecimal.valueOf(500));

        when(transactionRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.deleteTransaction(100L));
    }

    @Test
    void userACannotAccessUserBCategory() {
        authenticateAs(userA);

        Category categoryB = new Category();
        categoryB.setId(200L);
        categoryB.setUser(userB);
        categoryB.setName("User B Private Category");

        when(categoryRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.getCategoryById(200L));
    }
}
