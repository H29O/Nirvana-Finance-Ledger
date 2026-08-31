package com.heet.backend.service;

import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.RegisterRequest;
import com.heet.backend.dto.response.AuthResponse;
import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.entity.User;
import com.heet.backend.exception.DuplicateEmailException;
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
    }

    @Test
    void register_EncodesPasswordWithBCryptAndReturnsToken() {
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
        when(jwtTokenProvider.generateToken("alice@example.com")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("alice@example.com", response.getUser().getEmail());

        // Verify password was strictly hashed and never saved as plaintext
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("$2a$10$encodedPasswordHash", savedUser.getPasswordHash());
        assertNotEquals("plainTextPass123", savedUser.getPasswordHash());
        verify(passwordEncoder).encode("plainTextPass123");
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
        verify(jwtTokenProvider, never()).generateToken(any());
    }

    @Test
    void login_ValidCredentials_ReturnsTokenAndUserProfile() {
        LoginRequest request = LoginRequest.builder()
                .email("alice@example.com")
                .password("plainTextPass123")
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("plainTextPass123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken("alice@example.com")).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
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
