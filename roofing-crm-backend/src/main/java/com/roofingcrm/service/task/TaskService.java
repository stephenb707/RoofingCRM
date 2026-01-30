package com.roofingcrm.service.task;

import com.roofingcrm.api.v1.task.CreateTaskRequest;
import com.roofingcrm.api.v1.task.TaskDto;
import com.roofingcrm.api.v1.task.UpdateTaskRequest;
import com.roofingcrm.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.UUID;

public interface TaskService {

    TaskDto createTask(@NonNull UUID tenantId, @NonNull UUID userId, CreateTaskRequest request);

    TaskDto updateTask(@NonNull UUID tenantId, @NonNull UUID userId, UUID taskId, UpdateTaskRequest request);

    TaskDto getTask(@NonNull UUID tenantId, @NonNull UUID userId, UUID taskId);

    Page<TaskDto> listTasks(@NonNull UUID tenantId, @NonNull UUID userId,
            TaskStatus status, UUID assignedToUserId, UUID leadId, UUID jobId, UUID customerId,
            Instant dueBefore, Instant dueAfter, Pageable pageable);
}
