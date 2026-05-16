package com.roofingcrm.security;

import org.springframework.lang.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 over UTF-8 bytes, lowercase hex (matches PostgreSQL {@code encode(digest(...),'hex')}).
 */
public final class PublicShareTokenHasher {

    private PublicShareTokenHasher() {
    }

    public static @NonNull String sha256HexUtf8(@NonNull String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Objects.requireNonNull(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
