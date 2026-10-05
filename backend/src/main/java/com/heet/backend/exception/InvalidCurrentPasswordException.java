package com.heet.backend.exception;

public class InvalidCurrentPasswordException extends RuntimeException {
    public InvalidCurrentPasswordException() {
        super("Current password is incorrect.");
    }

    public InvalidCurrentPasswordException(String message) {
        super(message);
    }
}
