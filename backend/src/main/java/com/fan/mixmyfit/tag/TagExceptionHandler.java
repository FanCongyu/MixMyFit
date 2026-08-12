package com.fan.mixmyfit.tag;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class TagExceptionHandler {
    @ExceptionHandler(TagException.class)
    ResponseEntity<TagErrorResponse> handleTagException(TagException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TagErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
