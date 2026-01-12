package com.roofingcrm.service.customer;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.customer.CreateCustomerRequest;
import com.roofingcrm.api.v1.customer.CustomerDto;
import com.roofingcrm.api.v1.customer.UpdateCustomerRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @NonNull
    private UUID tenantId = Objects.requireNonNull(UUID.randomUUID());
    private UUID userId;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = new Tenant();
        tenant.setName("Test Roofing");
        tenant.setSlug("test-roofing");
        tenant = tenantRepository.save(tenant);

        this.tenantId = Objects.requireNonNull(tenant.getId());
        this.userId = UUID.randomUUID();
    }

    @Test
    void createCustomer_persistsAndReturnsDto() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPrimaryPhone("555-1234");
        request.setEmail("john@example.com");

        AddressDto address = new AddressDto();
        address.setLine1("123 Main St");
        address.setCity("Chicago");
        address.setState("IL");
        address.setZip("60601");
        address.setCountryCode("US");
        request.setBillingAddress(address);
        request.setNotes("Important customer");

        CustomerDto dto = customerService.createCustomer(tenantId, userId, request);

        assertNotNull(dto.getId());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("555-1234", dto.getPrimaryPhone());
        assertEquals("john@example.com", dto.getEmail());
        assertNotNull(dto.getCreatedAt());

        Customer saved = customerRepository.findById(Objects.requireNonNull(dto.getId())).orElseThrow();
        assertEquals(tenantId, saved.getTenant().getId());
    }

    @Test
    void listCustomers_returnsOnlyTenantCustomers() {
        // Seed a second tenant + customer; ensure filtering by tenant works
        Tenant otherTenant = new Tenant();
        otherTenant.setName("Another Roofing");
        otherTenant.setSlug("another-roofing");
        otherTenant = tenantRepository.save(otherTenant);

        Customer otherCustomer = new Customer();
        otherCustomer.setTenant(otherTenant);
        otherCustomer.setFirstName("Other");
        otherCustomer.setLastName("Tenant");
        otherCustomer.setPrimaryPhone("000");
        customerRepository.save(otherCustomer);

        // Create a customer for main tenant through service
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPrimaryPhone("555-1234");
        CustomerDto dto = customerService.createCustomer(tenantId, userId, request);

        Page<CustomerDto> page = customerService.listCustomers(tenantId, PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals(dto.getId(), page.getContent().get(0).getId());
    }

    @Test
    void getCustomer_nonExisting_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> customerService.getCustomer(tenantId, randomId));
    }

    @Test
    void updateCustomer_updatesFields() {
        // Create
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        createRequest.setPrimaryPhone("555-1234");
        CustomerDto created = customerService.createCustomer(tenantId, userId, createRequest);

        // Update
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setFirstName("Jane");
        updateRequest.setLastName("Smith");
        updateRequest.setPrimaryPhone("999-9999");
        updateRequest.setEmail("jane@example.com");

        CustomerDto updated = customerService.updateCustomer(tenantId, userId, created.getId(), updateRequest);

        assertEquals("Jane", updated.getFirstName());
        assertEquals("Smith", updated.getLastName());
        assertEquals("999-9999", updated.getPrimaryPhone());
        assertEquals("jane@example.com", updated.getEmail());
    }
}
