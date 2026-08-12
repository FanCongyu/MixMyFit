package com.fan.mixmyfit.category;

class CategoryException extends RuntimeException {
    private final String code;

    CategoryException(String code, String message) {
        super(message);
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
