package com.heet.backend.exception;

public class InvalidLoanPaymentException extends RuntimeException {

    public InvalidLoanPaymentException(String message) {
        super(message);
    }
}
