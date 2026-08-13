package com.fan.mixmyfit.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SecurityExceptionHandler {
    @ExceptionHandler(AuthenticationRequiredException.class)
    ResponseEntity<SecurityErrorResponse> handleAuthenticationRequired(AuthenticationRequiredException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new SecurityErrorResponse(AuthenticationRequiredException.CODE, exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<SecurityErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new SecurityErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
