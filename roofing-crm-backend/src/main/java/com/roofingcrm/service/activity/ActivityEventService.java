package com.roofingcrm.service.activity;

import com.roofingcrm.api.v1.activity.ActivityEventDto;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.UUID;

public interface ActivityEventService {

    /** Records an event. Internal use; use the userId overload from other services. entityId must not be null. */
    com.roofingcrm.domain.entity.ActivityEvent recordEventWithActor(Tenant tenant, User actor, ActivityEntityType entityType, @NonNull UUID entityId,
                              ActivityEventType type, String message, Map<String, Object> metadata);

    /** Convenience: loads User by userId and records the event. entityId must not be null. */
    void recordEvent(Tenant tenant, UUID userId, ActivityEntityType entityType, @NonNull UUID entityId,
                     ActivityEventType type, String message, Map<String, Object> metadata);

    /** entityId must not be null. */
    ActivityEventDto createNote(Tenant tenant, UUID userId, ActivityEntityType entityType, @NonNull UUID entityId, String body);

    /** entityId must not be null. */
    Page<ActivityEventDto> listEvents(Tenant tenant, ActivityEntityType entityType, @NonNull UUID entityId, Pageable pageable);
}
