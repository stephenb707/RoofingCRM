package com.roofingcrm.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * Refuses to start with an unsafe JWT signing secret when a production-like profile is active.
 */
@Component
public class JwtSecretStartupChecker {

    private static final Logger log = LoggerFactory.getLogger(JwtSecretStartupChecker.class);
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final Set<String> KNOWN_DEFAULT_SECRETS = Set.of(
            "REPLACE_WITH_A_LONG_RANDOM_SECRET_KEY_FOR_DEV_ONLY_MIN_32_CHARS",
            "dev_only_change_me_to_a_long_random_string_32_chars_min");

    private final Environment environment;
    private final JwtProperties jwtProperties;

    public JwtSecretStartupChecker(Environment environment,
                                   JwtProperties jwtProperties) {
        this.environment = environment;
        this.jwtProperties = jwtProperties;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        boolean prodLike = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "APP_SECURITY_JWT_SECRET (app.security.jwt.secret) is required in every profile.");
        }

        String trimmedSecret = secret.trim();
        boolean knownDefault = KNOWN_DEFAULT_SECRETS.contains(trimmedSecret);
        boolean tooShort = trimmedSecret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES;

        if (prodLike && (knownDefault || tooShort)) {
            throw new IllegalStateException(
                    "APP_SECURITY_JWT_SECRET (app.security.jwt.secret) must be at least 32 UTF-8 bytes "
                            + "and must not use a shipped default when running with a production profile.");
        }
        if (!prodLike && (knownDefault || tooShort)) {
            log.warn(
                    "APP_SECURITY_JWT_SECRET (app.security.jwt.secret) is unsafe: it must be at least "
                            + "32 UTF-8 bytes and must not use a shipped default before production deployment.");
        }
    }
}
