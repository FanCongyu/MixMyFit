package com.fan.mixmyfit.auth;

import com.fan.mixmyfit.domain.User;

record AuthUserResponse(Long userId, String username, String nickname) {
    static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getUserId(), user.getUsername(), user.getNickname());
    }
}
