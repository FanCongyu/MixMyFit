package com.fan.mixmyfit.user;

import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.security.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProfileService {
    private final UserRepository users;
    private final SessionRegistry sessions;
    private final PasswordEncoder passwordEncoder;

    ProfileService(UserRepository users, SessionRegistry sessions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    ProfileResponse getProfile(String sessionId) {
        return ProfileResponse.from(currentUser(sessionId));
    }

    @Transactional
    ProfileResponse updateProfile(String sessionId, ProfileUpdateRequest request) {
        User user = currentUser(sessionId);
        user.updateNickname(blankToNull(request.nickname()));
        return ProfileResponse.from(user);
    }

    @Transactional
    void updatePassword(String sessionId, PasswordUpdateRequest request) {
        User user = currentUser(sessionId);
        String oldPassword = required(request.oldPassword(), "OLD_PASSWORD_REQUIRED", "Old password is required");
        String newPassword = required(request.newPassword(), "NEW_PASSWORD_REQUIRED", "New password is required");
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new UserException("INVALID_OLD_PASSWORD", "Old password is invalid");
        }
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
    }

    private User currentUser(String sessionId) {
        Long userId = sessions.findUserId(sessionId)
                .orElseThrow(() -> new UserException("AUTHENTICATION_REQUIRED", "Authentication is required"));
        return users.findById(userId)
                .orElseThrow(() -> new UserException("AUTHENTICATION_REQUIRED", "Authentication is required"));
    }

    private static String required(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new UserException(code, message);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
