package com.roofingcrm.api.v1.task;

import com.roofingcrm.domain.enums.TaskPriority;
import com.roofingcrm.domain.enums.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class UpdateTaskRequest {

    @Size(max = 255)
    private String title;

    private String description;

    private TaskStatus status;

    private TaskPriority priority;

    private Instant dueAt;

    private UUID assignedToUserId;

    private UUID leadId;

    private UUID jobId;

    private UUID customerId;
}
