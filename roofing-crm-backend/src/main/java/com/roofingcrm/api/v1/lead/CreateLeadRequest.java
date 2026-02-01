package com.roofingcrm.api.v1.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.LeadSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateLeadRequest {

    // Option 1: link to existing customer
    private UUID customerId;

    // Option 2: create a new customer inline
    @Valid
    private NewLeadCustomerRequest newCustomer;

    private LeadSource source;

    private String leadNotes;

    @Valid
    @NotNull
    private AddressDto propertyAddress;
}
