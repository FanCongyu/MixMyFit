package com.fan.mixmyfit.user;

import com.fan.mixmyfit.domain.User;

record ProfileResponse(Long userId, String username, String nickname) {
    static ProfileResponse from(User user) {
        return new ProfileResponse(user.getUserId(), user.getUsername(), user.getNickname());
    }
}
