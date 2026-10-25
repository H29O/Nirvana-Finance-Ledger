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

    @Mock
    private com.heet.backend.repository.ReceiptRepository receiptRepository;

    @Mock
    private com.heet.backend.repository.ReceiptOcrDataRepository receiptOcrDataRepository;

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

    @Test
    void updateTransaction_NotFound_ThrowsTransactionNotFoundException() {
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(com.heet.backend.exception.TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(99L, requestWithCategory));
    }

    @Test
    void deleteTransaction_Success() {
        Transaction existing = new Transaction();
        existing.setId(1L);
        existing.setUser(testUser);

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        transactionService.deleteTransaction(1L);

        verify(transactionRepository, times(1)).delete(existing);
    }

    @Test
    void deleteTransaction_NotFound_ThrowsTransactionNotFoundException() {
        when(transactionRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThrows(com.heet.backend.exception.TransactionNotFoundException.class,
                () -> transactionService.deleteTransaction(99L));
    }

    @Test
    void getTransactions_FilterByType_DispatchesToRepository() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Transaction sample = new Transaction();
        sample.setId(1L);
        sample.setTransactionType(TransactionType.EXPENSE);
        sample.setUser(testUser);

        org.springframework.data.domain.Page<Transaction> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(sample));
        when(transactionRepository.findByUserIdAndTransactionType(1L, TransactionType.EXPENSE, pageable))
                .thenReturn(page);

        org.springframework.data.domain.Page<TransactionResponse> result =
                transactionService.getTransactions(TransactionType.EXPENSE, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByUserIdAndTransactionType(1L, TransactionType.EXPENSE, pageable);
    }

    @Test
    void getTransactions_FilterByPaymentMethod_DispatchesToRepository() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Transaction sample = new Transaction();
        sample.setId(1L);
        sample.setPaymentMethod(PaymentMethod.DIGITAL);
        sample.setUser(testUser);

        org.springframework.data.domain.Page<Transaction> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(sample));
        when(transactionRepository.findByUserIdAndPaymentMethod(1L, PaymentMethod.DIGITAL, pageable))
                .thenReturn(page);

        org.springframework.data.domain.Page<TransactionResponse> result =
                transactionService.getTransactions(null, PaymentMethod.DIGITAL, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByUserIdAndPaymentMethod(1L, PaymentMethod.DIGITAL, pageable);
    }

    @Test
    void getTransactions_FilterByDateRange_DispatchesToRepository() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 8, 1, 0, 0);
        java.time.LocalDateTime end = java.time.LocalDateTime.of(2026, 8, 31, 23, 59);

        Transaction sample = new Transaction();
        sample.setId(1L);
        sample.setUser(testUser);

        org.springframework.data.domain.Page<Transaction> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(sample));
        when(transactionRepository.findByUserIdAndTransactionDateBetween(1L, start, end, pageable))
                .thenReturn(page);

        org.springframework.data.domain.Page<TransactionResponse> result =
                transactionService.getTransactions(null, null, start, end, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByUserIdAndTransactionDateBetween(1L, start, end, pageable);
    }

    @Test
    void getTransactions_NoFilters_DispatchesToFindByUserId() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Transaction sample = new Transaction();
        sample.setId(1L);
        sample.setUser(testUser);

        org.springframework.data.domain.Page<Transaction> page = new org.springframework.data.domain.PageImpl<>(java.util.List.of(sample));
        when(transactionRepository.findByUserId(1L, pageable)).thenReturn(page);

        org.springframework.data.domain.Page<TransactionResponse> result =
                transactionService.getTransactions(null, null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(transactionRepository).findByUserId(1L, pageable);
    }

    @Test
    void deleteTransaction_WithLinkedReceipt_UnlinksReceiptAndDeletes() {
        Transaction sample = new Transaction();
        sample.setId(10L);
        sample.setUser(testUser);

        com.heet.backend.entity.Receipt receipt = new com.heet.backend.entity.Receipt();
        receipt.setId(100L);
        receipt.setTransaction(sample);

        when(transactionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sample));
        when(receiptRepository.findByTransactionIdAndUserId(10L, 1L)).thenReturn(Optional.of(receipt));

        transactionService.deleteTransaction(10L);

        assertNull(receipt.getTransaction());
        verify(receiptRepository).save(receipt);
        verify(transactionRepository).delete(sample);
    }

    @Test
    void deleteTransaction_WithoutLinkedReceipt_DeletesCleanly() {
        Transaction sample = new Transaction();
        sample.setId(10L);
        sample.setUser(testUser);

        when(transactionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(sample));
        when(receiptRepository.findByTransactionIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        transactionService.deleteTransaction(10L);

        verify(receiptRepository, never()).save(any());
        verify(transactionRepository).delete(sample);
    }

    @Test
    void updateTransaction_UpdateTransactionDate_Success() {
        Transaction existing = new Transaction();
        existing.setId(1L);
        existing.setUser(testUser);
        existing.setTransactionDate(java.time.LocalDateTime.of(2026, 1, 1, 10, 0));

        java.time.LocalDateTime newDate = java.time.LocalDateTime.of(2026, 10, 25, 14, 30);
        CreateTransactionRequest updateReq = new CreateTransactionRequest(
                BigDecimal.valueOf(120.00),
                TransactionType.EXPENSE,
                PaymentMethod.DIGITAL,
                EntrySource.MANUAL,
                "New Store",
                "Updated note",
                null,
                newDate
        );

        when(transactionRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransactionResponse response = transactionService.updateTransaction(1L, updateReq);

        assertNotNull(response);
        assertEquals(newDate, response.getTransactionDate());
        assertEquals("New Store", response.getMerchant());
        assertEquals(0, BigDecimal.valueOf(120.00).compareTo(response.getAmount()));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_UnauthorizedUser_ThrowsException() {
        when(transactionRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        CreateTransactionRequest updateReq = new CreateTransactionRequest(
                BigDecimal.valueOf(100.00),
                TransactionType.EXPENSE,
                PaymentMethod.CASH,
                EntrySource.MANUAL,
                "Merchant",
                null,
                null,
                java.time.LocalDateTime.now()
        );

        assertThrows(com.heet.backend.exception.TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(999L, updateReq));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
