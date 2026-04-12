package com.roofingcrm.api.v1.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.LeadSource;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class LeadDto {

    private UUID id;

    private UUID customerId;

    private String customerFirstName;
    private String customerLastName;
    private String customerEmail;
    private String customerPhone;

    private UUID statusDefinitionId;
    /** Stable key: built-in enum name or {@code C_<uuid>} for custom. */
    private String statusKey;
    /** Tenant-configured display label. */
    private String statusLabel;

    private LeadSource source;

    private int pipelinePosition;

    private String leadNotes;

    private AddressDto propertyAddress;

    private Instant createdAt;
    private Instant updatedAt;

    /** Set when this lead has been converted to a job (one job per lead). */
    private UUID convertedJobId;
}
