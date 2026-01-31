package com.roofingcrm.service.attachment;

import com.roofingcrm.api.v1.attachment.AttachmentDto;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.AttachmentTag;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.storage.AttachmentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class AttachmentServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private LeadRepository leadRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private AttachmentStorageService storageService;
    @Mock
    private ActivityEventService activityEventService;

    private AttachmentServiceImpl service;

    private Tenant tenant;
    private Lead lead;
    private Job job;
    private UUID tenantId;
    private UUID userId;
    private UUID leadId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        service = new AttachmentServiceImpl(
                tenantAccessService, attachmentRepository, leadRepository, jobRepository,
                storageService, activityEventService);

        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug("test-tenant");
        tenantId = tenant.getId();

        userId = UUID.randomUUID();

        lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setTenant(tenant);
        leadId = lead.getId();

        job = new Job();
        job.setId(UUID.randomUUID());
        job.setTenant(tenant);
        jobId = job.getId();
    }

    @Test
    void uploadForLead_withTag_savesAttachmentAndEmitsActivityEvent() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)).thenReturn(Optional.of(lead));

        Attachment savedAttachment = new Attachment();
        savedAttachment.setId(UUID.randomUUID());
        savedAttachment.setFileName("damage.jpg");
        savedAttachment.setContentType("image/jpeg");
        savedAttachment.setFileSize(1024L);
        savedAttachment.setTag(AttachmentTag.DAMAGE);
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId(savedAttachment.getId());
            a.setFileName(savedAttachment.getFileName());
            a.setContentType(savedAttachment.getContentType());
            a.setFileSize(savedAttachment.getFileSize());
            a.setTag(savedAttachment.getTag());
            return a;
        });
        when(storageService.store(anyString(), any(UUID.class), any())).thenReturn("storage/key/damage.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "damage.jpg", "image/jpeg", "bytes".getBytes());

        AttachmentDto dto = service.uploadForLead(tenantId, userId, leadId, file, AttachmentTag.DAMAGE, "Roof damage photo");

        assertNotNull(dto.getId());
        assertEquals("damage.jpg", dto.getFileName());
        assertEquals(AttachmentTag.DAMAGE, dto.getTag());

        ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.LEAD), eq(leadId),
                eq(ActivityEventType.ATTACHMENT_ADDED), contains("Added "), metaCaptor.capture());

        Map<String, Object> meta = metaCaptor.getValue();
        assertNotNull(meta.get("attachmentId"));
        assertEquals("damage.jpg", meta.get("fileName"));
        assertEquals("image/jpeg", meta.get("contentType"));
        assertEquals(1024L, meta.get("fileSize"));
        assertEquals("DAMAGE", meta.get("tag"));
    }

    @Test
    void uploadForJob_withTag_savesAttachmentAndEmitsActivityEvent() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));

        Attachment savedAttachment = new Attachment();
        savedAttachment.setId(UUID.randomUUID());
        savedAttachment.setFileName("after.jpg");
        savedAttachment.setContentType("image/jpeg");
        savedAttachment.setFileSize(2048L);
        savedAttachment.setTag(AttachmentTag.AFTER);
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            a.setId(savedAttachment.getId());
            a.setFileName(savedAttachment.getFileName());
            a.setContentType(savedAttachment.getContentType());
            a.setFileSize(savedAttachment.getFileSize());
            a.setTag(savedAttachment.getTag());
            return a;
        });
        when(storageService.store(anyString(), any(UUID.class), any())).thenReturn("storage/key/after.jpg");

        MockMultipartFile file = new MockMultipartFile("file", "after.jpg", "image/jpeg", "bytes".getBytes());

        AttachmentDto dto = service.uploadForJob(tenantId, userId, jobId, file, AttachmentTag.AFTER, null);

        assertNotNull(dto.getId());
        assertEquals(AttachmentTag.AFTER, dto.getTag());

        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.ATTACHMENT_ADDED), contains("Added "), any(Map.class));
    }
}
