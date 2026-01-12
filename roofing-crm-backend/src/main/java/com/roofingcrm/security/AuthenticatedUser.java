package com.roofingcrm.security;

import org.springframework.lang.NonNull;

import java.util.UUID;

public record AuthenticatedUser(@NonNull UUID userId, @NonNull String email) {
}
