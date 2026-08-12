package com.fan.mixmyfit.tag;

class TagException extends RuntimeException {
    private final String code;

    TagException(String code, String message) {
        super(message);
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
