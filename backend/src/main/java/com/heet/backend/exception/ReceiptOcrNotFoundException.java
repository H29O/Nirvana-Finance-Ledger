package com.heet.backend.exception;

public class ReceiptOcrNotFoundException extends RuntimeException {

    public ReceiptOcrNotFoundException(Long receiptId) {
        super("OCR data for receipt with id " + receiptId + " not found");
    }
}
