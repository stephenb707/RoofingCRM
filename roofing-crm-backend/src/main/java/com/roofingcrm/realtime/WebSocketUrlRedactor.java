package com.roofingcrm.realtime;

/**
 * Redacts the {@code token=...} query parameter from URLs/strings before they go to logs.
 *
 * <p>Browsers cannot set custom headers on the WebSocket upgrade, so the access token is passed
 * as a query parameter on /ws. Query parameters can leak into reverse-proxy access logs and
 * exception stack traces if not redacted, so any code that logs a WebSocket URL or query
 * string MUST run it through {@link #redactToken(String)} first.
 *
 * <p>Reverse proxy reminder: load balancers and CDNs in front of this app must also strip or
 * redact the {@code token} query parameter from their access logs (e.g. nginx
 * {@code log_format} should mask {@code $arg_token}).
 */
public final class WebSocketUrlRedactor {

    private WebSocketUrlRedactor() {}

    public static String redactToken(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // Match token=... up to next & or end of string. Keep the marker so logs still note
        // that a token was present.
        return value.replaceAll("(?i)([?&])token=[^&]*", "$1token=REDACTED");
    }
}
