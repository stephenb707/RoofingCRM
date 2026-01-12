package com.roofingcrm.domain.entity;

import com.roofingcrm.domain.value.Address;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a customer (homeowner/property owner) within a tenant.
 */
@Entity
@Table(name = "customers",
        indexes = {
                @Index(name = "idx_customer_tenant_last_name", columnList = "tenant_id, last_name"),
                @Index(name = "idx_customer_tenant_email", columnList = "tenant_id, email")
        })
@Getter
@Setter
@NoArgsConstructor
public class Customer extends TenantAuditedEntity {

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    private String primaryPhone;

    @Email
    private String email;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "line1", column = @Column(name = "billing_address_line1")),
            @AttributeOverride(name = "line2", column = @Column(name = "billing_address_line2")),
            @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
            @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
            @AttributeOverride(name = "zip", column = @Column(name = "billing_zip")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "billing_country_code"))
    })
    private Address billingAddress;

    @Column(columnDefinition = "text")
    private String notes;
}
