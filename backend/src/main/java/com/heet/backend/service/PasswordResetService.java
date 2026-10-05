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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
public class PasswordResetService {

    public static final int TOKEN_EXPIRY_MINUTES = 15;
    public static final int RESEND_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Handles forgot password requests.
     * To prevent user enumeration, always returns neutrally without leaking existence.
     *
     * @param request the forgot password request with email
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String trimmedEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        Optional<User> userOpt = userRepository.findByEmail(trimmedEmail);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", maskEmail(trimmedEmail));
            return;
        }

        User user = userOpt.get();

        // Enforce cooldown to prevent spamming
        Optional<PasswordResetToken> latestOpt = resetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        LocalDateTime now = LocalDateTime.now();
        if (latestOpt.isPresent()) {
            LocalDateTime cooldownUntil = latestOpt.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (now.isBefore(cooldownUntil)) {
                long remainingSeconds = Duration.between(now, cooldownUntil).getSeconds();
                log.info("Password reset cooldown active for user id {}, ignoring request ({}s remaining)",
                        user.getId(), remainingSeconds);
                return;
            }
        }

        // Invalidate any previously active pending reset tokens for this user
        resetTokenRepository.invalidateAllPendingForUser(user.getId());

        // Generate cryptographically secure random token (256 bits / 32 bytes)
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiresAt = now.plusMinutes(TOKEN_EXPIRY_MINUTES);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .status(ResetTokenStatus.PENDING)
                .build();

        resetTokenRepository.save(resetToken);

        // Dispatch email containing the raw token (raw token is never stored or logged)
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), rawToken);
        } catch (Exception e) {
            log.error("Failed to send password reset email to user id {}: {}", user.getId(), e.getMessage());
            // Do not throw to keep email delivery failures opaque and prevent enumeration
        }
    }

    /**
     * Validates whether a reset token is valid, active, unexpired, and unused without consuming it.
     *
     * @param rawToken raw token received from query param
     * @return true if valid, throws PasswordResetException if invalid or expired
     */
    @Transactional
    public boolean validateResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        String tokenHash = hashToken(rawToken.trim());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new PasswordResetException("This password reset link is invalid or has expired."));

        LocalDateTime now = LocalDateTime.now();

        if (resetToken.getStatus() != ResetTokenStatus.PENDING || resetToken.getUsedAt() != null) {
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        if (now.isAfter(resetToken.getExpiresAt())) {
            resetToken.setStatus(ResetTokenStatus.EXPIRED);
            resetTokenRepository.save(resetToken);
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        return true;
    }

    /**
     * Resets a user's password using a valid raw reset token.
     * Marks token as USED, updates password hash, updates passwordChangedAt,
     * and invalidates all other pending reset tokens.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : "";
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        String tokenHash = hashToken(request.getToken().trim());
        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new PasswordResetException("This password reset link is invalid or has expired."));

        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        if (resetToken.getStatus() != ResetTokenStatus.PENDING || resetToken.getUsedAt() != null) {
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        if (now.isAfter(resetToken.getExpiresAt())) {
            resetToken.setStatus(ResetTokenStatus.EXPIRED);
            resetTokenRepository.save(resetToken);
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw new PasswordResetException("This password reset link is invalid or has expired.");
        }

        // Prevent setting the exact same password
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from your current password.");
        }

        // 1. Mark current reset token as USED
        resetToken.setStatus(ResetTokenStatus.USED);
        resetToken.setUsedAt(now);
        resetTokenRepository.save(resetToken);

        // 2. Invalidate any other active reset tokens for this user
        resetTokenRepository.invalidateAllPendingForUser(user.getId());

        // 3. Update user password and passwordChangedAt timestamp
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        log.info("Password successfully reset for user id {}", user.getId());
    }

    /**
     * Changes an authenticated user's password.
     * Verifies the current password against stored hash, validates new password rules,
     * updates passwordChangedAt, and invalidates any active reset tokens.
     */
    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        if (principal == null || principal.getUser() == null) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(InvalidCredentialsException::new);

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException("Current password is incorrect.");
        }

        String newPassword = request.getNewPassword() != null ? request.getNewPassword().trim() : "";
        String confirmPassword = request.getConfirmPassword() != null ? request.getConfirmPassword().trim() : "";

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long.");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from your current password.");
        }

        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        // Update password and record changed timestamp for session invalidation
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(now);
        userRepository.save(user);

        // Invalidate any pending reset tokens for this user
        resetTokenRepository.invalidateAllPendingForUser(user.getId());

        log.info("Password successfully changed by authenticated user id {}", user.getId());
    }

    /**
     * Computes deterministic SHA-256 hash of a raw token.
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
