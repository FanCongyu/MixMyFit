package com.fan.mixmyfit.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class UserExceptionHandler {
    @ExceptionHandler(UserException.class)
    ResponseEntity<UserErrorResponse> handleUserException(UserException exception) {
        HttpStatus status = "AUTHENTICATION_REQUIRED".equals(exception.getCode())
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new UserErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
