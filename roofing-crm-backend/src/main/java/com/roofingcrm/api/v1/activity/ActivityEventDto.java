package com.roofingcrm.api.v1.activity;

import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ActivityEventDto {

    private UUID activityId;
    private ActivityEntityType entityType;
    private UUID entityId;
    private ActivityEventType eventType;
    private String message;
    private Instant createdAt;
    private UUID createdByUserId;
    private String createdByName;
    private Map<String, Object> metadata;
}
