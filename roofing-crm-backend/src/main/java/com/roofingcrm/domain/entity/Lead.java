package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.value.Address;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a sales lead within a tenant.
 * Leads track potential roofing jobs from initial contact through conversion.
 */
@Entity
@Table(name = "leads",
        indexes = {
                @Index(name = "idx_lead_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_lead_tenant_source", columnList = "tenant_id, source")
        })
@Getter
@Setter
@NoArgsConstructor
public class Lead extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeadStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeadSource source;

    @Column(name = "pipeline_position", nullable = false)
    private int pipelinePosition = 0;

    @Column(columnDefinition = "text")
    private String leadNotes;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1", column = @Column(name = "property_address_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "property_address_line2")),
            @AttributeOverride(name = "city", column = @Column(name = "property_city")),
            @AttributeOverride(name = "state", column = @Column(name = "property_state")),
            @AttributeOverride(name = "zip", column = @Column(name = "property_zip")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "property_country_code"))
    })
    private Address propertyAddress;
}
