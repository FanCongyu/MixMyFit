package com.fan.mixmyfit.user;

class UserException extends RuntimeException {
    private final String code;

    UserException(String code, String message) {
        super(message);
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
