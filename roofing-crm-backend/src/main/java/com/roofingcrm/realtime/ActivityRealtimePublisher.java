package com.roofingcrm.realtime;

import com.roofingcrm.domain.enums.ActivityEntityType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Publishes activity event notifications to WebSocket subscribers.
 * Topic: /topic/tenants/{tenantId}/activity/{entityType}/{entityId}
 */
@Service
public class ActivityRealtimePublisher {

    private static final String TOPIC_PREFIX = "/topic/tenants/";

    private final SimpMessagingTemplate messagingTemplate;

    public ActivityRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast that a new ActivityEvent was created for the given entity.
     * Clients subscribed to this topic can invalidate/refetch their activity list.
     */
    public void publish(UUID tenantId, ActivityEntityType entityType, UUID entityId, UUID activityEventId) {
        String destination = TOPIC_PREFIX + tenantId + "/activity/" + entityType.name() + "/" + entityId;
        ActivityEventCreatedMessage message = new ActivityEventCreatedMessage(
                tenantId, entityType, entityId, activityEventId);
        messagingTemplate.convertAndSend(destination, message);
    }
}
