package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.PipelineType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tenant-owned label and ordering for a lead or job pipeline column.
 * {@code systemKey} is stable ({@code NEW}, {@code SCHEDULED}, or {@code C_<uuid>} for custom).
 */
@Entity
@Table(name = "pipeline_status_definitions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_pipeline_status_def_tenant_type_key",
                columnNames = {"tenant_id", "pipeline_type", "system_key"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class PipelineStatusDefinition extends TenantAuditedEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "pipeline_type", nullable = false, length = 10)
    private PipelineType pipelineType;

    @NotBlank
    @Column(name = "system_key", nullable = false, length = 64)
    private String systemKey;

    @NotBlank
    @Column(nullable = false)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "built_in", nullable = false)
    private boolean builtIn;

    @Column(nullable = false)
    private boolean active = true;
}
