package com.fan.mixmyfit.outfit;

class OutfitException extends RuntimeException {
    private final String code;

    OutfitException(String code, String message) {
        super(message);
        this.code = code;
    }

    String getCode() {
        return code;
    }
}
