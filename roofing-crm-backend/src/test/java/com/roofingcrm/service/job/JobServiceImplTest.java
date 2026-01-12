package com.roofingcrm.service.job;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
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

class JobServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private JobRepository jobRepository;

    @NonNull
    private UUID tenantId = Objects.requireNonNull(UUID.randomUUID());
    private UUID userId;
    private UUID customerId;
    private UUID leadId;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        leadRepository.deleteAll();
        customerRepository.deleteAll();
        tenantRepository.deleteAll();

        Tenant tenant = new Tenant();
        tenant.setName("Test Roofing");
        tenant.setSlug("test-roofing");
        tenant = tenantRepository.save(tenant);
        this.tenantId = Objects.requireNonNull(tenant.getId());

        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setPrimaryPhone("555-1234");
        customer = customerRepository.save(customer);
        this.customerId = Objects.requireNonNull(customer.getId());

        Lead lead = new Lead();
        lead.setTenant(tenant);
        lead.setCustomer(customer);
        lead.setStatus(LeadStatus.NEW);
        lead.setSource(LeadSource.WEBSITE);
        lead = leadRepository.save(lead);
        this.leadId = Objects.requireNonNull(lead.getId());

        this.userId = UUID.randomUUID();
    }

    private AddressDto createPropertyAddress() {
        AddressDto address = new AddressDto();
        address.setLine1("123 Main St");
        address.setCity("Chicago");
        address.setState("IL");
        address.setZip("60601");
        address.setCountryCode("US");
        return address;
    }

    @Test
    void createJob_withLeadId_createsJobLinkedToLead() {
        CreateJobRequest request = new CreateJobRequest();
        request.setLeadId(leadId);
        request.setType(JobType.REPLACEMENT);
        request.setPropertyAddress(createPropertyAddress());
        request.setInternalNotes("Test job notes");
        request.setCrewName("Team Alpha");

        JobDto dto = jobService.createJob(tenantId, userId, request);

        assertNotNull(dto.getId());
        assertEquals(customerId, dto.getCustomerId());
        assertEquals(leadId, dto.getLeadId());
        assertEquals(JobStatus.SCHEDULED, dto.getStatus());
        assertEquals(JobType.REPLACEMENT, dto.getType());
        assertEquals("Test job notes", dto.getInternalNotes());
        assertEquals("Team Alpha", dto.getCrewName());
        assertNotNull(dto.getPropertyAddress());
        assertEquals("123 Main St", dto.getPropertyAddress().getLine1());
    }

    @Test
    void createJob_withCustomerId_createsJobWithoutLead() {
        CreateJobRequest request = new CreateJobRequest();
        request.setCustomerId(customerId);
        request.setType(JobType.REPAIR);
        request.setPropertyAddress(createPropertyAddress());

        JobDto dto = jobService.createJob(tenantId, userId, request);

        assertNotNull(dto.getId());
        assertEquals(customerId, dto.getCustomerId());
        assertNull(dto.getLeadId());
        assertEquals(JobStatus.SCHEDULED, dto.getStatus());
        assertEquals(JobType.REPAIR, dto.getType());
    }

    @Test
    void createJob_withoutLeadOrCustomer_throwsException() {
        CreateJobRequest request = new CreateJobRequest();
        request.setType(JobType.INSPECTION_ONLY);
        request.setPropertyAddress(createPropertyAddress());

        assertThrows(IllegalArgumentException.class,
                () -> jobService.createJob(tenantId, userId, request));
    }

    @Test
    void listJobs_withStatusFilter_returnsOnlyMatching() {
        // Create two jobs with different statuses
        CreateJobRequest request1 = new CreateJobRequest();
        request1.setCustomerId(customerId);
        request1.setType(JobType.REPLACEMENT);
        request1.setPropertyAddress(createPropertyAddress());
        JobDto job1 = jobService.createJob(tenantId, userId, request1);

        CreateJobRequest request2 = new CreateJobRequest();
        request2.setCustomerId(customerId);
        request2.setType(JobType.REPAIR);
        request2.setPropertyAddress(createPropertyAddress());
        JobDto job2 = jobService.createJob(tenantId, userId, request2);

        // Update second job to IN_PROGRESS
        jobService.updateJobStatus(tenantId, userId, job2.getId(), JobStatus.IN_PROGRESS);

        Page<JobDto> scheduledJobs = jobService.listJobs(tenantId, JobStatus.SCHEDULED, null, PageRequest.of(0, 10));
        Page<JobDto> inProgressJobs = jobService.listJobs(tenantId, JobStatus.IN_PROGRESS, null, PageRequest.of(0, 10));

        assertEquals(1, scheduledJobs.getTotalElements());
        assertEquals(job1.getId(), scheduledJobs.getContent().get(0).getId());

        assertEquals(1, inProgressJobs.getTotalElements());
        assertEquals(job2.getId(), inProgressJobs.getContent().get(0).getId());
    }

    @Test
    void listJobs_withCustomerIdFilter_returnsOnlyCustomerJobs() {
        // Create another customer and job
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer otherCustomer = new Customer();
        otherCustomer.setTenant(tenant);
        otherCustomer.setFirstName("Jane");
        otherCustomer.setLastName("Smith");
        otherCustomer.setPrimaryPhone("555-5678");
        otherCustomer = customerRepository.save(otherCustomer);

        CreateJobRequest request1 = new CreateJobRequest();
        request1.setCustomerId(customerId);
        request1.setType(JobType.REPLACEMENT);
        request1.setPropertyAddress(createPropertyAddress());
        JobDto job1 = jobService.createJob(tenantId, userId, request1);

        CreateJobRequest request2 = new CreateJobRequest();
        request2.setCustomerId(otherCustomer.getId());
        request2.setType(JobType.REPAIR);
        request2.setPropertyAddress(createPropertyAddress());
        jobService.createJob(tenantId, userId, request2);

        Page<JobDto> customerJobs = jobService.listJobs(tenantId, null, customerId, PageRequest.of(0, 10));

        assertEquals(1, customerJobs.getTotalElements());
        assertEquals(job1.getId(), customerJobs.getContent().get(0).getId());
    }

    @Test
    void updateJobStatus_changesStatus() {
        CreateJobRequest request = new CreateJobRequest();
        request.setCustomerId(customerId);
        request.setType(JobType.REPLACEMENT);
        request.setPropertyAddress(createPropertyAddress());
        JobDto job = jobService.createJob(tenantId, userId, request);

        assertEquals(JobStatus.SCHEDULED, job.getStatus());

        JobDto updated = jobService.updateJobStatus(tenantId, userId, job.getId(), JobStatus.COMPLETED);

        assertEquals(JobStatus.COMPLETED, updated.getStatus());
    }

    @Test
    void getJob_nonExisting_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> jobService.getJob(tenantId, randomId));
    }
}
