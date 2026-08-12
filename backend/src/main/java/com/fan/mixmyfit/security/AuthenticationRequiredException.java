package com.fan.mixmyfit.security;

public class AuthenticationRequiredException extends RuntimeException {
    static final String CODE = "AUTHENTICATION_REQUIRED";

    public AuthenticationRequiredException() {
        super("Authentication is required");
    }
}
