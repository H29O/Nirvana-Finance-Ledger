package com.heet.backend.exception;

public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(Long id) {
        super("Cannot delete category with id " + id + " because it is referenced by existing transactions");
    }

    public CategoryInUseException(String message) {
        super(message);
    }
}
