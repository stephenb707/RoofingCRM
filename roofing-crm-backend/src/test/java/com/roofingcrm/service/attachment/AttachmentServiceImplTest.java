package com.roofingcrm.service.attachment;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.TestDatabaseCleaner;
import com.roofingcrm.api.v1.attachment.AttachmentDto;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentServiceImplTest extends AbstractIntegrationTest {

    @Autowired
    private AttachmentService attachmentService;

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
    private TestDatabaseCleaner dbCleaner;

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
    void uploadForLead_storesMetadataAndFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-doc.pdf",
                "application/pdf",
                "Test PDF content".getBytes()
        );

        AttachmentDto dto = attachmentService.uploadForLead(tenantId, userId, leadId, file, null, null);

        assertNotNull(dto.getId());
        assertEquals("test-doc.pdf", dto.getFileName());
        assertEquals("application/pdf", dto.getContentType());
        assertEquals("LOCAL", dto.getStorageProvider());
        assertEquals(leadId, dto.getLeadId());
        assertNull(dto.getJobId());
        assertNotNull(dto.getStorageKey());
    }

    @Test
    void uploadForJob_storesMetadataAndFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "job-photo.jpg",
                "image/jpeg",
                "Fake image bytes".getBytes()
        );

        AttachmentDto dto = attachmentService.uploadForJob(tenantId, userId, jobId, file, null, null);

        assertNotNull(dto.getId());
        assertEquals("job-photo.jpg", dto.getFileName());
        assertEquals("image/jpeg", dto.getContentType());
        assertEquals("LOCAL", dto.getStorageProvider());
        assertEquals(jobId, dto.getJobId());
        assertNull(dto.getLeadId());
    }

    @Test
    void listForLead_returnsAttachments() {
        MockMultipartFile file1 = new MockMultipartFile("file", "doc1.pdf", "application/pdf", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "doc2.pdf", "application/pdf", "content2".getBytes());

        attachmentService.uploadForLead(tenantId, userId, leadId, file1, null, null);
        attachmentService.uploadForLead(tenantId, userId, leadId, file2, null, null);

        List<AttachmentDto> attachments = attachmentService.listForLead(tenantId, userId, leadId);

        assertEquals(2, attachments.size());
    }

    @Test
    void uploadForLead_withoutMembership_throwsAccessDenied() {
        // Create a new user without membership
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setFullName("Another User");
        anotherUser.setPasswordHash("hash");
        anotherUser.setEnabled(true);
        anotherUser = userRepository.save(anotherUser);

        UUID otherUserId = Objects.requireNonNull(anotherUser.getId());
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        assertThrows(TenantAccessDeniedException.class,
                () -> attachmentService.uploadForLead(tenantId, otherUserId, leadId, file, null, null));
    }
}
