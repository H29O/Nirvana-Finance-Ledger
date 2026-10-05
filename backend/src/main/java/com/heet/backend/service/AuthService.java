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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CategoryService categoryService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            CategoryService categoryService,
            EmailVerificationService emailVerificationService) {
        this(userRepository, passwordEncoder, jwtTokenProvider, categoryService, emailVerificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            CategoryService categoryService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.categoryService = categoryService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    @Transactional
    public void forgotPassword(com.heet.backend.dto.request.ForgotPasswordRequest request) {
        if (passwordResetService != null) {
            passwordResetService.forgotPassword(request);
        }
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        if (passwordResetService != null) {
            return passwordResetService.validateResetToken(token);
        }
        return false;
    }

    @Transactional
    public void resetPassword(com.heet.backend.dto.request.ResetPasswordRequest request) {
        if (passwordResetService != null) {
            passwordResetService.resetPassword(request);
        }
    }

    @Transactional
    public void changePassword(UserPrincipal principal, com.heet.backend.dto.request.ChangePasswordRequest request) {
        if (passwordResetService != null) {
            passwordResetService.changePassword(principal, request);
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String trimmedEmail = request.getEmail().trim();

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new DuplicateEmailException(trimmedEmail);
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(trimmedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        // Seed initial default categories for the new user account
        categoryService.createDefaultCategoriesForUser(savedUser);

        // Generate and send 6-digit OTP verification code
        boolean emailSent = emailVerificationService.createAndSendVerification(savedUser);

        String message = emailSent
                ? "Verification email sent."
                : "Verification email could not be sent. Please try again.";

        // Do not issue JWT for unverified account
        return AuthResponse.builder()
                .token(null)
                .user(mapToUserResponse(savedUser))
                .message(message)
                .verificationRequired(true)
                .emailDeliverySuccess(emailSent)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String trimmedEmail = request.getEmail().trim();

        User user = userRepository.findByEmail(trimmedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // Reject unverified login attempts with clear notice without exposing sensitive details
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(user.getEmail());
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());
        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .message("Login successful.")
                .verificationRequired(false)
                .build();
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User verifiedUser = emailVerificationService.verifyEmail(request.getEmail(), request.getOtp());
        String token = jwtTokenProvider.generateToken(verifiedUser.getEmail());
        return AuthResponse.builder()
                .token(token)
                .user(mapToUserResponse(verifiedUser))
                .message("Email successfully verified.")
                .verificationRequired(false)
                .build();
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.getEmail());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(InvalidCredentialsException::new);
        return mapToUserResponse(user);
    }

    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
