package com.heet.backend.exception;

import lombok.Getter;

@Getter
public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Email is not verified. Please verify your email before logging in.");
        this.email = email;
    }
}
