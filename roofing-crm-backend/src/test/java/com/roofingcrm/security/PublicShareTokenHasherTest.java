package com.roofingcrm.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicShareTokenHasherTest {

    @Test
    void sha256HexUtf8_matchesLowercaseHexOfUtf8Bytes() {
        assertEquals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                PublicShareTokenHasher.sha256HexUtf8("hello"));
    }
}
