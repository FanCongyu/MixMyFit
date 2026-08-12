package com.fan.mixmyfit.security;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OwnershipGuard {
    public void requireOwner(Long currentUserId, Long resourceOwnerUserId) {
        if (currentUserId == null
                || resourceOwnerUserId == null
                || !Objects.equals(currentUserId, resourceOwnerUserId)) {
            throw new AccessDeniedException("RESOURCE_NOT_FOUND", "Resource not found");
        }
    }
}
