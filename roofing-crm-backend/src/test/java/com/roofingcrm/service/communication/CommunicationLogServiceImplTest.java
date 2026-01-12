package com.roofingcrm.service.communication;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.api.v1.communication.CommunicationLogDto;
import com.roofingcrm.api.v1.communication.CreateCommunicationLogRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantUserMembership;
import com.roofingcrm.domain.entity.User;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.CommunicationLogRepository;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import com.roofingcrm.domain.repository.TenantUserMembershipRepository;
import com.roofingcrm.domain.repository.UserRepository;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommunicationLogServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private CommunicationLogService communicationLogService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantUserMembershipRepository membershipRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @NonNull
    private UUID tenantId = Objects.requireNonNull(UUID.randomUUID());
    @NonNull
    private UUID userId = Objects.requireNonNull(UUID.randomUUID());
    @NonNull
    private UUID leadId = Objects.requireNonNull(UUID.randomUUID());
    @NonNull
    private UUID jobId = Objects.requireNonNull(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        communicationLogRepository.deleteAll();
        jobRepository.deleteAll();
        leadRepository.deleteAll();
        membershipRepository.deleteAll();
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

        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setCreatedByUserId(user.getId());
        customer.setUpdatedByUserId(user.getId());
        customer = customerRepository.save(customer);

        Lead lead = new Lead();
        lead.setTenant(tenant);
        lead.setCustomer(customer);
        lead.setStatus(LeadStatus.NEW);
        lead.setSource(LeadSource.WEBSITE);
        lead.setCreatedByUserId(user.getId());
        lead.setUpdatedByUserId(user.getId());
        lead = leadRepository.save(lead);

        Job job = new Job();
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setStatus(JobStatus.SCHEDULED);
        job.setJobType(JobType.REPLACEMENT);
        job.setCreatedByUserId(user.getId());
        job.setUpdatedByUserId(user.getId());
        job = jobRepository.save(job);

        this.tenantId = Objects.requireNonNull(tenant.getId());
        this.userId = Objects.requireNonNull(user.getId());
        this.leadId = Objects.requireNonNull(lead.getId());
        this.jobId = Objects.requireNonNull(job.getId());
    }

    @Test
    void addForLead_createsLogEntry() {
        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setChannel("CALL");
        request.setDirection("OUTBOUND");
        request.setSubject("Initial contact");
        request.setBody("Called to discuss roofing needs");

        CommunicationLogDto dto = communicationLogService.addForLead(tenantId, userId, leadId, request);

        assertNotNull(dto.getId());
        assertEquals("CALL", dto.getChannel());
        assertEquals("OUTBOUND", dto.getDirection());
        assertEquals("Initial contact", dto.getSubject());
        assertEquals(leadId, dto.getLeadId());
        assertNull(dto.getJobId());
        assertNotNull(dto.getOccurredAt()); // Should default to now
    }

    @Test
    void addForLead_withOccurredAt_usesProvidedTime() {
        Instant customTime = Instant.parse("2025-06-15T10:30:00Z");

        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setChannel("EMAIL");
        request.setSubject("Follow up");
        request.setOccurredAt(customTime);

        CommunicationLogDto dto = communicationLogService.addForLead(tenantId, userId, leadId, request);

        assertEquals(customTime, dto.getOccurredAt());
    }

    @Test
    void addForJob_createsLogEntry() {
        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setChannel("NOTE");
        request.setSubject("Job site visit notes");
        request.setBody("Measured the roof area");

        CommunicationLogDto dto = communicationLogService.addForJob(tenantId, userId, jobId, request);

        assertNotNull(dto.getId());
        assertEquals("NOTE", dto.getChannel());
        assertEquals(jobId, dto.getJobId());
        assertNull(dto.getLeadId());
    }

    @Test
    void listForLead_returnsLogsInDescendingOrder() {
        Instant older = Instant.parse("2025-01-01T10:00:00Z");
        Instant newer = Instant.parse("2025-06-01T10:00:00Z");

        CreateCommunicationLogRequest request1 = new CreateCommunicationLogRequest();
        request1.setChannel("CALL");
        request1.setSubject("First call");
        request1.setOccurredAt(older);

        CreateCommunicationLogRequest request2 = new CreateCommunicationLogRequest();
        request2.setChannel("EMAIL");
        request2.setSubject("Follow up email");
        request2.setOccurredAt(newer);

        communicationLogService.addForLead(tenantId, userId, leadId, request1);
        communicationLogService.addForLead(tenantId, userId, leadId, request2);

        List<CommunicationLogDto> logs = communicationLogService.listForLead(tenantId, userId, leadId);

        assertEquals(2, logs.size());
        // Should be ordered descending by occurredAt
        assertEquals("Follow up email", logs.get(0).getSubject());
        assertEquals("First call", logs.get(1).getSubject());
    }

    @Test
    void addForLead_withoutMembership_throwsAccessDenied() {
        // Create a new user without membership
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setFullName("Another User");
        anotherUser.setPasswordHash("hash");
        anotherUser.setEnabled(true);
        anotherUser = userRepository.save(anotherUser);

        UUID otherUserId = Objects.requireNonNull(anotherUser.getId());

        CreateCommunicationLogRequest request = new CreateCommunicationLogRequest();
        request.setChannel("CALL");
        request.setSubject("Test");

        assertThrows(TenantAccessDeniedException.class,
                () -> communicationLogService.addForLead(tenantId, otherUserId, leadId, request));
    }
}
