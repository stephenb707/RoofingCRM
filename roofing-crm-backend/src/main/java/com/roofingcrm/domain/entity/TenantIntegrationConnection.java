package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.IntegrationConnectionStatus;
import com.roofingcrm.domain.enums.IntegrationProvider;
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
@Table(name = "tenant_integration_connections",
        uniqueConstraints = @UniqueConstraint(name = "uq_tenant_integration_provider", columnNames = {"tenant_id", "provider"}),
        indexes = @Index(name = "idx_tenant_integration_connections_tenant", columnList = "tenant_id"))
@Getter
@Setter
@NoArgsConstructor
public class TenantIntegrationConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private IntegrationProvider provider;

    @Column(nullable = false)
    private boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IntegrationConnectionStatus status = IntegrationConnectionStatus.NOT_CONFIGURED;

    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> configJson;

    @Column(columnDefinition = "text")
    private String encryptedSecretJson;

    private Instant lastConnectedAt;

    @Column(columnDefinition = "text")
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
