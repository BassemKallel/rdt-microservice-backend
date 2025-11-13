package com.replate.usermanagementservice.exception;

public class MissingRequiredFieldsException extends RuntimeException {
    public MissingRequiredFieldsException(String message) {
        super(message);
    }
}
