package com.fan.mixmyfit.file;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FileExceptionHandler {
    @ExceptionHandler(FileException.class)
    ResponseEntity<FileErrorResponse> handleFileException(FileException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new FileErrorResponse(exception.getCode(), exception.getMessage()));
    }
}
