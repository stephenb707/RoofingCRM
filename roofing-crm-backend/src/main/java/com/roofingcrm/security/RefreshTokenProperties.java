package com.roofingcrm.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.refresh-token")
public class RefreshTokenProperties {

    private String cookieName = "rc_refresh_token";
    private long expirationDays = 14;
    private boolean secureCookie = false;
    private String sameSite = "Lax";
    private String cookiePath = "/api/v1/auth";

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public long getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(long expirationDays) {
        this.expirationDays = expirationDays;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }

    public void setSecureCookie(boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public String getSameSite() {
        return sameSite;
    }

    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    public String getCookiePath() {
        return cookiePath;
    }

    public void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }
}
