package com.heet.backend.exception;

public class LoanRecordNotFoundException extends RuntimeException {

    public LoanRecordNotFoundException(Long id) {
        super("Loan record with id " + id + " not found");
    }
}
