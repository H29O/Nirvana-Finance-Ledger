package com.heet.backend.service;

import com.heet.backend.entity.EmailVerification;
import com.heet.backend.entity.EmailVerification.VerificationStatus;
import com.heet.backend.entity.User;
import com.heet.backend.exception.EmailVerificationException;
import com.heet.backend.repository.EmailVerificationRepository;
import com.heet.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class EmailVerificationService {

    public static final int OTP_EXPIRY_MINUTES = 5;
    public static final int RESEND_COOLDOWN_SECONDS = 60;
    public static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationRepository verificationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Creates and sends a new email verification code for the given user.
     * Invalidates any previously active codes for this user.
     *
     * @param user the registered user
     * @return true if email was successfully dispatched, false if delivery failed
     */
    @Transactional
    public boolean createAndSendVerification(User user) {
        // Invalidate previous active verification attempts
        List<EmailVerification> activeVerifications = verificationRepository.findByUserIdAndStatus(
                user.getId(), VerificationStatus.PENDING);
        for (EmailVerification v : activeVerifications) {
            v.setStatus(VerificationStatus.INVALIDATED);
        }
        if (!activeVerifications.isEmpty()) {
            verificationRepository.saveAll(activeVerifications);
        }

        // Generate cryptographically secure 6-digit numeric code
        String rawOtp = generateSecure6DigitOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(OTP_EXPIRY_MINUTES);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .otpHash(hashedOtp)
                .createdAt(now)
                .expiresAt(expiresAt)
                .attemptCount(0)
                .status(VerificationStatus.PENDING)
                .build();

        verificationRepository.save(verification);

        // Send email to the exact registered user address
        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), rawOtp);
            return true;
        } catch (Exception e) {
            log.warn("Failed to deliver verification email during registration/generation for user id {}: {}",
                    user.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Resends a verification code for the given user with cooldown check.
     */
    @Transactional
    public void resendVerification(String email) {
        String trimmedEmail = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(trimmedEmail)
                .orElseThrow(() -> new EmailVerificationException("Account not found for email: " + trimmedEmail));

        if (user.isEmailVerified()) {
            throw new EmailVerificationException("Your email is already verified. Please log in.");
        }

        // Check cooldown on the latest verification record
        Optional<EmailVerification> latestOpt = verificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        if (latestOpt.isPresent()) {
            EmailVerification latest = latestOpt.get();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime cooldownUntil = latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (now.isBefore(cooldownUntil)) {
                long remainingSeconds = Duration.between(now, cooldownUntil).getSeconds();
                if (remainingSeconds < 1) remainingSeconds = 1;
                throw new EmailVerificationException(
                        "Please wait " + remainingSeconds + " seconds before requesting a new code.",
                        (int) remainingSeconds
                );
            }
        }

        // Generate and send new verification code
        boolean delivered = createAndSendVerification(user);
        if (!delivered) {
            throw new com.heet.backend.exception.EmailDeliveryException(
                    "Verification email could not be sent. Please try again.");
        }
    }

    /**
     * Verifies the submitted OTP for the user email.
     * Enforces expiry, attempt limits, and single-use rules.
     */
    @Transactional
    public User verifyEmail(String email, String submittedOtp) {
        String trimmedEmail = email != null ? email.trim() : "";
        User user = userRepository.findByEmail(trimmedEmail)
                .orElseThrow(() -> new EmailVerificationException("Account not found for email: " + trimmedEmail));

        if (user.isEmailVerified()) {
            return user; // Idempotent: already verified
        }

        EmailVerification verification = verificationRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), VerificationStatus.PENDING)
                .orElseThrow(() -> new EmailVerificationException("No active verification code found. Please request a new code."));

        LocalDateTime now = LocalDateTime.now();

        // 1. Check expiration
        if (now.isAfter(verification.getExpiresAt())) {
            verification.setStatus(VerificationStatus.EXPIRED);
            verificationRepository.save(verification);
            throw new EmailVerificationException("Verification code has expired. Please request a new code.");
        }

        // 2. Check and increment attempt count
        verification.setAttemptCount(verification.getAttemptCount() + 1);

        if (!passwordEncoder.matches(submittedOtp, verification.getOtpHash())) {
            if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
                verification.setStatus(VerificationStatus.INVALIDATED);
                verificationRepository.save(verification);
                throw new EmailVerificationException("Too many incorrect attempts. This code is now invalidated. Please request a new code.");
            }
            verificationRepository.save(verification);
            int remainingAttempts = MAX_ATTEMPTS - verification.getAttemptCount();
            throw new EmailVerificationException("Invalid verification code. " + remainingAttempts + " attempt(s) remaining.");
        }

        // 3. Mark verification as verified
        verification.setStatus(VerificationStatus.VERIFIED);
        verification.setUsedAt(now);
        verificationRepository.save(verification);

        // 4. Mark user as verified
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private String generateSecure6DigitOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
