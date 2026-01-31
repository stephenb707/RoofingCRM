package com.roofingcrm.service.attachment;

import com.roofingcrm.api.v1.attachment.AttachmentDto;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.AttachmentTag;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.storage.AttachmentStorageService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private final TenantAccessService tenantAccessService;
    private final AttachmentRepository attachmentRepository;
    private final LeadRepository leadRepository;
    private final JobRepository jobRepository;
    private final AttachmentStorageService storageService;
    private final ActivityEventService activityEventService;

    public AttachmentServiceImpl(TenantAccessService tenantAccessService,
                                  AttachmentRepository attachmentRepository,
                                  LeadRepository leadRepository,
                                  JobRepository jobRepository,
                                  AttachmentStorageService storageService,
                                  ActivityEventService activityEventService) {
        this.tenantAccessService = tenantAccessService;
        this.attachmentRepository = attachmentRepository;
        this.leadRepository = leadRepository;
        this.jobRepository = jobRepository;
        this.storageService = storageService;
        this.activityEventService = activityEventService;
    }

    @Override
    public AttachmentDto uploadForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId, MultipartFile file,
                                       AttachmentTag tag, String description) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        Attachment attachment = createAttachment(tenant, userId, file, tag, description);
        attachment.setLead(lead);

        // Save first to get ID
        attachment = attachmentRepository.save(attachment);

        // Store file and update storage key
        String tenantSlug = tenant.getSlug() != null ? tenant.getSlug() : tenant.getId().toString();
        String storageKey = storageService.store(tenantSlug, attachment.getId(), file);
        attachment.setStorageKey(storageKey);

        attachment = attachmentRepository.save(attachment);

        Map<String, Object> meta = new HashMap<>();
        meta.put("attachmentId", attachment.getId());
        meta.put("fileName", attachment.getFileName());
        meta.put("contentType", attachment.getContentType());
        meta.put("fileSize", attachment.getFileSize());
        meta.put("tag", attachment.getTag().name());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.LEAD, lead.getId(),
                ActivityEventType.ATTACHMENT_ADDED, "Added " + formatAttachmentLabel(attachment), meta);

        return toDto(attachment);
    }

    @Override
    public AttachmentDto uploadForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId, MultipartFile file,
                                      AttachmentTag tag, String description) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        Attachment attachment = createAttachment(tenant, userId, file, tag, description);
        attachment.setJob(job);

        // Save first to get ID
        attachment = attachmentRepository.save(attachment);

        // Store file and update storage key
        String tenantSlug = tenant.getSlug() != null ? tenant.getSlug() : tenant.getId().toString();
        String storageKey = storageService.store(tenantSlug, attachment.getId(), file);
        attachment.setStorageKey(storageKey);

        attachment = attachmentRepository.save(attachment);

        Map<String, Object> meta = new HashMap<>();
        meta.put("attachmentId", attachment.getId());
        meta.put("fileName", attachment.getFileName());
        meta.put("contentType", attachment.getContentType());
        meta.put("fileSize", attachment.getFileSize());
        meta.put("tag", attachment.getTag().name());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB, job.getId(),
                ActivityEventType.ATTACHMENT_ADDED, "Added " + formatAttachmentLabel(attachment), meta);

        return toDto(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> listForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        return attachmentRepository.findByTenantAndLeadAndArchivedFalse(tenant, lead)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> listForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return attachmentRepository.findByTenantAndJobAndArchivedFalse(tenant, job)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDto getAttachment(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID attachmentId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Attachment attachment = attachmentRepository.findByIdAndTenantAndArchivedFalse(attachmentId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        return toDto(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream loadAttachmentContent(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID attachmentId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Attachment attachment = attachmentRepository.findByIdAndTenantAndArchivedFalse(attachmentId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        if (attachment.getStorageKey() == null) {
            throw new RuntimeException("Attachment has no storage key");
        }

        return storageService.loadAsStream(attachment.getStorageKey());
    }

    private Attachment createAttachment(Tenant tenant, UUID userId, MultipartFile file, AttachmentTag tag, String description) {
        Attachment attachment = new Attachment();
        attachment.setTenant(tenant);
        attachment.setCreatedByUserId(userId);
        attachment.setUpdatedByUserId(userId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStorageProvider("LOCAL");
        attachment.setTag(tag != null ? tag : AttachmentTag.OTHER);
        attachment.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        return attachment;
    }

    private static String formatAttachmentLabel(Attachment a) {
        String fn = a.getFileName();
        return "photo/doc (" + a.getTag().name() + "): " + (fn != null ? fn : "unnamed");
    }

    private AttachmentDto toDto(Attachment entity) {
        AttachmentDto dto = new AttachmentDto();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setContentType(entity.getContentType());
        dto.setFileSize(entity.getFileSize());
        dto.setStorageProvider(entity.getStorageProvider());
        dto.setStorageKey(entity.getStorageKey());
        dto.setDescription(entity.getDescription());
        dto.setTag(entity.getTag());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getLead() != null) {
            dto.setLeadId(entity.getLead().getId());
        }
        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
        }

        return dto;
    }
}
