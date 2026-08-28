package com.heet.backend.exception;

public class ReceiptNotFoundException extends RuntimeException {

    public ReceiptNotFoundException(Long id) {
        super("Receipt with id " + id + " not found");
    }
}
