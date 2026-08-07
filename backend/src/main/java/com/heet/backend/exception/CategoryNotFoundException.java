package com.heet.backend.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category with id " + id + " not found");
    }

    public CategoryNotFoundException(String message) {
        super(message);
    }
}
