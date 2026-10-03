package com.heet.backend.service;

import com.heet.backend.entity.EmailVerification;
import com.heet.backend.entity.EmailVerification.VerificationStatus;
import com.heet.backend.entity.User;
import com.heet.backend.exception.EmailVerificationException;
import com.heet.backend.repository.EmailVerificationRepository;
import com.heet.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(10L);
        testUser.setName("Bob");
        testUser.setEmail("bob@example.com");
        testUser.setEmailVerified(false);
    }

    @Test
    void createAndSendVerification_GeneratesSecureOtpAndSendsEmail() {
        when(verificationRepository.findByUserIdAndStatus(10L, VerificationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$hashedOtp");

        emailVerificationService.createAndSendVerification(testUser);

        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository).save(captor.capture());
        EmailVerification saved = captor.getValue();

        assertNotNull(saved);
        assertEquals(testUser, saved.getUser());
        assertEquals("$2a$10$hashedOtp", saved.getOtpHash());
        assertEquals(VerificationStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttemptCount());
        assertTrue(saved.getExpiresAt().isAfter(saved.getCreatedAt().plusMinutes(4)));
        assertTrue(saved.getExpiresAt().isBefore(saved.getCreatedAt().plusMinutes(6)));

        // Verify email sending was invoked with a 6-digit numeric string
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq("bob@example.com"), eq("Bob"), otpCaptor.capture());
        String generatedOtp = otpCaptor.getValue();
        assertNotNull(generatedOtp);
        assertEquals(6, generatedOtp.length());
        assertTrue(generatedOtp.matches("^[0-9]{6}$"));
    }

    @Test
    void createAndSendVerification_InvalidatesPreviousActiveVerifications() {
        EmailVerification oldVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .status(VerificationStatus.PENDING)
                .build();

        when(verificationRepository.findByUserIdAndStatus(10L, VerificationStatus.PENDING))
                .thenReturn(List.of(oldVerification));
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$hashedOtp");

        emailVerificationService.createAndSendVerification(testUser);

        assertEquals(VerificationStatus.INVALIDATED, oldVerification.getStatus());
        verify(verificationRepository).saveAll(any());
        verify(verificationRepository).save(any(EmailVerification.class));
    }

    @Test
    void verifyEmail_CorrectOtp_MarksVerifiedAndUserVerified() {
        EmailVerification activeVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .otpHash("$2a$10$hashedOtp")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .attemptCount(0)
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(10L, VerificationStatus.PENDING))
                .thenReturn(Optional.of(activeVerification));
        when(passwordEncoder.matches("123456", "$2a$10$hashedOtp")).thenReturn(true);
        when(userRepository.save(testUser)).thenReturn(testUser);

        User result = emailVerificationService.verifyEmail("bob@example.com", "123456");

        assertTrue(result.isEmailVerified());
        assertEquals(VerificationStatus.VERIFIED, activeVerification.getStatus());
        assertNotNull(activeVerification.getUsedAt());
        verify(verificationRepository).save(activeVerification);
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyEmail_IncorrectOtp_IncrementsAttemptsAndThrows() {
        EmailVerification activeVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .otpHash("$2a$10$hashedOtp")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .attemptCount(0)
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(10L, VerificationStatus.PENDING))
                .thenReturn(Optional.of(activeVerification));
        when(passwordEncoder.matches("999999", "$2a$10$hashedOtp")).thenReturn(false);

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> emailVerificationService.verifyEmail("bob@example.com", "999999"));

        assertTrue(ex.getMessage().contains("Invalid verification code"));
        assertEquals(1, activeVerification.getAttemptCount());
        assertEquals(VerificationStatus.PENDING, activeVerification.getStatus());
        assertFalse(testUser.isEmailVerified());
    }

    @Test
    void verifyEmail_FiveIncorrectAttempts_InvalidatesCode() {
        EmailVerification activeVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .otpHash("$2a$10$hashedOtp")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .attemptCount(4) // Already failed 4 times
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(10L, VerificationStatus.PENDING))
                .thenReturn(Optional.of(activeVerification));
        when(passwordEncoder.matches("999999", "$2a$10$hashedOtp")).thenReturn(false);

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> emailVerificationService.verifyEmail("bob@example.com", "999999"));

        assertTrue(ex.getMessage().contains("Too many incorrect attempts"));
        assertEquals(5, activeVerification.getAttemptCount());
        assertEquals(VerificationStatus.INVALIDATED, activeVerification.getStatus());
    }

    @Test
    void verifyEmail_ExpiredOtp_MarksExpiredAndThrows() {
        EmailVerification expiredVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .otpHash("$2a$10$hashedOtp")
                .createdAt(LocalDateTime.now().minusMinutes(6))
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // Expired 1 min ago
                .attemptCount(0)
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(10L, VerificationStatus.PENDING))
                .thenReturn(Optional.of(expiredVerification));

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> emailVerificationService.verifyEmail("bob@example.com", "123456"));

        assertTrue(ex.getMessage().contains("expired"));
        assertEquals(VerificationStatus.EXPIRED, expiredVerification.getStatus());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void verifyEmail_AlreadyVerifiedUser_ReturnsImmediately() {
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));

        User result = emailVerificationService.verifyEmail("bob@example.com", "123456");

        assertTrue(result.isEmailVerified());
        verify(verificationRepository, never()).findTopByUserIdAndStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void resendVerification_Enforces60SecondsCooldown() {
        EmailVerification recentVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .createdAt(LocalDateTime.now().minusSeconds(20)) // Created 20s ago, 40s remaining
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(recentVerification));

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> emailVerificationService.resendVerification("bob@example.com"));

        assertTrue(ex.getMessage().contains("Please wait"));
        assertTrue(ex.getRemainingCooldownSeconds() > 0);
        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
    }

    @Test
    void resendVerification_AfterCooldown_GeneratesNewOtpAndSendsEmail() {
        EmailVerification olderVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .createdAt(LocalDateTime.now().minusSeconds(65)) // Cooldown passed
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(olderVerification));
        when(verificationRepository.findByUserIdAndStatus(10L, VerificationStatus.PENDING))
                .thenReturn(List.of(olderVerification));
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$newHash");

        emailVerificationService.resendVerification("bob@example.com");

        verify(emailService).sendVerificationEmail(eq("bob@example.com"), eq("Bob"), any(String.class));
        assertEquals(VerificationStatus.INVALIDATED, olderVerification.getStatus());
    }

    @Test
    void resendVerification_AlreadyVerifiedUser_ThrowsException() {
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));

        EmailVerificationException ex = assertThrows(EmailVerificationException.class,
                () -> emailVerificationService.resendVerification("bob@example.com"));

        assertTrue(ex.getMessage().contains("already verified"));
    }

    @Test
    void createAndSendVerification_EmailDeliveryFails_ReturnsFalseAndPreservesRecord() {
        when(verificationRepository.findByUserIdAndStatus(10L, VerificationStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$hashedOtp");
        doThrow(new com.heet.backend.exception.EmailDeliveryException("SMTP error"))
                .when(emailService).sendVerificationEmail(any(), any(), any());

        boolean result = emailVerificationService.createAndSendVerification(testUser);

        assertFalse(result);
        verify(verificationRepository).save(any(EmailVerification.class));
    }

    @Test
    void resendVerification_EmailDeliveryFails_ThrowsEmailDeliveryException() {
        EmailVerification olderVerification = EmailVerification.builder()
                .id(1L)
                .user(testUser)
                .createdAt(LocalDateTime.now().minusSeconds(65))
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .status(VerificationStatus.PENDING)
                .build();

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(testUser));
        when(verificationRepository.findTopByUserIdOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(olderVerification));
        when(verificationRepository.findByUserIdAndStatus(10L, VerificationStatus.PENDING))
                .thenReturn(List.of(olderVerification));
        when(passwordEncoder.encode(any(String.class))).thenReturn("$2a$10$newHash");
        doThrow(new com.heet.backend.exception.EmailDeliveryException("SMTP delivery failed"))
                .when(emailService).sendVerificationEmail(any(), any(), any());

        assertThrows(com.heet.backend.exception.EmailDeliveryException.class,
                () -> emailVerificationService.resendVerification("bob@example.com"));
    }
}
