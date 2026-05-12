package com.roofingcrm.realtime;

import com.roofingcrm.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Validates the JWT presented during the WebSocket handshake.
 *
 * <p>The token is passed as a query parameter (?token=...) because browsers cannot set custom
 * headers on the WebSocket upgrade. Query parameters can leak into reverse-proxy access logs,
 * so this interceptor:
 * <ul>
 *     <li>Never logs the raw URI or full query string.</li>
 *     <li>Routes any URI logging through {@link WebSocketUrlRedactor#redactToken(String)}.</li>
 *     <li>Relies on a short access-token lifetime to bound exposure if a log line is captured.</li>
 * </ul>
 *
 * <p>Reverse proxy operators must also redact the {@code token} query parameter in their own
 * access logs (e.g. nginx custom {@code log_format} masking {@code $arg_token}).
 *
 * <p>Session invalidation note: this only validates new handshakes. Established WS connections
 * are not bound to the lifetime of the JWT they used to handshake; a logged-out user who keeps
 * an open connection will continue to receive published messages until the next reconnect, at
 * which point the now-expired/rejected JWT will be denied here. Short access-token lifetime
 * keeps the worst-case window small (≈ access token expiration). If you need stricter
 * invalidation later, add a session registry that is closed on logout.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private static final String TOKEN_PARAM = "token";

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        String token = getToken(request);
        if (!StringUtils.hasText(token)) {
            log.debug("WS handshake rejected: missing token (path={})", request.getURI().getPath());
            return false;
        }
        try {
            jwtService.parseToken(token);
            return true;
        } catch (Exception e) {
            // Never include the URI/query here — it carries the (now-invalid) token.
            log.debug("WS handshake rejected: invalid token (path={})", request.getURI().getPath());
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // No-op
    }

    private String getToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String param = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
            if (param != null) return param;
        }
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && TOKEN_PARAM.equals(pair.substring(0, eq))) {
                    return pair.substring(eq + 1);
                }
            }
        }
        return null;
    }
}
