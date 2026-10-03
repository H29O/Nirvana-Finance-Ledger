package com.heet.backend.service;

import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.RegisterRequest;
import com.heet.backend.dto.request.ResendVerificationRequest;
import com.heet.backend.dto.request.VerifyEmailRequest;
import com.heet.backend.dto.response.AuthResponse;
import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.entity.User;
import com.heet.backend.exception.DuplicateEmailException;
import com.heet.backend.exception.EmailNotVerifiedException;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.repository.UserRepository;
import com.heet.backend.security.JwtTokenProvider;
import com.heet.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("Alice");
        sampleUser.setEmail("alice@example.com");
        sampleUser.setPasswordHash("$2a$10$encodedPasswordHash");
        sampleUser.setEmailVerified(true);
    }

    @Test
    void register_EncodesPasswordCreatesUnverifiedUserAndTriggersVerification() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("  alice@example.com  ")
                .password("plainTextPass123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainTextPass123")).thenReturn("$2a$10$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(emailVerificationService.createAndSendVerification(any(User.class))).thenReturn(true);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNull(response.getToken()); // Must not issue JWT for unverified registration
        assertTrue(response.isVerificationRequired());
        assertEquals(true, response.getEmailDeliverySuccess());
        assertEquals("Verification email sent.", response.getMessage());
        assertEquals("alice@example.com", response.getUser().getEmail());
        assertFalse(response.getUser().isEmailVerified());

        // Verify password was strictly hashed and never saved as plaintext
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("$2a$10$encodedPasswordHash", savedUser.getPasswordHash());
        assertNotEquals("plainTextPass123", savedUser.getPasswordHash());
        assertFalse(savedUser.isEmailVerified());
        verify(passwordEncoder).encode("plainTextPass123");
        verify(emailVerificationService).createAndSendVerification(savedUser);
    }

    @Test
    void register_EmailDeliveryFails_ReturnsFailureMessageAndPreservesRecord() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("plainTextPass123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainTextPass123")).thenReturn("$2a$10$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(emailVerificationService.createAndSendVerification(any(User.class))).thenReturn(false);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(false, response.getEmailDeliverySuccess());
        assertEquals("Verification email could not be sent. Please try again.", response.getMessage());
        assertTrue(response.isVerificationRequired());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsDuplicateEmailException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("plainTextPass123")
                .build();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
        verify(emailVerificationService, never()).createAndSendVerification(any());
    }

    @Test
    void login_VerifiedUser_ReturnsTokenAndUserProfile() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("plainTextPass123")
                .build();

        sampleUser.setEmailVerified(true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plainTextPass123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken("alice@example.com")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
        assertTrue(response.getUser().isEmailVerified());
        assertFalse(response.isVerificationRequired());
    }

    @Test
    void login_UnverifiedUser_ThrowsEmailNotVerifiedExceptionAndNoJwt() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("plainTextPass123")
                .build();

        sampleUser.setEmailVerified(false);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plainTextPass123", sampleUser.getPasswordHash())).thenReturn(true);

        EmailNotVerifiedException ex = assertThrows(EmailNotVerifiedException.class, () -> authService.login(request));
        assertEquals("alice@example.com", ex.getEmail());
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void login_InvalidPassword_ThrowsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void login_NonExistentEmail_ThrowsInvalidCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void verifyEmail_CorrectOtp_VerifiesUserAndReturnsJwt() {
        VerifyEmailRequest request = VerifyEmailRequest.builder()
                .email("alice@example.com")
                .otp("123456")
                .build();

        sampleUser.setEmailVerified(true);
        when(emailVerificationService.verifyEmail("alice@example.com", "123456")).thenReturn(sampleUser);
        when(jwtTokenProvider.generateToken("alice@example.com")).thenReturn("verified.jwt.token");

        AuthResponse response = authService.verifyEmail(request);

        assertNotNull(response);
        assertEquals("verified.jwt.token", response.getToken());
        assertTrue(response.getUser().isEmailVerified());
        assertFalse(response.isVerificationRequired());
    }

    @Test
    void resendVerification_DelegatesToService() {
        ResendVerificationRequest request = ResendVerificationRequest.builder()
                .email("alice@example.com")
                .build();

        doNothing().when(emailVerificationService).resendVerification("alice@example.com");

        authService.resendVerification(request);

        verify(emailVerificationService).resendVerification("alice@example.com");
    }

    @Test
    void getCurrentUserProfile_ValidPrincipal_ReturnsProfile() {
        UserPrincipal principal = new UserPrincipal(sampleUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUserProfile(principal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("alice@example.com", response.getEmail());
        assertEquals("Alice", response.getName());
    }

    @Test
    void getCurrentUserProfile_NullPrincipal_ThrowsInvalidCredentialsException() {
        assertThrows(InvalidCredentialsException.class, () -> authService.getCurrentUserProfile(null));
    }
}
