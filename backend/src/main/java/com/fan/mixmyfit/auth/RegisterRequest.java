package com.fan.mixmyfit.auth;

record RegisterRequest(String username, String password, String confirmPassword, String nickname) {
}
