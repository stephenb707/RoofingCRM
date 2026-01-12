package com.roofingcrm.domain.value;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object representing a physical address.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String line1;

    private String line2;

    private String city;

    private String state;

    private String zip;

    // ISO 3166-1 alpha-2 country code, e.g. "US", "CA", "GB"
    private String countryCode;
}
