package com.roofingcrm.security.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.roofingcrm.api.ApiErrorResponse;
import com.roofingcrm.security.RefreshTokenProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

/**
 * Rate limits authentication and anonymous public resource endpoints. Uses an in-memory per-minute counter
 * per composite key (IP plus SHA-256 hashes of normalized email, refresh cookie, or public token, as documented
 * on each branch).
 */
public class HighRiskEndpointRateLimitFilter extends OncePerRequestFilter {

    public static final String SAFE_LIMIT_MESSAGE = "Too many requests. Please try again later.";

    private static final ObjectMapper JSON = Jackson2ObjectMapperBuilder.json()
            .modulesToInstall(new JavaTimeModule())
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final RateLimitProperties properties;
    private final MinuteWindowRateLimiter limiter;
    private final RefreshTokenProperties refreshTokenProperties;

    public HighRiskEndpointRateLimitFilter(
            RateLimitProperties properties,
            MinuteWindowRateLimiter limiter,
            RefreshTokenProperties refreshTokenProperties) {
        this.properties = Objects.requireNonNull(properties);
        this.limiter = Objects.requireNonNull(limiter);
        this.refreshTokenProperties = Objects.requireNonNull(refreshTokenProperties);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.isEnabled() || HttpMethod.OPTIONS.matches(Objects.requireNonNull(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = pathWithinApplication(request);
        String method = request.getMethod();

        Route route = matchRoute(method, path);
        if (route == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(route);
        if (limit <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest requestToChain = request;
        byte[] jsonBody = null;

        if (route.needsJsonBody()) {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
            requestToChain = wrapped;
            jsonBody = wrapped.getCachedBody();
        }

        String clientIp = clientIp(request);
        String scopeKey = buildScopeKey(route, path, clientIp, requestToChain, jsonBody);

        if (!limiter.tryAcquire(scopeKey, limit)) {
            writeTooManyRequests(response, request);
            return;
        }

        filterChain.doFilter(requestToChain, response);
    }

    private int resolveLimit(Route route) {
        return switch (route) {
            case AUTH_LOGIN -> properties.getLoginPerMinute();
            case AUTH_REGISTER -> properties.getRegisterPerMinute();
            case AUTH_REGISTER_INVITE -> properties.getRegisterInvitePerMinute();
            case AUTH_REFRESH -> properties.getRefreshPerMinute();
            case PUBLIC_RESOURCE_GET -> properties.getPublicResourceGetPerMinute();
            case PUBLIC_ESTIMATE_DECISION_POST -> properties.getPublicEstimateDecisionPostPerMinute();
        };
    }

    private String buildScopeKey(
            Route route,
            String path,
            String clientIp,
            HttpServletRequest request,
            byte[] jsonBody) {
        return switch (route) {
            case AUTH_LOGIN -> "login:" + clientIp + ":" + emailHashFromJson(jsonBody, "email");
            case AUTH_REGISTER -> "reg:" + clientIp + ":" + emailHashFromJson(jsonBody, "email");
            case AUTH_REGISTER_INVITE -> "reginv:" + clientIp + ":" + emailHashFromJson(jsonBody, "email");
            case AUTH_REFRESH -> "refresh:" + clientIp + ":" + refreshCookieHash(request);
            case PUBLIC_RESOURCE_GET, PUBLIC_ESTIMATE_DECISION_POST ->
                    "pub:" + route.name() + ":" + clientIp + ":" + tokenHashFromPublicPath(path, route);
        };
    }

    private String emailHashFromJson(byte[] jsonBody, String field) {
        if (jsonBody == null || jsonBody.length == 0) {
            return RateLimitHashing.sha256Hex("");
        }
        try {
            JsonNode root = JSON.readTree(jsonBody);
            JsonNode node = root.get(field);
            String email = node != null && !node.isNull() ? node.asText() : "";
            return RateLimitHashing.sha256Hex(RateLimitHashing.normalizeEmail(email));
        } catch (Exception e) {
            return RateLimitHashing.sha256Hex("");
        }
    }

    private String refreshCookieHash(HttpServletRequest request) {
        String name = refreshTokenProperties.getCookieName();
        if (name == null || name.isBlank()) {
            name = "rc_refresh_token";
        }
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return RateLimitHashing.sha256Hex("no-cookie");
        }
        for (jakarta.servlet.http.Cookie c : cookies) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return RateLimitHashing.sha256Hex(c.getValue());
            }
        }
        return RateLimitHashing.sha256Hex("no-cookie");
    }

    private static String tokenHashFromPublicPath(String path, Route route) {
        String[] segments = path.split("/");
        // /api/public/estimates/{token}[/decision]  or /api/public/invoices/{token}
        String token = "";
        for (int i = 0; i < segments.length; i++) {
            if ("estimates".equals(segments[i]) || "invoices".equals(segments[i])) {
                if (i + 1 < segments.length) {
                    String seg = segments[i + 1];
                    if (!seg.isEmpty() && !"decision".equals(seg)) {
                        token = seg;
                    }
                }
                break;
            }
        }
        return RateLimitHashing.sha256Hex(token);
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && uri != null && uri.startsWith(context)) {
            return uri.substring(context.length());
        }
        return uri != null ? uri : "";
    }

    private static Route matchRoute(String method, String path) {
        if (path == null) {
            return null;
        }
        if (HttpMethod.POST.matches(Objects.requireNonNull(method))) {
            if ("/api/v1/auth/login".equals(path)) {
                return Route.AUTH_LOGIN;
            }
            if ("/api/v1/auth/register".equals(path)) {
                return Route.AUTH_REGISTER;
            }
            if ("/api/v1/auth/register-with-invite".equals(path)) {
                return Route.AUTH_REGISTER_INVITE;
            }
            if ("/api/v1/auth/refresh".equals(path)) {
                return Route.AUTH_REFRESH;
            }
            if (path.startsWith("/api/public/estimates/") && path.endsWith("/decision")) {
                return Route.PUBLIC_ESTIMATE_DECISION_POST;
            }
        }
        if (HttpMethod.GET.matches(method)) {
            if (path.startsWith("/api/public/estimates/") || path.startsWith("/api/public/invoices/")) {
                return Route.PUBLIC_RESOURCE_GET;
            }
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("utf-8");
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                429,
                "Too Many Requests",
                SAFE_LIMIT_MESSAGE,
                request.getRequestURI());
        JSON.writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }

    private enum Route {
        AUTH_LOGIN,
        AUTH_REGISTER,
        AUTH_REGISTER_INVITE,
        AUTH_REFRESH,
        PUBLIC_RESOURCE_GET,
        PUBLIC_ESTIMATE_DECISION_POST;

        boolean needsJsonBody() {
            return this == AUTH_LOGIN || this == AUTH_REGISTER || this == AUTH_REGISTER_INVITE;
        }
    }
}
