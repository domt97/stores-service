package com.dotran.example.store.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException() {
        super("Resource not found");
    }

    public NotFoundException(String resourceName, String id) {
        super(resourceName + " with ID " + id + " not found");
    }
}
