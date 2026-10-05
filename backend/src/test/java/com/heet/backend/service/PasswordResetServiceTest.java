package com.heet.backend.service;

import com.heet.backend.dto.request.ChangePasswordRequest;
import com.heet.backend.dto.request.ForgotPasswordRequest;
import com.heet.backend.dto.request.ResetPasswordRequest;
import com.heet.backend.entity.PasswordResetToken;
import com.heet.backend.entity.PasswordResetToken.ResetTokenStatus;
import com.heet.backend.entity.User;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.exception.InvalidCurrentPasswordException;
import com.heet.backend.exception.PasswordResetException;
import com.heet.backend.repository.PasswordResetTokenRepository;
import com.heet.backend.repository.UserRepository;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setName("Bob");
        sampleUser.setEmail("bob@example.com");
        sampleUser.setPasswordHash("$2a$10$oldHashedPassword");
        sampleUser.setEmailVerified(true);
    }

    // =========================================================================
    // FORGOT PASSWORD TESTS
    // =========================================================================

    @Test
    void forgotPassword_ValidEmail_CreatesHashedTokenAndSendsEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(" bob@example.com ")
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(sampleUser));
        when(resetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.empty());

        passwordResetService.forgotPassword(request);

        // Verify previous tokens were invalidated
        verify(resetTokenRepository).invalidateAllPendingForUser(10L);

        // Verify token saved is hashed (never raw)
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertNotNull(savedToken);
        assertEquals(sampleUser, savedToken.getUser());
        assertNotNull(savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length()); // SHA-256 hex is 64 characters
        assertEquals(ResetTokenStatus.PENDING, savedToken.getStatus());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));

        // Verify email dispatched contains raw token
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("bob@example.com"), eq("Bob"), rawTokenCaptor.capture());
        String dispatchedRawToken = rawTokenCaptor.getValue();
        assertNotNull(dispatchedRawToken);
        assertEquals(64, dispatchedRawToken.length());

        // Verify token in DB is the SHA-256 hash of the raw token dispatched
        assertEquals(PasswordResetService.hashToken(dispatchedRawToken), savedToken.getTokenHash());
    }

    @Test
    void forgotPassword_UnknownEmail_DoesNotThrowAndDoesNotSendEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("unknown@example.com")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> passwordResetService.forgotPassword(request));

        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void forgotPassword_CooldownActive_DoesNotSendDuplicateEmail() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("bob@example.com")
                .build();

        PasswordResetToken recentToken = PasswordResetToken.builder()
                .user(sampleUser)
                .tokenHash("hash123")
                .createdAt(LocalDateTime.now().minusSeconds(10))
                .status(ResetTokenStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(sampleUser));
        when(resetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(10L)).thenReturn(Optional.of(recentToken));

        passwordResetService.forgotPassword(request);

        // In cooldown, no new email or token
        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    // =========================================================================
    // VALIDATE RESET TOKEN TESTS
    // =========================================================================

    @Test
    void validateResetToken_ValidActiveToken_ReturnsTrue() {
        String rawToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .status(ResetTokenStatus.PENDING)
                .build();

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        assertTrue(passwordResetService.validateResetToken(rawToken));
    }

    @Test
    void validateResetToken_ExpiredToken_ThrowsPasswordResetExceptionAndUpdatesStatus() {
        String rawToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired
                .status(ResetTokenStatus.PENDING)
                .build();

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        PasswordResetException ex = assertThrows(PasswordResetException.class,
                () -> passwordResetService.validateResetToken(rawToken));
        assertEquals("This password reset link is invalid or has expired.", ex.getMessage());
        assertEquals(ResetTokenStatus.EXPIRED, token.getStatus());
        verify(resetTokenRepository).save(token);
    }

    @Test
    void validateResetToken_AlreadyUsedToken_ThrowsPasswordResetException() {
        String rawToken = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .usedAt(LocalDateTime.now().minusMinutes(2))
                .status(ResetTokenStatus.USED)
                .build();

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        assertThrows(PasswordResetException.class, () -> passwordResetService.validateResetToken(rawToken));
    }

    @Test
    void validateResetToken_InvalidToken_ThrowsPasswordResetException() {
        when(resetTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(PasswordResetException.class, () -> passwordResetService.validateResetToken("completelyUnknownToken"));
    }

    // =========================================================================
    // RESET PASSWORD TESTS
    // =========================================================================

    @Test
    void resetPassword_ValidToken_SuccessfullyChangesPasswordAndMarksTokenUsed() {
        String rawToken = "testRawToken1234567890abcdef1234567890abcdef";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(sampleUser)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .status(ResetTokenStatus.PENDING)
                .build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("brandNewPassword123")
                .confirmPassword("brandNewPassword123")
                .build();

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("brandNewPassword123", "$2a$10$oldHashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("brandNewPassword123")).thenReturn("$2a$10$brandNewHashedPassword");

        passwordResetService.resetPassword(request);

        // Verify token marked used
        assertEquals(ResetTokenStatus.USED, token.getStatus());
        assertNotNull(token.getUsedAt());
        verify(resetTokenRepository).save(token);

        // Verify other pending tokens invalidated
        verify(resetTokenRepository).invalidateAllPendingForUser(10L);

        // Verify user password updated and passwordChangedAt set
        assertEquals("$2a$10$brandNewHashedPassword", sampleUser.getPasswordHash());
        assertNotNull(sampleUser.getPasswordChangedAt());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void resetPassword_PasswordMismatch_ThrowsIllegalArgumentException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("anyToken")
                .newPassword("newPass1")
                .confirmPassword("differentPass")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals("New password and confirmation do not match.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_PasswordTooShort_ThrowsIllegalArgumentException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("anyToken")
                .newPassword("12345")
                .confirmPassword("12345")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals("Password must be at least 6 characters long.", ex.getMessage());
    }

    @Test
    void resetPassword_SameAsOldPassword_ThrowsIllegalArgumentException() {
        String rawToken = "testRawToken";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(sampleUser)
                .tokenHash(tokenHash)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .status(ResetTokenStatus.PENDING)
                .build();

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("oldPlainPassword")
                .confirmPassword("oldPlainPassword")
                .build();

        when(resetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("oldPlainPassword", sampleUser.getPasswordHash())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword(request));
        assertEquals("New password must be different from your current password.", ex.getMessage());
    }

    // =========================================================================
    // CHANGE PASSWORD TESTS
    // =========================================================================

    @Test
    void changePassword_AuthenticatedUser_CorrectCurrentPassword_Succeeds() {
        UserPrincipal principal = new UserPrincipal(sampleUser);
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("correctCurrentPass")
                .newPassword("freshNewPassword99")
                .confirmPassword("freshNewPassword99")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("correctCurrentPass", sampleUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("freshNewPassword99", sampleUser.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("freshNewPassword99")).thenReturn("$2a$10$encodedFreshPassword");

        passwordResetService.changePassword(principal, request);

        assertEquals("$2a$10$encodedFreshPassword", sampleUser.getPasswordHash());
        assertNotNull(sampleUser.getPasswordChangedAt());
        verify(userRepository).save(sampleUser);
        verify(resetTokenRepository).invalidateAllPendingForUser(10L);
    }

    @Test
    void changePassword_IncorrectCurrentPassword_ThrowsInvalidCurrentPasswordException() {
        UserPrincipal principal = new UserPrincipal(sampleUser);
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("freshNewPassword99")
                .confirmPassword("freshNewPassword99")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        InvalidCurrentPasswordException ex = assertThrows(InvalidCurrentPasswordException.class,
                () -> passwordResetService.changePassword(principal, request));
        assertEquals("Current password is incorrect.", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_SamePassword_ThrowsIllegalArgumentException() {
        UserPrincipal principal = new UserPrincipal(sampleUser);
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("samePass")
                .newPassword("samePass")
                .confirmPassword("samePass")
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("samePass", sampleUser.getPasswordHash())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.changePassword(principal, request));
        assertEquals("New password must be different from your current password.", ex.getMessage());
    }

    @Test
    void changePassword_Unauthenticated_ThrowsInvalidCredentialsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("pass")
                .newPassword("newPass123")
                .confirmPassword("newPass123")
                .build();

        assertThrows(InvalidCredentialsException.class, () -> passwordResetService.changePassword(null, request));
    }
}
