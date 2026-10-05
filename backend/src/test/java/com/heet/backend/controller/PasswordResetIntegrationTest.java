package com.heet.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heet.backend.dto.request.ChangePasswordRequest;
import com.heet.backend.dto.request.ForgotPasswordRequest;
import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.ResetPasswordRequest;
import com.heet.backend.entity.PasswordResetToken;
import com.heet.backend.entity.PasswordResetToken.ResetTokenStatus;
import com.heet.backend.entity.User;
import com.heet.backend.repository.PasswordResetTokenRepository;
import com.heet.backend.repository.UserRepository;
import com.heet.backend.security.JwtTokenProvider;
import com.heet.backend.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class PasswordResetIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User verifiedUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        org.springframework.mail.javamail.JavaMailSenderImpl dummySender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        when(mailSender.createMimeMessage()).thenReturn(dummySender.createMimeMessage());

        // Create a verified test user
        userRepository.findByEmail("resetuser@example.com").ifPresent(userRepository::delete);

        verifiedUser = new User();
        verifiedUser.setName("Reset Tester");
        verifiedUser.setEmail("resetuser@example.com");
        verifiedUser.setPasswordHash(passwordEncoder.encode("InitialPassword123!"));
        verifiedUser.setEmailVerified(true);
        verifiedUser = userRepository.save(verifiedUser);
    }

    // =========================================================================
    // PART 1, 2, 3: FORGOT PASSWORD ENDPOINT & ANTI-ENUMERATION
    // =========================================================================

    @Test
    void forgotPassword_ExistingUser_ReturnsNeutralResponseAndCreatesHashedToken() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("resetuser@example.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If an account exists for this email, we've sent a password reset link.")))
                .andExpect(jsonPath("$.emailDeliverySuccess", is(true)));

        // Verify token record in database
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(verifiedUser.getId());
        assertTrue(tokenOpt.isPresent());
        PasswordResetToken dbToken = tokenOpt.get();
        assertEquals(ResetTokenStatus.PENDING, dbToken.getStatus());
        assertEquals(64, dbToken.getTokenHash().length());
        assertNull(dbToken.getUsedAt());

        // Verify email was dispatched
        verify(mailSender, atLeastOnce()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void forgotPassword_UnknownUser_ReturnsIdenticalNeutralResponseWithoutEnumeration() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nonexistent.user.999@example.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("If an account exists for this email, we've sent a password reset link.")))
                .andExpect(jsonPath("$.emailDeliverySuccess", is(true)));

        // No email sent for unknown user
        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    // =========================================================================
    // PART 4 & 5: TOKEN VALIDATION & RESET PASSWORD ENDPOINTS
    // =========================================================================

    @Test
    void validateResetToken_ValidAndExpiredTokens() throws Exception {
        String rawToken = "rawSecureToken12345678901234567890123456789012";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(verifiedUser)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .status(ResetTokenStatus.PENDING)
                .build();
        resetTokenRepository.save(token);

        // 1. Valid token -> 200 OK
        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));

        // 2. Expire token
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        resetTokenRepository.save(token);

        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", rawToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("This password reset link is invalid or has expired.")));

        // 3. Completely bogus token -> 400 Bad Request
        mockMvc.perform(get("/api/auth/validate-reset-token")
                        .param("token", "totallyInvalidRandomToken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("This password reset link is invalid or has expired.")));
    }

    @Test
    void resetPassword_FullFlow_SingleUse_OldPasswordFails_NewPasswordSucceeds() throws Exception {
        String rawToken = "validResetToken12345678901234567890";
        String tokenHash = PasswordResetService.hashToken(rawToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .user(verifiedUser)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .status(ResetTokenStatus.PENDING)
                .build();
        resetTokenRepository.save(token);

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("SuperSecretNewPassword99!")
                .confirmPassword("SuperSecretNewPassword99!")
                .build();

        // 1. Submit valid password reset
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password changed successfully.")))
                .andExpect(jsonPath("$.success", is(true)));

        // 2. Try using the exact same reset token a second time -> Rejected (Single use)
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("This password reset link is invalid or has expired.")));

        // 3. Login with OLD password -> Rejected
        LoginRequest oldLoginReq = LoginRequest.builder()
                .email("resetuser@example.com")
                .password("InitialPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginReq)))
                .andExpect(status().isUnauthorized());

        // 4. Login with NEW password -> Succeeds
        LoginRequest newLoginReq = LoginRequest.builder()
                .email("resetuser@example.com")
                .password("SuperSecretNewPassword99!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email", is("resetuser@example.com")));
    }

    // =========================================================================
    // PART 7 & 8: SETTINGS CHANGE PASSWORD ENDPOINT
    // =========================================================================

    @Test
    void changePassword_Unauthenticated_RejectedWith401() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("InitialPassword123!")
                .newPassword("BrandNewPassword123!")
                .confirmPassword("BrandNewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_IncorrectCurrentPassword_Returns400() throws Exception {
        String jwt = jwtTokenProvider.generateToken(verifiedUser.getEmail());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongInitialPassword")
                .newPassword("BrandNewPassword123!")
                .confirmPassword("BrandNewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Current password is incorrect.")));
    }

    @Test
    void changePassword_Successful_OldPasswordNoLongerWorks() throws Exception {
        String jwt = jwtTokenProvider.generateToken(verifiedUser.getEmail());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("InitialPassword123!")
                .newPassword("BrandNewPassword123!")
                .confirmPassword("BrandNewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Password changed successfully.")))
                .andExpect(jsonPath("$.success", is(true)));

        // Login with old password -> 401
        LoginRequest oldLoginReq = LoginRequest.builder()
                .email("resetuser@example.com")
                .password("InitialPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginReq)))
                .andExpect(status().isUnauthorized());

        // Login with new password -> 200 OK
        LoginRequest newLoginReq = LoginRequest.builder()
                .email("resetuser@example.com")
                .password("BrandNewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLoginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void changePassword_NewPasswordSameAsCurrent_Returns400() throws Exception {
        String jwt = jwtTokenProvider.generateToken(verifiedUser.getEmail());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("InitialPassword123!")
                .newPassword("InitialPassword123!")
                .confirmPassword("InitialPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("New password must be different from your current password.")));
    }
}
