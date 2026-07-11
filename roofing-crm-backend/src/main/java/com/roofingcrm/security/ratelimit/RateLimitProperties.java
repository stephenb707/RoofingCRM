package com.roofingcrm.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory per-window limits for high-risk endpoints. Values are requests allowed per rolling minute window,
 * per composite key (see {@link HighRiskEndpointRateLimitFilter}).
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    /**
     * Exact IP addresses or CIDR ranges for proxies that are allowed to supply
     * {@code X-Forwarded-For}. Empty by default so forwarded headers are ignored.
     */
    private List<String> trustedProxies = new ArrayList<>();
    /** Max POST /auth/login per IP+email per minute. */
    private int loginPerMinute = 20;
    /** Max POST /auth/register per IP+email per minute. */
    private int registerPerMinute = 10;
    /** Max POST /auth/register-with-invite per IP+email per minute. */
    private int registerInvitePerMinute = 10;
    /** Max POST /auth/refresh per IP+refresh-suffix per minute (suffix is a hash of the cookie when present). */
    private int refreshPerMinute = 90;
    /** Max GET /api/public/estimates|invoices/{token} per IP+token-hash per minute. */
    private int publicResourceGetPerMinute = 90;
    /** Max POST public estimate decision per IP+token-hash per minute. */
    private int publicEstimateDecisionPostPerMinute = 25;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies != null ? trustedProxies : new ArrayList<>();
    }

    public int getLoginPerMinute() {
        return loginPerMinute;
    }

    public void setLoginPerMinute(int loginPerMinute) {
        this.loginPerMinute = loginPerMinute;
    }

    public int getRegisterPerMinute() {
        return registerPerMinute;
    }

    public void setRegisterPerMinute(int registerPerMinute) {
        this.registerPerMinute = registerPerMinute;
    }

    public int getRegisterInvitePerMinute() {
        return registerInvitePerMinute;
    }

    public void setRegisterInvitePerMinute(int registerInvitePerMinute) {
        this.registerInvitePerMinute = registerInvitePerMinute;
    }

    public int getRefreshPerMinute() {
        return refreshPerMinute;
    }

    public void setRefreshPerMinute(int refreshPerMinute) {
        this.refreshPerMinute = refreshPerMinute;
    }

    public int getPublicResourceGetPerMinute() {
        return publicResourceGetPerMinute;
    }

    public void setPublicResourceGetPerMinute(int publicResourceGetPerMinute) {
        this.publicResourceGetPerMinute = publicResourceGetPerMinute;
    }

    public int getPublicEstimateDecisionPostPerMinute() {
        return publicEstimateDecisionPostPerMinute;
    }

    public void setPublicEstimateDecisionPostPerMinute(int publicEstimateDecisionPostPerMinute) {
        this.publicEstimateDecisionPostPerMinute = publicEstimateDecisionPostPerMinute;
    }
}
