package com.roofingcrm.api.v1.task;

import com.roofingcrm.domain.enums.TaskPriority;
import com.roofingcrm.domain.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class TaskDto {

    private UUID taskId;

    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Instant dueAt;

    private Instant completedAt;

    private UUID assignedToUserId;

    private String assignedToName;

    private UUID leadId;

    private UUID jobId;

    private UUID customerId;

    private Instant createdAt;

    private Instant updatedAt;
}
