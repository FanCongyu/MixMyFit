package com.fan.mixmyfit.user;

import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.security.CurrentUserResolver;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ProfileService {
    private final CurrentUserResolver currentUsers;
    private final PasswordEncoder passwordEncoder;

    ProfileService(CurrentUserResolver currentUsers, PasswordEncoder passwordEncoder) {
        this.currentUsers = currentUsers;
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
        return currentUsers.requireUser(sessionId);
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
