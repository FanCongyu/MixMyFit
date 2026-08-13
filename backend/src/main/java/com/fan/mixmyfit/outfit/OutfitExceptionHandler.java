package com.fan.mixmyfit.outfit;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class OutfitExceptionHandler {
    @ExceptionHandler(OutfitException.class)
    ResponseEntity<OutfitErrorResponse> handleOutfitException(OutfitException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new OutfitErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
