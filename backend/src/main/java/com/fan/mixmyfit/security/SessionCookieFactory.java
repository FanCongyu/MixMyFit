package com.fan.mixmyfit.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class SessionCookieFactory {
    public static final String SESSION_COOKIE_NAME = "MMF_SESSION";
    private static final Duration SESSION_TTL = Duration.ofDays(7);

    private final boolean secureCookie;
    private final String sameSite;

    public SessionCookieFactory(
            @Value("${mixmyfit.auth.cookie-secure:false}") boolean secureCookie,
            @Value("${mixmyfit.auth.cookie-same-site:Lax}") String sameSite) {
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
    }

    public ResponseCookie createSessionCookie(String sessionId) {
        return baseCookie(sessionId)
                .maxAge(SESSION_TTL)
                .build();
    }

    public ResponseCookie clearSessionCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, value)
                .path("/")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite);
    }
}
