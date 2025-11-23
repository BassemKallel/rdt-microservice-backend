package com.replate.offermanagementservice.exception;

import com.replate.offermanagementservice.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(errorMessage);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST); // 400
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND); // 404
    }

    @ExceptionHandler(ForbiddenPermissionException.class)
    public ResponseEntity<Object> handleForbiddenPermission(ForbiddenPermissionException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN); // 403
    }

    @ExceptionHandler(AccountNotValidatedException.class)
    public ResponseEntity<Object> handleAccountNotValidated(AccountNotValidatedException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage());
        // On utilise 403 (Forbidden) car l'utilisateur est authentifié mais n'a pas la permission de poster.
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN); // 403
    }
}