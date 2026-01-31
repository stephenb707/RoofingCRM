package com.roofingcrm.service.lead;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.TestDatabaseCleaner;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.lead.ConvertLeadToJobRequest;
import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.NewLeadCustomerRequest;
import com.roofingcrm.api.v1.lead.UpdateLeadStatusRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.exception.LeadConversionNotAllowedException;
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
    private CustomerRepository customerRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantUserMembershipRepository membershipRepository;

    @Autowired
    private TestDatabaseCleaner dbCleaner;

    @NonNull
    private UUID tenantId = Objects.requireNonNull(UUID.randomUUID());
    @NonNull
    private UUID userId = Objects.requireNonNull(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        dbCleaner.reset();

        Tenant tenant = new Tenant();
        tenant.setName("Test Roofing");
        tenant.setSlug("test-roofing");
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setEmail("test-user@example.com");
        user.setFullName("Test User");
        user.setPasswordHash("irrelevant-for-this-test");
        user.setEnabled(true);
        user = userRepository.save(user);

        TenantUserMembership membership = new TenantUserMembership();
        membership.setTenant(tenant);
        membership.setUser(user);
        membership.setRole(UserRole.OWNER);
        membershipRepository.save(membership);

        this.tenantId = Objects.requireNonNull(tenant.getId());
        this.userId = Objects.requireNonNull(user.getId());
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
        assertEquals("Alice", dto.getCustomerFirstName());
        assertEquals("Roofer", dto.getCustomerLastName());
        assertEquals("alice@example.com", dto.getCustomerEmail());
        assertEquals("555-0000", dto.getCustomerPhone());
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

        Page<LeadDto> newLeads = leadService.listLeads(tenantId, userId, LeadStatus.NEW, null, PageRequest.of(0, 10));
        Page<LeadDto> wonLeads = leadService.listLeads(tenantId, userId, LeadStatus.WON, null, PageRequest.of(0, 10));

        assertEquals(1, newLeads.getTotalElements());
        LeadDto firstNew = newLeads.getContent().get(0);
        assertEquals(lead1.getId(), firstNew.getId());
        assertEquals("Lead1", firstNew.getCustomerFirstName());
        assertEquals("New", firstNew.getCustomerLastName());

        assertEquals(1, wonLeads.getTotalElements());
        LeadDto firstWon = wonLeads.getContent().get(0);
        assertEquals(lead2.getId(), firstWon.getId());
        assertEquals("Lead2", firstWon.getCustomerFirstName());
        assertEquals("Won", firstWon.getCustomerLastName());
    }

    @Test
    void getLead_nonExisting_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> leadService.getLead(tenantId, userId, randomId));
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

    @Test
    void listLeads_returnsCustomerFields() {
        CreateLeadRequest request = new CreateLeadRequest();
        NewLeadCustomerRequest c = new NewLeadCustomerRequest();
        c.setFirstName("Customer");
        c.setLastName("Fields");
        c.setPrimaryPhone("555-1234");
        c.setEmail("customer@example.com");
        request.setNewCustomer(c);
        request.setPropertyAddress(createPropertyAddress());

        LeadDto created = leadService.createLead(tenantId, userId, request);

        Page<LeadDto> page = leadService.listLeads(tenantId, userId, null, null, PageRequest.of(0, 10));
        LeadDto found = page.getContent().stream()
                .filter(l -> l.getId().equals(created.getId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(found.getCustomerId());
        assertEquals("Customer", found.getCustomerFirstName());
        assertEquals("Fields", found.getCustomerLastName());
        assertEquals("customer@example.com", found.getCustomerEmail());
        assertEquals("555-1234", found.getCustomerPhone());
    }

    @Test
    void listLeads_withCustomerIdFilter_returnsOnlyMatchingLeads() {
        // Create two customers
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer customer1 = new Customer();
        customer1.setTenant(tenant);
        customer1.setFirstName("Customer1");
        customer1.setLastName("Test");
        customer1.setPrimaryPhone("111");
        customer1 = customerRepository.save(customer1);
        UUID customer1Id = Objects.requireNonNull(customer1.getId());

        Customer customer2 = new Customer();
        customer2.setTenant(tenant);
        customer2.setFirstName("Customer2");
        customer2.setLastName("Test");
        customer2.setPrimaryPhone("222");
        customer2 = customerRepository.save(customer2);
        UUID customer2Id = Objects.requireNonNull(customer2.getId());

        // Create leads for each customer
        CreateLeadRequest request1 = new CreateLeadRequest();
        request1.setCustomerId(customer1Id);
        request1.setPropertyAddress(createPropertyAddress());
        LeadDto lead1 = leadService.createLead(tenantId, userId, request1);

        CreateLeadRequest request2 = new CreateLeadRequest();
        request2.setCustomerId(customer2Id);
        request2.setPropertyAddress(createPropertyAddress());
        LeadDto lead2 = leadService.createLead(tenantId, userId, request2);

        // Filter by customer1Id
        Page<LeadDto> customer1Leads = leadService.listLeads(tenantId, userId, null, customer1Id, PageRequest.of(0, 10));
        assertEquals(1, customer1Leads.getTotalElements());
        assertEquals(lead1.getId(), customer1Leads.getContent().get(0).getId());

        // Filter by customer2Id
        Page<LeadDto> customer2Leads = leadService.listLeads(tenantId, userId, null, customer2Id, PageRequest.of(0, 10));
        assertEquals(1, customer2Leads.getTotalElements());
        assertEquals(lead2.getId(), customer2Leads.getContent().get(0).getId());

        // No filter returns both
        Page<LeadDto> allLeads = leadService.listLeads(tenantId, userId, null, null, PageRequest.of(0, 10));
        assertEquals(2, allLeads.getTotalElements());
    }

    @Test
    void convertLeadToJob_createsJobAndUpdatesLeadStatus() {
        // Create customer and lead
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("Convert");
        customer.setLastName("Test");
        customer.setPrimaryPhone("555-9999");
        customer.setEmail("convert@example.com");
        customer = customerRepository.save(customer);

        CreateLeadRequest leadRequest = new CreateLeadRequest();
        leadRequest.setCustomerId(customer.getId());
        leadRequest.setPropertyAddress(createPropertyAddress());
        LeadDto lead = leadService.createLead(tenantId, userId, leadRequest);

        // Update lead to a convertible status
        leadService.updateLeadStatus(tenantId, userId, lead.getId(), LeadStatus.QUOTE_SENT);

        // Convert to job
        ConvertLeadToJobRequest convertRequest = new ConvertLeadToJobRequest();
        convertRequest.setType(JobType.REPLACEMENT);
        convertRequest.setScheduledStartDate(java.time.LocalDate.now().plusDays(7));
        convertRequest.setCrewName("Team Alpha");
        convertRequest.setInternalNotes("Converted from lead");

        JobDto job = leadService.convertLeadToJob(tenantId, userId, lead.getId(), convertRequest);

        // Assert job properties
        assertNotNull(job.getId());
        assertEquals(lead.getId(), job.getLeadId());
        assertEquals(customer.getId(), job.getCustomerId());
        assertEquals("Convert", job.getCustomerFirstName());
        assertEquals("Test", job.getCustomerLastName());
        assertEquals("convert@example.com", job.getCustomerEmail());
        assertEquals("555-9999", job.getCustomerPhone());
        assertEquals(JobType.REPLACEMENT, job.getType());
        assertEquals(convertRequest.getScheduledStartDate(), job.getScheduledStartDate());
        assertEquals("Team Alpha", job.getCrewName());
        assertEquals("Converted from lead", job.getInternalNotes());

        // Assert property address matches lead
        assertNotNull(job.getPropertyAddress());
        assertEquals("456 Roof St", job.getPropertyAddress().getLine1());
        assertEquals("Chicago", job.getPropertyAddress().getCity());
        assertEquals("IL", job.getPropertyAddress().getState());
        assertEquals("60602", job.getPropertyAddress().getZip());

        // Assert lead status updated to WON
        LeadDto updatedLead = leadService.getLead(tenantId, userId, lead.getId());
        assertEquals(LeadStatus.WON, updatedLead.getStatus());
    }

    @Test
    void convertLeadToJob_isIdempotent_returnsExistingJob() {
        // Create customer and lead
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("Idempotent");
        customer.setLastName("Test");
        customer.setPrimaryPhone("555-8888");
        customer = customerRepository.save(customer);

        CreateLeadRequest leadRequest = new CreateLeadRequest();
        leadRequest.setCustomerId(customer.getId());
        leadRequest.setPropertyAddress(createPropertyAddress());
        LeadDto lead = leadService.createLead(tenantId, userId, leadRequest);

        // Convert first time
        ConvertLeadToJobRequest convertRequest = new ConvertLeadToJobRequest();
        convertRequest.setType(JobType.REPAIR);
        JobDto job1 = leadService.convertLeadToJob(tenantId, userId, lead.getId(), convertRequest);

        // Convert second time
        ConvertLeadToJobRequest convertRequest2 = new ConvertLeadToJobRequest();
        convertRequest2.setType(JobType.INSPECTION_ONLY); // Different type, should be ignored
        JobDto job2 = leadService.convertLeadToJob(tenantId, userId, lead.getId(), convertRequest2);

        // Assert same job returned
        assertEquals(job1.getId(), job2.getId());

        // Assert only one job exists for this lead
        assertTrue(jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, lead.getId()).isPresent());
        assertEquals(1, jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, lead.getId()).stream().count());
    }

    @Test
    void convertLeadToJob_whenLeadLost_returnsConflict() {
        // Create customer and lead
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("Lost");
        customer.setLastName("Lead");
        customer.setPrimaryPhone("555-7777");
        customer = customerRepository.save(customer);

        CreateLeadRequest leadRequest = new CreateLeadRequest();
        leadRequest.setCustomerId(customer.getId());
        leadRequest.setPropertyAddress(createPropertyAddress());
        LeadDto lead = leadService.createLead(tenantId, userId, leadRequest);

        // Mark lead as LOST
        leadService.updateLeadStatus(tenantId, userId, lead.getId(), LeadStatus.LOST);

        // Attempt conversion
        ConvertLeadToJobRequest convertRequest = new ConvertLeadToJobRequest();
        convertRequest.setType(JobType.REPLACEMENT);

        assertThrows(LeadConversionNotAllowedException.class,
                () -> leadService.convertLeadToJob(tenantId, userId, lead.getId(), convertRequest));
    }

    @Test
    void getLead_whenConverted_setsConvertedJobId() {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("Converted");
        customer.setLastName("Lead");
        customer.setPrimaryPhone("555-6666");
        customer = customerRepository.save(customer);

        CreateLeadRequest leadRequest = new CreateLeadRequest();
        leadRequest.setCustomerId(customer.getId());
        leadRequest.setPropertyAddress(createPropertyAddress());
        LeadDto lead = leadService.createLead(tenantId, userId, leadRequest);

        ConvertLeadToJobRequest convertRequest = new ConvertLeadToJobRequest();
        convertRequest.setType(JobType.REPLACEMENT);
        JobDto job = leadService.convertLeadToJob(tenantId, userId, lead.getId(), convertRequest);

        LeadDto got = leadService.getLead(tenantId, userId, lead.getId());
        assertEquals(job.getId(), got.getConvertedJobId());
    }
}
