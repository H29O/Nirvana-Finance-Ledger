package com.heet.backend.exception;

public class DuplicateCategoryException extends RuntimeException {

    public DuplicateCategoryException(String name) {
        super("Category with name '" + name + "' already exists");
    }
}
