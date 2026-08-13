package com.fan.mixmyfit.file;

public class FileException extends RuntimeException {
    private final String code;

    public FileException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
