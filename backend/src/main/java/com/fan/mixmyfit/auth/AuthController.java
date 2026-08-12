package com.fan.mixmyfit.auth;

import com.fan.mixmyfit.security.SessionCookieFactory;
import com.fan.mixmyfit.security.SessionRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {
    private final AuthService authService;
    private final SessionCookieFactory cookieFactory;
    private final SessionRegistry sessions;

    AuthController(AuthService authService, SessionCookieFactory cookieFactory, SessionRegistry sessions) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
        this.sessions = sessions;
    }

    @PostMapping("/register")
    ResponseEntity<AuthUserResponse> register(@RequestBody RegisterRequest request) {
        AuthUserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    ResponseEntity<AuthUserResponse> login(@RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.createSessionCookie(result.sessionId()).toString())
                .body(result.user());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        sessions.invalidate(sessionId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clearSessionCookie().toString())
                .build();
    }
}
