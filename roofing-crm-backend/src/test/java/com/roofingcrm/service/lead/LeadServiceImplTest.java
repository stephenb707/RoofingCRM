package com.roofingcrm.service.lead;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.NewLeadCustomerRequest;
import com.roofingcrm.api.v1.lead.UpdateLeadStatusRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.LeadRepository;
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

class LeadServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private LeadService leadService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @NonNull
    private UUID tenantId = Objects.requireNonNull(UUID.randomUUID());
    private UUID userId;

    @BeforeEach
    void setUp() {
        leadRepository.deleteAll();
        customerRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = new Tenant();
        tenant.setName("Test Roofing");
        tenant.setSlug("test-roofing");
        tenant = tenantRepository.save(tenant);

        this.tenantId = Objects.requireNonNull(tenant.getId());
        this.userId = UUID.randomUUID();
    }

    private AddressDto createPropertyAddress() {
        AddressDto address = new AddressDto();
        address.setLine1("456 Roof St");
        address.setCity("Chicago");
        address.setState("IL");
        address.setZip("60602");
        address.setCountryCode("US");
        return address;
    }

    @Test
    void createLead_withNewCustomer_createsLeadAndCustomer() {
        NewLeadCustomerRequest newCustomer = new NewLeadCustomerRequest();
        newCustomer.setFirstName("Alice");
        newCustomer.setLastName("Roofer");
        newCustomer.setPrimaryPhone("555-0000");
        newCustomer.setEmail("alice@example.com");

        CreateLeadRequest request = new CreateLeadRequest();
        request.setNewCustomer(newCustomer);
        request.setPropertyAddress(createPropertyAddress());

        LeadDto dto = leadService.createLead(tenantId, userId, request);

        assertNotNull(dto.getId());
        assertNotNull(dto.getCustomerId());
        assertEquals(LeadStatus.NEW, dto.getStatus());
    }

    @Test
    void listLeads_withStatusFilter_returnsOnlyMatching() {
        // Create two leads: NEW and WON
        CreateLeadRequest request1 = new CreateLeadRequest();
        NewLeadCustomerRequest c1 = new NewLeadCustomerRequest();
        c1.setFirstName("Lead1");
        c1.setLastName("New");
        c1.setPrimaryPhone("111");
        request1.setNewCustomer(c1);
        request1.setPropertyAddress(createPropertyAddress());
        LeadDto lead1 = leadService.createLead(tenantId, userId, request1);

        CreateLeadRequest request2 = new CreateLeadRequest();
        NewLeadCustomerRequest c2 = new NewLeadCustomerRequest();
        c2.setFirstName("Lead2");
        c2.setLastName("Won");
        c2.setPrimaryPhone("222");
        request2.setNewCustomer(c2);
        request2.setPropertyAddress(createPropertyAddress());
        LeadDto lead2 = leadService.createLead(tenantId, userId, request2);

        // Update second lead to WON
        leadService.updateLeadStatus(tenantId, userId, lead2.getId(), LeadStatus.WON);

        Page<LeadDto> newLeads = leadService.listLeads(tenantId, LeadStatus.NEW, PageRequest.of(0, 10));
        Page<LeadDto> wonLeads = leadService.listLeads(tenantId, LeadStatus.WON, PageRequest.of(0, 10));

        assertEquals(1, newLeads.getTotalElements());
        assertEquals(lead1.getId(), newLeads.getContent().get(0).getId());

        assertEquals(1, wonLeads.getTotalElements());
        assertEquals(lead2.getId(), wonLeads.getContent().get(0).getId());
    }

    @Test
    void getLead_nonExisting_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> leadService.getLead(tenantId, randomId));
    }

    @Test
    void updateLeadStatus_changesStatus() {
        CreateLeadRequest request = new CreateLeadRequest();
        NewLeadCustomerRequest c = new NewLeadCustomerRequest();
        c.setFirstName("Status");
        c.setLastName("Test");
        c.setPrimaryPhone("123");
        request.setNewCustomer(c);
        request.setPropertyAddress(createPropertyAddress());

        LeadDto lead = leadService.createLead(tenantId, userId, request);

        UpdateLeadStatusRequest statusRequest = new UpdateLeadStatusRequest();
        statusRequest.setStatus(LeadStatus.LOST);

        LeadDto updated = leadService.updateLeadStatus(tenantId, userId, lead.getId(), statusRequest.getStatus());

        assertEquals(LeadStatus.LOST, updated.getStatus());
    }
}
