package com.replate.offermanagementservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenPermissionException extends RuntimeException {
    public ForbiddenPermissionException(String message) {
        super(message);
    }
}
