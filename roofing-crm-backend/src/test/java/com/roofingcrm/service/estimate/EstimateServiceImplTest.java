package com.roofingcrm.service.estimate;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.api.v1.estimate.CreateEstimateRequest;
import com.roofingcrm.api.v1.estimate.EstimateDto;
import com.roofingcrm.api.v1.estimate.EstimateItemRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstimateServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private EstimateService estimateService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EstimateRepository estimateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantUserMembershipRepository membershipRepository;

    @NonNull
    private UUID tenantId = Objects.requireNonNull(UUID.randomUUID());
    @NonNull
    private UUID userId = Objects.requireNonNull(UUID.randomUUID());
    private UUID jobId;
    @NonNull
    private UUID customerId = Objects.requireNonNull(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        estimateRepository.deleteAll();
        jobRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

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

        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setPrimaryPhone("555-1234");
        customer = customerRepository.save(customer);
        this.customerId = Objects.requireNonNull(customer.getId());

        Job job = new Job();
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setJobType(JobType.REPLACEMENT);
        job.setStatus(JobStatus.SCHEDULED);
        Address address = new Address();
        address.setLine1("123 Main St");
        address.setCity("Chicago");
        address.setState("IL");
        address.setZip("60601");
        job.setPropertyAddress(address);
        job = jobRepository.save(job);
        this.jobId = Objects.requireNonNull(job.getId());
    }

    @Test
    void createEstimateForJob_withMultipleItems_persistsAndComputesTotals() {
        CreateEstimateRequest request = new CreateEstimateRequest();
        request.setTitle("Roof Replacement Estimate");
        request.setNotes("Customer notes here");
        request.setIssueDate(LocalDate.now());
        request.setValidUntil(LocalDate.now().plusDays(30));

        EstimateItemRequest item1 = new EstimateItemRequest();
        item1.setName("Shingles");
        item1.setDescription("Architectural shingles");
        item1.setQuantity(new BigDecimal("100"));
        item1.setUnitPrice(new BigDecimal("25.00"));
        item1.setUnit("sqft");

        EstimateItemRequest item2 = new EstimateItemRequest();
        item2.setName("Labor");
        item2.setDescription("Installation labor");
        item2.setQuantity(new BigDecimal("8"));
        item2.setUnitPrice(new BigDecimal("75.00"));
        item2.setUnit("hours");

        request.setItems(List.of(item1, item2));

        EstimateDto dto = estimateService.createEstimateForJob(tenantId, userId, jobId, request);

        assertNotNull(dto.getId());
        assertEquals(jobId, dto.getJobId());
        assertEquals(customerId, dto.getCustomerId());
        assertEquals(EstimateStatus.DRAFT, dto.getStatus());
        assertEquals("Roof Replacement Estimate", dto.getTitle());
        assertEquals("Customer notes here", dto.getNotes());
        assertNotNull(dto.getItems());
        assertEquals(2, dto.getItems().size());

        // Verify computed totals: (100 * 25) + (8 * 75) = 2500 + 600 = 3100
        assertEquals(new BigDecimal("3100.00"), dto.getSubtotal());
        assertEquals(new BigDecimal("3100.00"), dto.getTotal());
    }

    @Test
    void createEstimateForJob_withInitialStatus_usesProvidedStatus() {
        CreateEstimateRequest request = new CreateEstimateRequest();
        request.setTitle("Sent Estimate");
        request.setStatus(EstimateStatus.SENT);

        EstimateItemRequest item = new EstimateItemRequest();
        item.setName("Inspection");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("150.00"));
        request.setItems(List.of(item));

        EstimateDto dto = estimateService.createEstimateForJob(tenantId, userId, jobId, request);

        assertEquals(EstimateStatus.SENT, dto.getStatus());
    }

    @Test
    void listEstimatesForJob_returnsOnlyJobEstimates() {
        // Create estimate for main job
        CreateEstimateRequest request1 = new CreateEstimateRequest();
        request1.setTitle("Estimate 1");
        EstimateItemRequest item1 = new EstimateItemRequest();
        item1.setName("Item 1");
        item1.setQuantity(new BigDecimal("1"));
        item1.setUnitPrice(new BigDecimal("100.00"));
        request1.setItems(List.of(item1));
        EstimateDto estimate1 = estimateService.createEstimateForJob(tenantId, userId, jobId, request1);

        // Create another job and estimate
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        Job otherJob = new Job();
        otherJob.setTenant(tenant);
        otherJob.setCustomer(customer);
        otherJob.setJobType(JobType.REPAIR);
        otherJob.setStatus(JobStatus.SCHEDULED);
        otherJob = jobRepository.save(otherJob);

        CreateEstimateRequest request2 = new CreateEstimateRequest();
        request2.setTitle("Estimate 2");
        EstimateItemRequest item2 = new EstimateItemRequest();
        item2.setName("Item 2");
        item2.setQuantity(new BigDecimal("1"));
        item2.setUnitPrice(new BigDecimal("200.00"));
        request2.setItems(List.of(item2));
        estimateService.createEstimateForJob(tenantId, userId, otherJob.getId(), request2);

        // List estimates for main job
        List<EstimateDto> estimates = estimateService.listEstimatesForJob(tenantId, userId, jobId);

        assertEquals(1, estimates.size());
        assertEquals(estimate1.getId(), estimates.get(0).getId());
    }

    @Test
    void updateEstimateStatus_changesStatus() {
        CreateEstimateRequest request = new CreateEstimateRequest();
        request.setTitle("Test Estimate");
        EstimateItemRequest item = new EstimateItemRequest();
        item.setName("Test Item");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("100.00"));
        request.setItems(List.of(item));

        EstimateDto estimate = estimateService.createEstimateForJob(tenantId, userId, jobId, request);
        assertEquals(EstimateStatus.DRAFT, estimate.getStatus());

        EstimateDto updated = estimateService.updateEstimateStatus(tenantId, userId, estimate.getId(), EstimateStatus.ACCEPTED);

        assertEquals(EstimateStatus.ACCEPTED, updated.getStatus());
    }

    @Test
    void getEstimate_nonExisting_throwsNotFound() {
        UUID randomId = UUID.randomUUID();
        assertThrows(ResourceNotFoundException.class,
                () -> estimateService.getEstimate(tenantId, userId, randomId));
    }
}
