package com.roofingcrm.realtime;

import com.roofingcrm.domain.enums.ActivityEntityType;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Application event fired when an ActivityEvent is persisted.
 * Published after transaction commit to avoid clients seeing events that roll back.
 */
public class ActivityEventCreatedAppEvent extends ApplicationEvent {

    private final UUID tenantId;
    private final ActivityEntityType entityType;
    private final UUID entityId;
    private final UUID activityEventId;

    public ActivityEventCreatedAppEvent(Object source, UUID tenantId, ActivityEntityType entityType,
                                       UUID entityId, UUID activityEventId) {
        super(source);
        this.tenantId = tenantId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.activityEventId = activityEventId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public ActivityEntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getActivityEventId() {
        return activityEventId;
    }
}
