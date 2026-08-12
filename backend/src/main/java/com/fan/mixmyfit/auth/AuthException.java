package com.fan.mixmyfit.auth;

class AuthException extends RuntimeException {
    private final String code;

    AuthException(String code, String message) {
        super(message);
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
