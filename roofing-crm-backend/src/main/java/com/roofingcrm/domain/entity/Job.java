package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.value.Address;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents a roofing job within a tenant.
 * Jobs are the main work items - either repairs, replacements, or inspections.
 */
@Entity
@Table(name = "jobs",
        indexes = {
                @Index(name = "idx_job_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_job_tenant_customer", columnList = "tenant_id, customer_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class Job extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private JobType jobType;

    private String roofType;

    private LocalDate scheduledStartDate;

    private LocalDate scheduledEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    private String assignedCrew;

    @Column(columnDefinition = "text")
    private String jobNotes;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1", column = @Column(name = "job_property_address_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "job_property_address_line2")),
            @AttributeOverride(name = "city", column = @Column(name = "job_property_city")),
            @AttributeOverride(name = "state", column = @Column(name = "job_property_state")),
            @AttributeOverride(name = "zip", column = @Column(name = "job_property_zip")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "job_property_country_code"))
    })
    private Address propertyAddress;
}
