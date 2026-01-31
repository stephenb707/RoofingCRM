package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.TaskPriority;
import com.roofingcrm.domain.enums.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a task or follow-up within a tenant.
 * Tasks can be linked to leads, jobs, and customers.
 */
@Entity
@Table(name = "tasks",
        indexes = {
                @Index(name = "idx_tasks_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_tasks_tenant_due_at", columnList = "tenant_id, due_at"),
                @Index(name = "idx_tasks_tenant_assigned_to", columnList = "tenant_id, assigned_to_user_id"),
                @Index(name = "idx_tasks_tenant_lead", columnList = "tenant_id, lead_id"),
                @Index(name = "idx_tasks_tenant_job", columnList = "tenant_id, job_id"),
                @Index(name = "idx_tasks_tenant_customer", columnList = "tenant_id, customer_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Task extends TenantAuditedEntity {

    @NotBlank
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
