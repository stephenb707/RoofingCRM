package com.roofingcrm.realtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WebSocketUrlRedactorTest {

    @Test
    void redactsTokenWhenOnlyQueryParam() {
        assertEquals(
                "/ws?token=REDACTED",
                WebSocketUrlRedactor.redactToken("/ws?token=eyJhbGciOiJIUzI1NiJ9.payload.signature"));
    }

    @Test
    void redactsTokenWhenAmongOtherParams() {
        assertEquals(
                "/ws?foo=bar&token=REDACTED&baz=qux",
                WebSocketUrlRedactor.redactToken("/ws?foo=bar&token=eyJhbGciOiJIUzI1NiJ9.payload&baz=qux"));
    }

    @Test
    void leavesUrlsWithoutTokenAlone() {
        assertEquals(
                "/ws?foo=bar",
                WebSocketUrlRedactor.redactToken("/ws?foo=bar"));
    }

    @Test
    void handlesNullAndEmpty() {
        assertNull(WebSocketUrlRedactor.redactToken(null));
        assertEquals("", WebSocketUrlRedactor.redactToken(""));
    }
}
