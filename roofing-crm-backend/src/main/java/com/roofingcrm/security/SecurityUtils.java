package com.roofingcrm.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser)) {
            return null;
        }
        return (AuthenticatedUser) auth.getPrincipal();
    }

    public static UUID getCurrentUserIdOrThrow() {
        AuthenticatedUser user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return user.userId();
    }
}
