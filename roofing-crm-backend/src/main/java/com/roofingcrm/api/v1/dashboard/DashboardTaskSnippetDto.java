package com.roofingcrm.api.v1.dashboard;

import com.roofingcrm.domain.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class DashboardTaskSnippetDto {

    private UUID taskId;

    private String title;

    private TaskStatus status;

    private Instant dueAt;

    private UUID leadId;

    private UUID jobId;

    private UUID customerId;
}
