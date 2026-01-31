package com.roofingcrm.api.v1.task;

import com.roofingcrm.domain.enums.TaskStatus;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.task.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Validated
public class TaskController {

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskDto> createTask(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateTaskRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        TaskDto created = taskService.createTask(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<TaskDto>> listTasks(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "assignedToUserId", required = false) UUID assignedToUserId,
            @RequestParam(value = "leadId", required = false) UUID leadId,
            @RequestParam(value = "jobId", required = false) UUID jobId,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @RequestParam(value = "dueBefore", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueBefore,
            @RequestParam(value = "dueAfter", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dueAfter,
            @PageableDefault(size = 20) @NonNull Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<TaskDto> page = taskService.listTasks(tenantId, userId, status, assignedToUserId,
                leadId, jobId, customerId, dueBefore, dueAfter, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskDto> getTask(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("taskId") UUID taskId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        TaskDto dto = taskService.getTask(tenantId, userId, taskId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<TaskDto> updateTask(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("taskId") UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        TaskDto updated = taskService.updateTask(tenantId, userId, taskId, request);
        return ResponseEntity.ok(updated);
    }
}
