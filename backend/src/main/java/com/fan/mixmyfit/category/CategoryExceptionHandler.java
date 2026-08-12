package com.fan.mixmyfit.category;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class CategoryExceptionHandler {
    @ExceptionHandler(CategoryException.class)
    ResponseEntity<CategoryErrorResponse> handleCategoryException(CategoryException exception) {
        HttpStatus status = "CATEGORY_NOT_FOUND".equals(exception.getCode())
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new CategoryErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
