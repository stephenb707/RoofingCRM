package com.roofingcrm.domain.entity;

import com.roofingcrm.background.BackgroundJobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "integration_background_jobs",
        indexes = {
                @Index(name = "idx_integration_bg_jobs_tenant", columnList = "tenant_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class IntegrationBackgroundJob {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 128)
    private String jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BackgroundJobStatus status = BackgroundJobStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payloadJson;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private int maxAttempts = 5;

    @Column(nullable = false)
    private Instant nextRunAt;

    @Column(columnDefinition = "text")
    private String lastError;

    private Instant lockedAt;

    @Column(length = 128)
    private String lockedBy;

    @Column(length = 64)
    private String correlationId;

    @Column(length = 64)
    private String entityType;

    private UUID entityId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (nextRunAt == null) {
            nextRunAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
