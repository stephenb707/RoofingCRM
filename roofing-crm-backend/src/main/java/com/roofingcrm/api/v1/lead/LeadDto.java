package com.roofingcrm.api.v1.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
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

    private LeadStatus status;

    private LeadSource source;

    private String leadNotes;

    private AddressDto propertyAddress;

    private String preferredContactMethod;

    private Instant createdAt;
    private Instant updatedAt;
}
