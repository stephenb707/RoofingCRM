package com.roofingcrm.realtime;

import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class TenantSubscriptionAuthorizationInterceptorTest {

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private TenantUserMembership membership;

    @Mock
    private MessageChannel channel;

    private TenantSubscriptionAuthorizationInterceptor interceptor;
    private UUID userId;

    @BeforeEach
    void setUp() {
        interceptor = new TenantSubscriptionAuthorizationInterceptor(tenantAccessService);
        userId = UUID.randomUUID();
    }

    @Test
    void subscribe_memberOfTenant_isAllowed() {
        UUID tenantId = UUID.randomUUID();
        Message<?> message = subscription(
                "/topic/tenants/" + tenantId + "/activity/JOB/" + UUID.randomUUID(),
                authenticatedSession());
        when(tenantAccessService.loadMembershipForUserOrThrow(tenantId, userId))
                .thenReturn(membership);

        assertSame(message, interceptor.preSend(message, channel));

        verify(tenantAccessService).loadMembershipForUserOrThrow(tenantId, userId);
    }

    @Test
    void subscribe_nonMemberOfTenant_isRejected() {
        UUID tenantId = UUID.randomUUID();
        Message<?> message = subscription(
                "/topic/tenants/" + tenantId + "/activity/JOB/" + UUID.randomUUID(),
                authenticatedSession());
        when(tenantAccessService.loadMembershipForUserOrThrow(tenantId, userId))
                .thenThrow(new TenantAccessDeniedException("Tenant access denied"));

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void subscribe_malformedTenantId_isRejected() {
        Message<?> message = subscription(
                "/topic/tenants/not-a-uuid/activity/JOB/" + UUID.randomUUID(),
                authenticatedSession());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
        verifyNoInteractions(tenantAccessService);
    }

    @Test
    void subscribe_missingTenantSegment_isRejected() {
        Message<?> message = subscription(
                "/topic/tenants//activity/JOB/" + UUID.randomUUID(),
                authenticatedSession());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
        verifyNoInteractions(tenantAccessService);
    }

    @Test
    void subscribe_missingAuthenticatedUser_isRejected() {
        UUID tenantId = UUID.randomUUID();
        Message<?> message = subscription(
                "/topic/tenants/" + tenantId + "/activity/JOB/" + UUID.randomUUID(),
                Map.of());

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
        verifyNoInteractions(tenantAccessService);
    }

    private Map<String, Object> authenticatedSession() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(
                JwtHandshakeInterceptor.AUTH_USER_ATTRIBUTE,
                new AuthenticatedUser(userId, "member@example.com"));
        return attributes;
    }

    private static Message<?> subscription(String destination, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId("test-session");
        accessor.setSessionAttributes(sessionAttributes);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
