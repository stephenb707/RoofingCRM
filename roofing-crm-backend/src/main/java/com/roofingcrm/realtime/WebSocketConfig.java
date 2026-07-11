package com.roofingcrm.realtime;

import com.roofingcrm.config.CorsProperties;
import com.roofingcrm.security.JwtService;
import com.roofingcrm.service.tenant.TenantAccessService;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time message broker configuration.
 *
 * <p>Auth model:
 * <ul>
 *   <li>Handshake JWT is validated by {@link JwtHandshakeInterceptor}; the access token is
 *       passed as a query param because browsers cannot set custom headers on WS upgrade.</li>
 *   <li>Reverse proxies in front of this app must redact {@code token} from access logs.
 *       See {@link WebSocketUrlRedactor} for our in-app redaction helper.</li>
 *   <li>The JWT is only re-checked on (re)handshake. An open connection survives until the
 *       client reconnects or the connection drops; short access-token lifetime keeps that
 *       window small and the frontend already halts reconnects when refresh fails.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final TenantSubscriptionAuthorizationInterceptor tenantSubscriptionAuthorizationInterceptor;
    private final CorsProperties corsProperties;

    @Autowired
    public WebSocketConfig(JwtService jwtService,
                           TenantAccessService tenantAccessService,
                           CorsProperties corsProperties) {
        this.jwtHandshakeInterceptor = new JwtHandshakeInterceptor(jwtService);
        this.tenantSubscriptionAuthorizationInterceptor =
                new TenantSubscriptionAuthorizationInterceptor(tenantAccessService);
        this.corsProperties = corsProperties;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(Objects.requireNonNull(heartbeatScheduler()));
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(tenantSubscriptionAuthorizationInterceptor);
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(Objects.requireNonNull(
                        corsProperties.getAllowedOrigins().toArray(String[]::new)))
                .addInterceptors(jwtHandshakeInterceptor)
                .withSockJS();
    }

    private static ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(1);
        s.setThreadNamePrefix("ws-heartbeat-");
        s.initialize();
        return s;
    }
}
