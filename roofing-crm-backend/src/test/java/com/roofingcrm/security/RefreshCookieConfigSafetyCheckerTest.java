package com.roofingcrm.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshCookieConfigSafetyCheckerTest {

    @Test
    void looksLikeProductionUrl_recognizesHttpsAsProduction() {
        assertTrue(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl("https://app.example.com"));
    }

    @Test
    void looksLikeProductionUrl_recognizesNonLocalHttpAsProduction() {
        assertTrue(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl("http://app.example.com"));
    }

    @Test
    void looksLikeProductionUrl_treatsLocalhostAsDev() {
        assertFalse(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl("http://localhost:3000"));
        assertFalse(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl("http://127.0.0.1:8080"));
        assertFalse(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl("http://my-laptop.local"));
    }

    @Test
    void looksLikeProductionUrl_handlesBlankInput() {
        assertFalse(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl(null));
        assertFalse(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl(""));
        assertFalse(RefreshCookieConfigSafetyChecker.looksLikeProductionUrl("   "));
    }

    @Test
    void looksCrossSite_returnsFalseForSameRegistrableDomain() {
        assertFalse(RefreshCookieConfigSafetyChecker.looksCrossSite(
                "https://app.example.com",
                List.of("https://api.example.com")));
    }

    @Test
    void looksCrossSite_returnsTrueForDifferentRegistrableDomains() {
        assertTrue(RefreshCookieConfigSafetyChecker.looksCrossSite(
                "https://app.acme.com",
                List.of("https://other.example.com")));
    }

    @Test
    void looksCrossSite_returnsFalseWhenAnyOriginShares() {
        assertFalse(RefreshCookieConfigSafetyChecker.looksCrossSite(
                "https://app.acme.com",
                List.of("https://other.example.com", "https://api.acme.com")));
    }
}
