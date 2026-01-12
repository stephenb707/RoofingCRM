package com.roofingcrm.service.customer;

import com.roofingcrm.api.v1.customer.CreateCustomerRequest;
import com.roofingcrm.api.v1.customer.CustomerDto;
import com.roofingcrm.api.v1.customer.UpdateCustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {

    CustomerDto createCustomer(UUID tenantId, UUID userId, CreateCustomerRequest request);

    CustomerDto updateCustomer(UUID tenantId, UUID userId, UUID customerId, UpdateCustomerRequest request);

    CustomerDto getCustomer(UUID tenantId, UUID customerId);

    Page<CustomerDto> listCustomers(UUID tenantId, Pageable pageable);
}
