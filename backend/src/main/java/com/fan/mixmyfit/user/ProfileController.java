package com.fan.mixmyfit.user;

import com.fan.mixmyfit.security.SessionCookieFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
class ProfileController {
    private final ProfileService profiles;

    ProfileController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    ProfileResponse getProfile(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        return profiles.getProfile(sessionId);
    }

    @PatchMapping
    ProfileResponse updateProfile(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody ProfileUpdateRequest request) {
        return profiles.updateProfile(sessionId, request);
    }

    @PostMapping("/password")
    ResponseEntity<Void> updatePassword(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody PasswordUpdateRequest request) {
        profiles.updatePassword(sessionId, request);
        return ResponseEntity.noContent().build();
    }
}
