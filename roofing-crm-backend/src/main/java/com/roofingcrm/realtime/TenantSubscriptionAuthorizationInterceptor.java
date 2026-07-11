package com.roofingcrm.realtime;

import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Authorizes tenant-scoped STOMP subscriptions against active tenant membership.
 */
public class TenantSubscriptionAuthorizationInterceptor implements ChannelInterceptor {

    private static final String TENANT_TOPIC_PREFIX = "/topic/tenants/";

    private final TenantAccessService tenantAccessService;

    public TenantSubscriptionAuthorizationInterceptor(TenantAccessService tenantAccessService) {
        this.tenantAccessService = tenantAccessService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull org.springframework.messaging.MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(TENANT_TOPIC_PREFIX)) {
            return message;
        }

        UUID tenantId = parseTenantId(destination);
        AuthenticatedUser authUser = resolveAuthenticatedUser(accessor.getSessionAttributes());

        try {
            tenantAccessService.loadMembershipForUserOrThrow(
                    Objects.requireNonNull(tenantId),
                    Objects.requireNonNull(authUser.userId()));
        } catch (RuntimeException ex) {
            throw new MessagingException("Tenant subscription denied", ex);
        }
        return message;
    }

    private static UUID parseTenantId(String destination) {
        String remainder = destination.substring(TENANT_TOPIC_PREFIX.length());
        int slash = remainder.indexOf('/');
        String tenantSegment = slash >= 0 ? remainder.substring(0, slash) : remainder;
        if (tenantSegment.isBlank()) {
            throw new MessagingException("Tenant subscription destination is missing a tenant id");
        }
        try {
            return UUID.fromString(tenantSegment);
        } catch (IllegalArgumentException ex) {
            throw new MessagingException("Tenant subscription destination has an invalid tenant id", ex);
        }
    }

    private static AuthenticatedUser resolveAuthenticatedUser(Map<String, Object> sessionAttributes) {
        if (sessionAttributes == null) {
            throw new MessagingException("Tenant subscription has no authenticated session");
        }
        Object principal = sessionAttributes.get(JwtHandshakeInterceptor.AUTH_USER_ATTRIBUTE);
        if (!(principal instanceof AuthenticatedUser authUser)) {
            throw new MessagingException("Tenant subscription has no authenticated user");
        }
        return authUser;
    }
}
