package com.roofingcrm.security.ratelimit;

import com.roofingcrm.security.RefreshTokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class HighRiskEndpointRateLimitFilterClientIpTest {

    @Test
    void emptyTrustedProxiesIgnoresForwardedFor() {
        HighRiskEndpointRateLimitFilter filter = filterWithTrustedProxies(List.of());
        MockHttpServletRequest request = request("203.0.113.10", "198.51.100.25");

        assertThat(filter.clientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void trustedPeerUsesRightMostNonProxyForwardedAddress() {
        HighRiskEndpointRateLimitFilter filter =
                filterWithTrustedProxies(List.of("10.0.0.0/8", "192.0.2.10"));
        MockHttpServletRequest request =
                request("10.1.2.3", "198.51.100.25, 192.0.2.10");

        assertThat(filter.clientIp(request)).isEqualTo("198.51.100.25");
    }

    @Test
    void untrustedPeerIgnoresForwardedFor() {
        HighRiskEndpointRateLimitFilter filter =
                filterWithTrustedProxies(List.of("10.0.0.0/8"));
        MockHttpServletRequest request =
                request("203.0.113.10", "198.51.100.25");

        assertThat(filter.clientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void allProxyForwardedAddressesFallBackToDirectPeer() {
        HighRiskEndpointRateLimitFilter filter =
                filterWithTrustedProxies(List.of("10.0.0.0/8", "192.0.2.0/24"));
        MockHttpServletRequest request =
                request("10.1.2.3", "10.2.3.4, 192.0.2.10");

        assertThat(filter.clientIp(request)).isEqualTo("10.1.2.3");
    }

    private static HighRiskEndpointRateLimitFilter filterWithTrustedProxies(List<String> trustedProxies) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustedProxies(trustedProxies);
        return new HighRiskEndpointRateLimitFilter(
                properties,
                new MinuteWindowRateLimiter(),
                new RefreshTokenProperties());
    }

    private static MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
