package com.fan.mixmyfit.security;

import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {
    private final SessionRegistry sessions;
    private final UserRepository users;

    public CurrentUserResolver(SessionRegistry sessions, UserRepository users) {
        this.sessions = sessions;
        this.users = users;
    }

    public User requireUser(String sessionId) {
        Long userId = sessions.findUserId(sessionId)
                .orElseThrow(() -> new AuthenticationRequiredException());
        return users.findById(userId)
                .orElseThrow(() -> new AuthenticationRequiredException());
    }

    public Long requireUserId(String sessionId) {
        return requireUser(sessionId).getUserId();
    }
}
