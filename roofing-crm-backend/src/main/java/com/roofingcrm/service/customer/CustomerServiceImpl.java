package com.roofingcrm.service.customer;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.customer.CreateCustomerRequest;
import com.roofingcrm.api.v1.customer.CustomerDto;
import com.roofingcrm.api.v1.customer.UpdateCustomerRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.enums.PreferredContactMethod;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final TenantAccessService tenantAccessService;
    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerServiceImpl(TenantAccessService tenantAccessService,
                               CustomerRepository customerRepository) {
        this.tenantAccessService = tenantAccessService;
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerDto createCustomer(@NonNull UUID tenantId, @NonNull UUID userId, CreateCustomerRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setCreatedByUserId(userId);
        customer.setUpdatedByUserId(userId);

        applyCustomerData(customer, request.getFirstName(), request.getLastName(),
                request.getPrimaryPhone(), request.getEmail(),
                request.getPreferredContactMethod(),
                request.getBillingAddress(), request.getNotes());

        Customer saved = customerRepository.save(customer);
        return toDto(saved);
    }

    @Override
    public CustomerDto updateCustomer(@NonNull UUID tenantId, @NonNull UUID userId, UUID customerId, UpdateCustomerRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Customer customer = customerRepository.findByIdAndTenantAndArchivedFalse(customerId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        customer.setUpdatedByUserId(userId);

        applyCustomerData(customer, request.getFirstName(), request.getLastName(),
                request.getPrimaryPhone(), request.getEmail(),
                request.getPreferredContactMethod(),
                request.getBillingAddress(), request.getNotes());

        Customer saved = customerRepository.save(customer);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto getCustomer(@NonNull UUID tenantId, @NonNull UUID userId, UUID customerId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Customer customer = customerRepository.findByIdAndTenantAndArchivedFalse(customerId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return toDto(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDto> listCustomers(@NonNull UUID tenantId, @NonNull UUID userId, String q, @NonNull Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        
        String qNormalized = (q == null) ? null : q.trim().replaceAll("\\s+", " ");
        if (qNormalized == null || qNormalized.isBlank()) {
            return customerRepository.findByTenantAndArchivedFalse(tenant, pageable)
                    .map(this::toDto);
        }
        String digitsOnly = qNormalized.replaceAll("\\D", "");
        String digitsOnlyParam = digitsOnly.isEmpty() ? null : digitsOnly;
        String qNoSpaces = qNormalized.replace(" ", "");
        return customerRepository.search(tenant, qNormalized, digitsOnlyParam, qNoSpaces, pageable)
                .map(this::toDto);
    }

    private void applyCustomerData(Customer customer,
                                   String firstName,
                                   String lastName,
                                   String primaryPhone,
                                   String email,
                                   PreferredContactMethod preferredContactMethod,
                                   AddressDto billingAddressDto,
                                   String notes) {
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPrimaryPhone(primaryPhone);
        customer.setEmail(email);
        customer.setPreferredContactMethod(preferredContactMethod);
        customer.setNotes(notes);

        if (billingAddressDto != null) {
            Address address = customer.getBillingAddress();
            if (address == null) {
                address = new Address();
            }
            address.setLine1(billingAddressDto.getLine1());
            address.setLine2(billingAddressDto.getLine2());
            address.setCity(billingAddressDto.getCity());
            address.setState(billingAddressDto.getState());
            address.setZip(billingAddressDto.getZip());
            address.setCountryCode(billingAddressDto.getCountryCode());
            customer.setBillingAddress(address);
        } else {
            customer.setBillingAddress(null);
        }
    }

    private CustomerDto toDto(Customer entity) {
        CustomerDto dto = new CustomerDto();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setPrimaryPhone(entity.getPrimaryPhone());
        dto.setEmail(entity.getEmail());
        dto.setPreferredContactMethod(entity.getPreferredContactMethod());
        dto.setNotes(entity.getNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getBillingAddress() != null) {
            AddressDto a = new AddressDto();
            a.setLine1(entity.getBillingAddress().getLine1());
            a.setLine2(entity.getBillingAddress().getLine2());
            a.setCity(entity.getBillingAddress().getCity());
            a.setState(entity.getBillingAddress().getState());
            a.setZip(entity.getBillingAddress().getZip());
            a.setCountryCode(entity.getBillingAddress().getCountryCode());
            dto.setBillingAddress(a);
        }

        return dto;
    }
}
