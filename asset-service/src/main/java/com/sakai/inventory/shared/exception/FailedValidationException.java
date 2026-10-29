package com.sakai.inventory.shared.exception;

public class FailedValidationException extends RuntimeException {
    public FailedValidationException(String message) {
        super(message);
    }
}
