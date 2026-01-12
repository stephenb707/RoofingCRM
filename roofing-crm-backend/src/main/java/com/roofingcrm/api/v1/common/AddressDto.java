package com.roofingcrm.api.v1.common;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressDto {

    @Size(max = 255)
    private String line1;

    @Size(max = 255)
    private String line2;

    @Size(max = 255)
    private String city;

    @Size(max = 255)
    private String state;

    @Size(max = 20)
    private String zip;

    @Size(max = 2)
    private String countryCode; // ISO 3166-1 alpha-2
}
