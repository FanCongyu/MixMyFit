package com.fan.mixmyfit.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public String create(Long userId) {
        byte[] token = new byte[32];
        secureRandom.nextBytes(token);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        sessions.put(sessionId, userId);
        return sessionId;
    }

    public Optional<Long> findUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void invalidate(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessions.remove(sessionId);
    }
}
