package com.replate.offermanagementservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN) // 403
public class AccountNotValidatedException extends RuntimeException {
    public AccountNotValidatedException(String message) {
        super(message);
    }
}
