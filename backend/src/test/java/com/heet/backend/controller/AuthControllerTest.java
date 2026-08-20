package com.heet.backend.controller;

import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.RegisterRequest;
import com.heet.backend.dto.response.AuthResponse;
import com.heet.backend.dto.response.UserResponse;
import com.heet.backend.exception.DuplicateEmailException;
import com.heet.backend.exception.InvalidCredentialsException;
import com.heet.backend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        loginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        authResponse = AuthResponse.builder()
                .token("mock-jwt-token")
                .user(userResponse)
                .build();
    }

    @Test
    void register_Success_Returns201Created() {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.register(registerRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mock-jwt-token", response.getBody().getToken());
        assertEquals("john@example.com", response.getBody().getUser().getEmail());
    }

    @Test
    void register_DuplicateEmail_ThrowsDuplicateEmailException() {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateEmailException("john@example.com"));

        assertThrows(DuplicateEmailException.class, () -> authController.register(registerRequest));
    }

    @Test
    void login_Success_Returns200OK() {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        ResponseEntity<AuthResponse> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mock-jwt-token", response.getBody().getToken());
    }

    @Test
    void login_InvalidCredentials_ThrowsInvalidCredentialsException() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        assertThrows(InvalidCredentialsException.class, () -> authController.login(loginRequest));
    }
}
