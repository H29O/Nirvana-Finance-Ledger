package com.heet.backend.service;

import com.heet.backend.dto.request.CreateTransactionRequest;
import com.heet.backend.dto.response.TransactionResponse;
import com.heet.backend.entity.Category;
import com.heet.backend.entity.Transaction;
import com.heet.backend.entity.User;
import com.heet.backend.enums.EntrySource;
import com.heet.backend.enums.PaymentMethod;
import com.heet.backend.enums.TransactionType;
import com.heet.backend.exception.CategoryNotFoundException;
import com.heet.backend.repository.CategoryRepository;
import com.heet.backend.repository.TransactionRepository;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category category;
    private CreateTransactionRequest requestWithCategory;
    private CreateTransactionRequest requestWithoutCategory;

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

        category = new Category();
        category.setId(10L);
        category.setName("Utilities");
        category.setUser(testUser);

        requestWithCategory = new CreateTransactionRequest(
                BigDecimal.valueOf(100.00),
                TransactionType.EXPENSE,
                PaymentMethod.DIGITAL,
                EntrySource.MANUAL,
                "Electric Company",
                "Monthly bill",
                10L
        );

        requestWithoutCategory = new CreateTransactionRequest(
                BigDecimal.valueOf(50.00),
                TransactionType.EXPENSE,
                PaymentMethod.CASH,
                EntrySource.MANUAL,
                "Local Market",
                "Groceries",
                null
        );
    }

    @Test
    void createTransaction_WithValidCategory_Success() {
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(requestWithCategory);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(10L, response.getCategoryId());
        assertEquals("Utilities", response.getCategoryName());
        verify(categoryRepository, times(1)).findByIdAndUserId(10L, 1L);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WithoutCategory_Success() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        TransactionResponse response = transactionService.createTransaction(requestWithoutCategory);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertNull(response.getCategoryId());
        assertNull(response.getCategoryName());
        verify(categoryRepository, never()).findByIdAndUserId(any(), any());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_WithInvalidCategory_ThrowsCategoryNotFoundException() {
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        CreateTransactionRequest invalidRequest = new CreateTransactionRequest(
                BigDecimal.valueOf(100.00),
                TransactionType.EXPENSE,
                PaymentMethod.DIGITAL,
                EntrySource.MANUAL,
                "Electric Company",
                "Monthly bill",
                99L
        );

        assertThrows(CategoryNotFoundException.class, () -> transactionService.createTransaction(invalidRequest));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_UpdateCategory_Success() {
        Transaction existing = new Transaction();
        existing.setId(1L);
        existing.setUser(testUser);
        existing.setCategory(null);

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.updateTransaction(1L, requestWithCategory);

        assertNotNull(response);
        assertEquals(10L, response.getCategoryId());
        assertEquals("Utilities", response.getCategoryName());
    }
}
