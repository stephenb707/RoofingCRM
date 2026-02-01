package com.roofingcrm.api.v1.customer;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.domain.enums.PreferredContactMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCustomerRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String primaryPhone;

    @Email
    private String email;

    private PreferredContactMethod preferredContactMethod;

    @Valid
    private AddressDto billingAddress;

    private String notes;
}
