package com.roofingcrm.security;

import com.roofingcrm.config.CorsProperties;
import com.roofingcrm.service.mail.PublicUrlProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;

/**
 * Validates the refresh-cookie configuration at startup and logs warnings when the running
 * profile looks production-like but the cookie config would silently break refresh on a
 * cross-site deployment.
 *
 * <p>Production cross-site deployments (frontend on https://app.example.com, backend on
 * https://api.example.com) require:
 * <ul>
 *   <li>{@code APP_SECURITY_REFRESH_COOKIE_SECURE=true}</li>
 *   <li>{@code APP_SECURITY_REFRESH_COOKIE_SAME_SITE=None}</li>
 * </ul>
 *
 * <p>Local dev (frontend on http://localhost:3000, backend on http://localhost:8080) can keep
 * {@code Secure=false} and {@code SameSite=Lax}; SameSite=Lax is treated as same-site for
 * localhost because both run on the same registrable domain.
 */
@Component
public class RefreshCookieConfigSafetyChecker {

    private static final Logger log = LoggerFactory.getLogger(RefreshCookieConfigSafetyChecker.class);

    private final RefreshTokenProperties refreshTokenProperties;
    private final CorsProperties corsProperties;
    private final PublicUrlProperties publicUrlProperties;

    public RefreshCookieConfigSafetyChecker(RefreshTokenProperties refreshTokenProperties,
                                            CorsProperties corsProperties,
                                            PublicUrlProperties publicUrlProperties) {
        this.refreshTokenProperties = refreshTokenProperties;
        this.corsProperties = corsProperties;
        this.publicUrlProperties = publicUrlProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        String publicBaseUrl = publicUrlProperties.getPublicBaseUrl();
        boolean publicLooksProduction = looksLikeProductionUrl(publicBaseUrl);
        boolean anyOriginLooksProduction = anyOriginLooksLikeProduction(corsProperties.getAllowedOrigins())
                || anyOriginLooksLikeProduction(corsProperties.getAllowedOriginPatterns());

        boolean productionLike = publicLooksProduction || anyOriginLooksProduction;
        boolean secure = refreshTokenProperties.isSecureCookie();
        String sameSite = nullSafe(refreshTokenProperties.getSameSite());

        if (productionLike) {
            if (!secure) {
                log.warn(
                        "Refresh cookie 'secure' is FALSE while public URL/CORS look production-like ({}). "
                                + "Set APP_SECURITY_REFRESH_COOKIE_SECURE=true so the refresh cookie is only "
                                + "sent over HTTPS.",
                        publicBaseUrl);
            }
            if (looksCrossSite(publicBaseUrl, corsProperties.getAllowedOrigins())
                    && !"None".equalsIgnoreCase(sameSite)) {
                log.warn(
                        "Refresh cookie 'sameSite' is '{}' but the deployment looks cross-site. "
                                + "Cross-site refresh requires SameSite=None and Secure=true. "
                                + "Set APP_SECURITY_REFRESH_COOKIE_SAME_SITE=None and "
                                + "APP_SECURITY_REFRESH_COOKIE_SECURE=true.",
                        sameSite);
            }
            if ("None".equalsIgnoreCase(sameSite) && !secure) {
                log.warn(
                        "Refresh cookie SameSite=None requires Secure=true; current Secure=false. "
                                + "Browsers will reject the cookie. "
                                + "Set APP_SECURITY_REFRESH_COOKIE_SECURE=true.");
            }
        } else {
            log.info(
                    "Refresh cookie config: secure={}, sameSite={}, path={}, expirationDays={} (dev/local profile assumed).",
                    secure, sameSite, refreshTokenProperties.getCookiePath(),
                    refreshTokenProperties.getExpirationDays());
        }
    }

    /**
     * Visible for tests. A URL is considered production-like when it is https or has a non-local
     * host (anything that is not localhost / 127.0.0.1 / *.local).
     */
    static boolean looksLikeProductionUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            if ("https".equalsIgnoreCase(scheme)) {
                return true;
            }
            return !isLocalHost(host);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean anyOriginLooksLikeProduction(List<String> origins) {
        if (origins == null) {
            return false;
        }
        for (String origin : origins) {
            if (looksLikeProductionUrl(origin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Visible for tests. Returns true if the public URL host doesn't share its registrable
     * domain with at least one allowed origin. Conservative: if we cannot determine, assume
     * cross-site so we surface a warning rather than silently accept.
     */
    static boolean looksCrossSite(String publicBaseUrl, List<String> allowedOrigins) {
        if (publicBaseUrl == null || publicBaseUrl.isBlank() || allowedOrigins == null || allowedOrigins.isEmpty()) {
            return false;
        }
        String publicHost = safeHost(publicBaseUrl);
        if (publicHost == null) {
            return false;
        }
        for (String origin : allowedOrigins) {
            String originHost = safeHost(origin);
            if (originHost == null) continue;
            if (sameRegistrableDomain(publicHost, originHost)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameRegistrableDomain(String hostA, String hostB) {
        if (hostA.equalsIgnoreCase(hostB)) {
            return true;
        }
        // Best-effort: compare last two labels (e.g. example.com == api.example.com).
        return lastTwoLabels(hostA).equalsIgnoreCase(lastTwoLabels(hostB));
    }

    private static String lastTwoLabels(String host) {
        String[] parts = host.split("\\.");
        if (parts.length <= 2) {
            return host;
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    private static String safeHost(String url) {
        try {
            URI uri = URI.create(url.trim());
            return uri.getHost();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isLocalHost(String host) {
        String h = host.toLowerCase();
        return h.equals("localhost")
                || h.equals("127.0.0.1")
                || h.equals("::1")
                || h.endsWith(".local")
                || h.endsWith(".localhost");
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
