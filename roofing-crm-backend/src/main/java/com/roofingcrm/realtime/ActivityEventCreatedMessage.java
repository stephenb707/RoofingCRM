package com.roofingcrm.realtime;

import com.roofingcrm.domain.enums.ActivityEntityType;

import java.util.UUID;

/**
 * Message published to WebSocket subscribers when a new ActivityEvent is created.
 * Scoped by tenant and entity so clients only receive relevant updates.
 */
public record ActivityEventCreatedMessage(
        UUID tenantId,
        ActivityEntityType entityType,
        UUID entityId,
        UUID activityEventId
) {}
