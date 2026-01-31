package com.roofingcrm.service.activity;

import com.roofingcrm.api.v1.activity.ActivityEventDto;
import com.roofingcrm.domain.entity.ActivityEvent;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.repository.ActivityEventRepository;
import com.roofingcrm.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActivityEventServiceImpl implements ActivityEventService {

    private final ActivityEventRepository activityEventRepository;
    private final UserRepository userRepository;

    @Autowired
    public ActivityEventServiceImpl(ActivityEventRepository activityEventRepository,
                                    UserRepository userRepository) {
        this.activityEventRepository = activityEventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ActivityEvent recordEventWithActor(Tenant tenant, User actor, ActivityEntityType entityType, @NonNull UUID entityId,
                                             ActivityEventType type, String message, Map<String, Object> metadata) {
        ActivityEvent event = new ActivityEvent();
        event.setTenant(tenant);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setEventType(type);
        event.setMessage(message != null ? message.trim() : "");
        event.setMetadata(metadata != null ? metadata : Collections.emptyMap());
        event.setCreatedBy(actor);
        return activityEventRepository.save(event);
    }

    @Override
    @Transactional
    public void recordEvent(Tenant tenant, UUID userId, ActivityEntityType entityType, @NonNull UUID entityId,
                            ActivityEventType type, String message, Map<String, Object> metadata) {
        User user = userRepository.findById(Objects.requireNonNull(userId)).orElse(null);
        recordEventWithActor(tenant, user, entityType, entityId, type, message, metadata);
    }

    @Override
    @Transactional
    public ActivityEventDto createNote(Tenant tenant, UUID userId, ActivityEntityType entityType, @NonNull UUID entityId,
                                       String body) {
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new com.roofingcrm.service.exception.ResourceNotFoundException("User not found"));
        String trimmed = body != null ? body.trim() : "";
        ActivityEvent event = recordEventWithActor(tenant, user, entityType, entityId, ActivityEventType.NOTE, trimmed,
                Collections.emptyMap());
        return toDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityEventDto> listEvents(Tenant tenant, ActivityEntityType entityType, @NonNull UUID entityId,
                                             Pageable pageable) {
        return activityEventRepository
                .findByTenantAndEntityTypeAndEntityIdAndArchivedFalseOrderByCreatedAtDesc(
                        tenant, entityType, entityId, pageable)
                .map(this::toDto);
    }

    private ActivityEventDto toDto(ActivityEvent e) {
        ActivityEventDto dto = new ActivityEventDto();
        dto.setActivityId(e.getId());
        dto.setEntityType(e.getEntityType());
        dto.setEntityId(e.getEntityId());
        dto.setEventType(e.getEventType());
        dto.setMessage(e.getMessage());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setMetadata(e.getMetadata());
        if (e.getCreatedBy() != null) {
            dto.setCreatedByUserId(e.getCreatedBy().getId());
            dto.setCreatedByName(formatFullName(e.getCreatedBy().getFullName()));
        }
        return dto;
    }

    private static String formatFullName(String fullName) {
        return (fullName != null && !fullName.isBlank()) ? fullName.trim() : null;
    }
}
