package com.roofingcrm.api.v1.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.LeadSource;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLeadRequest {

    private LeadSource source;

    private String leadNotes;

    @Valid
    private AddressDto propertyAddress;

    private String preferredContactMethod;
}
