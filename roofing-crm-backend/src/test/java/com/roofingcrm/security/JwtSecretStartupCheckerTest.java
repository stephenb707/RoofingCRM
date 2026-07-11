package com.roofingcrm.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecretStartupCheckerTest {

    @Test
    void prodProfileWithBlankSecretThrows() {
        JwtSecretStartupChecker checker = checker("prod", "   ");

        assertThrows(IllegalStateException.class, () -> checker.onApplicationReady(null));
    }

    @Test
    void prodProfileWithKnownDefaultThrows() {
        JwtSecretStartupChecker checker = checker(
                "production",
                "REPLACE_WITH_A_LONG_RANDOM_SECRET_KEY_FOR_DEV_ONLY_MIN_32_CHARS");

        assertThrows(IllegalStateException.class, () -> checker.onApplicationReady(null));
    }

    @Test
    void prodProfileWithFortyCharacterSecretPasses() {
        JwtSecretStartupChecker checker = checker(
                "prod",
                "a9B2c7D4e1F8g3H6j0K5m2N9p4Q7r1S8t6V3x5Z0");

        assertDoesNotThrow(() -> checker.onApplicationReady(null));
    }

    @Test
    void nonProdProfileWithShortSecretPasses() {
        JwtSecretStartupChecker checker = checker("dev", "short-secret");

        assertDoesNotThrow(() -> checker.onApplicationReady(null));
    }

    private JwtSecretStartupChecker checker(String profile, String secret) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        return new JwtSecretStartupChecker(environment, properties);
    }
}
