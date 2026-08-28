package com.heet.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.DateTimeException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationException(
                        MethodArgumentNotValidException exception) {

                Map<String, String> errors = new HashMap<>();

                exception.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
                        IllegalArgumentException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(DateTimeException.class)
        public ResponseEntity<Map<String, String>> handleDateTimeException(
                        DateTimeException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "Invalid year or month supplied."));
        }

        @ExceptionHandler(TransactionNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleTransactionNotFoundException(
                        TransactionNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(CategoryNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleCategoryNotFoundException(
                        CategoryNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(LoanRecordNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleLoanRecordNotFoundException(
                        LoanRecordNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(ReceiptNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleReceiptNotFoundException(
                        ReceiptNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(ReceiptOcrNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleReceiptOcrNotFoundException(
                        ReceiptOcrNotFoundException exception) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(InvalidReceiptException.class)
        public ResponseEntity<Map<String, String>> handleInvalidReceiptException(
                        InvalidReceiptException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
        public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(
                        org.springframework.web.multipart.MaxUploadSizeExceededException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", "File size exceeds maximum allowed limit of 10MB."));
        }

        @ExceptionHandler(FileStorageException.class)
        public ResponseEntity<Map<String, String>> handleFileStorageException(
                        FileStorageException exception) {

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(InvalidLoanPaymentException.class)
        public ResponseEntity<Map<String, String>> handleInvalidLoanPaymentException(
                        InvalidLoanPaymentException exception) {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(DuplicateCategoryException.class)
        public ResponseEntity<Map<String, String>> handleDuplicateCategoryException(
                        DuplicateCategoryException exception) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(CategoryInUseException.class)
        public ResponseEntity<Map<String, String>> handleCategoryInUseException(
                        CategoryInUseException exception) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<Map<String, String>> handleDuplicateEmailException(
                        DuplicateEmailException exception) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler({ InvalidCredentialsException.class,
                        org.springframework.security.authentication.BadCredentialsException.class })
        public ResponseEntity<Map<String, String>> handleInvalidCredentialsException(
                        Exception exception) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid email or password"));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, String>> handleGenericException(
                        Exception exception) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "An unexpected error occurred."));
        }
}