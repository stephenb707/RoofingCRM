package com.roofingcrm.service.customer;

import com.roofingcrm.api.v1.customer.CreateCustomerRequest;
import com.roofingcrm.api.v1.customer.CustomerDto;
import com.roofingcrm.api.v1.customer.UpdateCustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface CustomerService {

    CustomerDto createCustomer(@NonNull UUID tenantId, @NonNull UUID userId, CreateCustomerRequest request);

    CustomerDto updateCustomer(@NonNull UUID tenantId, @NonNull UUID userId, UUID customerId, UpdateCustomerRequest request);

    CustomerDto getCustomer(@NonNull UUID tenantId, @NonNull UUID userId, UUID customerId);

    Page<CustomerDto> listCustomers(@NonNull UUID tenantId, @NonNull UUID userId, String q, Pageable pageable);
}
