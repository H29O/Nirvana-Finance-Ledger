package com.heet.backend.exception;

import lombok.Getter;

@Getter
public class EmailVerificationException extends RuntimeException {

    private final int remainingCooldownSeconds;

    public EmailVerificationException(String message) {
        super(message);
        this.remainingCooldownSeconds = 0;
    }

    public EmailVerificationException(String message, int remainingCooldownSeconds) {
        super(message);
        this.remainingCooldownSeconds = remainingCooldownSeconds;
    }
}
