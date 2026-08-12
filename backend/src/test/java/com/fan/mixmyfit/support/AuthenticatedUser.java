package com.fan.mixmyfit.support;

import com.fan.mixmyfit.domain.User;
import org.springframework.http.HttpHeaders;

public record AuthenticatedUser(User user, HttpHeaders sessionHeaders) {
}
