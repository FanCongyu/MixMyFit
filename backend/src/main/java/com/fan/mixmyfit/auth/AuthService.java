package com.fan.mixmyfit.auth;

import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.security.SessionRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {
    private static final String INVALID_CREDENTIALS = "Invalid username or password";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessions;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder, SessionRegistry sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    @Transactional
    AuthUserResponse register(RegisterRequest request) {
        String username = required(request.username(), "USERNAME_REQUIRED", "Username is required");
        String password = required(request.password(), "PASSWORD_REQUIRED", "Password is required");
        String confirmPassword = required(request.confirmPassword(), "PASSWORD_REQUIRED", "Password confirmation is required");
        if (!password.equals(confirmPassword)) {
            throw new AuthException("PASSWORD_MISMATCH", "Password confirmation does not match");
        }
        if (users.existsByUsername(username)) {
            throw new AuthException("USERNAME_UNAVAILABLE", "Username is unavailable");
        }

        try {
            User saved = users.saveAndFlush(new User(username, passwordEncoder.encode(password), blankToNull(request.nickname())));
            return AuthUserResponse.from(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new AuthException("USERNAME_UNAVAILABLE", "Username is unavailable");
        }
    }

    @Transactional(readOnly = true)
    LoginResult login(LoginRequest request) {
        String username = required(request.username(), "INVALID_CREDENTIALS", INVALID_CREDENTIALS);
        String password = required(request.password(), "INVALID_CREDENTIALS", INVALID_CREDENTIALS);

        User user = users.findByUsername(username)
                .filter(candidate -> passwordEncoder.matches(password, candidate.getPasswordHash()))
                .orElseThrow(() -> new AuthException("INVALID_CREDENTIALS", INVALID_CREDENTIALS));

        return new LoginResult(AuthUserResponse.from(user), sessions.create(user.getUserId()));
    }

    private static String required(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new AuthException(code, message);
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
