package com.heet.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heet.backend.dto.request.LoginRequest;
import com.heet.backend.dto.request.RegisterRequest;
import com.heet.backend.dto.request.ResendVerificationRequest;
import com.heet.backend.dto.request.VerifyEmailRequest;
import com.heet.backend.entity.EmailVerification;
import com.heet.backend.entity.EmailVerification.VerificationStatus;
import com.heet.backend.entity.User;
import com.heet.backend.repository.EmailVerificationRepository;
import com.heet.backend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class EmailVerificationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private JavaMailSender mailSender;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        org.springframework.mail.javamail.JavaMailSenderImpl dummySender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        org.mockito.Mockito.when(mailSender.createMimeMessage())
                .thenReturn(dummySender.createMimeMessage());
    }

    @Test
    void completeVerificationFlow_Register_UnverifiedLoginFails_VerifyOtp_LoginSucceeds() throws Exception {
        String email = "flowtest@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Flow Tester")
                .email(email)
                .password("Password123!")
                .build();

        // 1. Register: Creates user, unverified, verificationRequired = true, token = null
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", nullValue()))
                .andExpect(jsonPath("$.verificationRequired", is(true)))
                .andExpect(jsonPath("$.emailDeliverySuccess", is(true)))
                .andExpect(jsonPath("$.message", is("Verification email sent.")))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.emailVerified", is(false)));

        User user = userRepository.findByEmail(email).orElseThrow();
        assertFalse(user.isEmailVerified());

        // Check verification record in DB
        Optional<EmailVerification> recordOpt = verificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(), VerificationStatus.PENDING);
        assertTrue(recordOpt.isPresent());
        EmailVerification record = recordOpt.get();
        assertEquals(0, record.getAttemptCount());

        // 2. Login before verification: Must be rejected with 403 Forbidden
        LoginRequest loginReq = LoginRequest.builder()
                .email(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.verificationRequired", is(true)))
                .andExpect(jsonPath("$.email", is(email)));

        // 3. Submit incorrect OTP: 400 Bad Request
        VerifyEmailRequest wrongOtpReq = VerifyEmailRequest.builder()
                .email(email)
                .otp("000000")
                .build();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Invalid verification code. 4 attempt(s) remaining.")));

        // 4. Manually set a known OTP hash for testing correct verification
        String knownOtp = "789123";
        record.setOtpHash(passwordEncoder.encode(knownOtp));
        verificationRepository.save(record);

        // 5. Submit correct OTP: 200 OK with authenticated JWT
        VerifyEmailRequest correctOtpReq = VerifyEmailRequest.builder()
                .email(email)
                .otp(knownOtp)
                .build();

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.verificationRequired", is(false)))
                .andExpect(jsonPath("$.user.emailVerified", is(true)))
                .andReturn();

        // 6. Login after verification: 200 OK with authenticated JWT
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.emailVerified", is(true)));
    }

    @Test
    void resendVerification_CooldownEnforced() throws Exception {
        String email = "cooldowntest@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Cooldown Tester")
                .email(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Immediately request resend -> should be rejected by 60s cooldown
        ResendVerificationRequest resendReq = ResendVerificationRequest.builder()
                .email(email)
                .build();

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());
    }

    @Test
    void register_WhenEmailDeliveryFails_ReturnsFailureMessageAndPreservesRecord() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        String email = "failtest@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Fail Tester")
                .email(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailDeliverySuccess", is(false)))
                .andExpect(jsonPath("$.message", is("Verification email could not be sent. Please try again.")))
                .andExpect(jsonPath("$.verificationRequired", is(true)))
                .andExpect(jsonPath("$.user.email", is(email)));

        // User and verification record are still created so user can request resend later
        User user = userRepository.findByEmail(email).orElseThrow();
        assertFalse(user.isEmailVerified());
        assertTrue(verificationRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), VerificationStatus.PENDING).isPresent());
    }

    @Test
    void resendVerification_WhenEmailDeliveryFails_Returns502() throws Exception {
        // Register first
        String email = "resendfail@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .name("Resend Tester")
                .email(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Expire cooldown manually in DB
        User user = userRepository.findByEmail(email).orElseThrow();
        EmailVerification record = verificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElseThrow();
        record.setCreatedAt(LocalDateTime.now().minusSeconds(70));
        verificationRepository.save(record);

        // Make mailSender throw on resend
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP auth error"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        ResendVerificationRequest resendReq = ResendVerificationRequest.builder()
                .email(email)
                .build();

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error", is("Verification email could not be sent. Please try again.")))
                .andExpect(jsonPath("$.emailDeliverySuccess", is(false)));
    }
}
