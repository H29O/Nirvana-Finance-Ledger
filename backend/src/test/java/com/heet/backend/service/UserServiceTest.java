package com.heet.backend.service;

import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.entity.User;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.repository.*;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReceiptOcrDataRepository receiptOcrDataRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LoanRecordRepository loanRecordRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private ReceiptStorageService receiptStorageService;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setName("Alice Test");
        user.setEmail("alice@example.com");
        user.setEmailVerified(true);

        principal = new UserPrincipal(user);
    }

    @Test
    void getCurrentUserProfile_ValidUser_ReturnsProfile() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUserProfile(principal);

        assertNotNull(response);
        assertEquals(42L, response.getId());
        assertEquals("Alice Test", response.getName());
        assertEquals("alice@example.com", response.getEmail());
    }

    @Test
    void getCurrentUserProfile_NullPrincipal_ThrowsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> userService.getCurrentUserProfile(null));
    }

    @Test
    void deleteAccount_ValidUser_DeletesInCorrectOrder() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        userService.deleteAccount(principal);

        // Verify exact deletion order for foreign key safety:
        // 1. OCR data
        verify(receiptOcrDataRepository, times(1)).deleteByUserId(42L);
        // 2. Receipts & files
        verify(receiptRepository, times(1)).deleteByUserId(42L);
        verify(receiptStorageService, times(1)).deleteUserStorage(42L);
        // 3. Transactions
        verify(transactionRepository, times(1)).deleteByUserId(42L);
        // 4. Categories
        verify(categoryRepository, times(1)).deleteByUserId(42L);
        // 5. Loan records
        verify(loanRecordRepository, times(1)).deleteByUserId(42L);
        // 6. Email verifications
        verify(emailVerificationRepository, times(1)).deleteByUserId(42L);
        // 7. User entity
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteAccount_NullPrincipal_ThrowsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class, () -> userService.deleteAccount(null));
        verifyNoInteractions(receiptOcrDataRepository, receiptRepository, transactionRepository,
                categoryRepository, loanRecordRepository, emailVerificationRepository, userRepository);
    }

    @Test
    void deleteAccount_UserNotFoundInDb_ThrowsInvalidCredentials() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> userService.deleteAccount(principal));
        verify(userRepository, never()).delete(any());
    }
}
