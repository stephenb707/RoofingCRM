package com.roofingcrm.api.v1.customer;

import com.roofingcrm.api.v1.common.AddressDto;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CustomerDto {

    private UUID id;

    private String firstName;
    private String lastName;
    private String primaryPhone;
    private String email;

    private AddressDto billingAddress;

    private String notes;

    private Instant createdAt;
    private Instant updatedAt;
}
