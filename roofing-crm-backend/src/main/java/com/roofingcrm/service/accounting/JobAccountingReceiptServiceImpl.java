package com.roofingcrm.service.accounting;

import com.roofingcrm.api.v1.accounting.CreateCostFromReceiptRequest;
import com.roofingcrm.api.v1.accounting.CreateJobCostEntryRequest;
import com.roofingcrm.api.v1.accounting.JobCostEntryDto;
import com.roofingcrm.api.v1.accounting.JobReceiptDto;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.AttachmentTag;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.JobCostEntryRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.audit.AuditSupport;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.storage.AttachmentStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class JobAccountingReceiptServiceImpl implements JobAccountingReceiptService {

    private static final Set<UserRole> MUTATION_ROLES = Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES);

    private final TenantAccessService tenantAccessService;
    private final AttachmentRepository attachmentRepository;
    private final JobRepository jobRepository;
    private final JobCostEntryRepository jobCostEntryRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final ActivityEventService activityEventService;
    private final JobAccountingService jobAccountingService;

    @Autowired
    public JobAccountingReceiptServiceImpl(TenantAccessService tenantAccessService,
                                           AttachmentRepository attachmentRepository,
                                           JobRepository jobRepository,
                                           JobCostEntryRepository jobCostEntryRepository,
                                           AttachmentStorageService attachmentStorageService,
                                           ActivityEventService activityEventService,
                                           JobAccountingService jobAccountingService) {
        this.tenantAccessService = tenantAccessService;
        this.attachmentRepository = attachmentRepository;
        this.jobRepository = jobRepository;
        this.jobCostEntryRepository = jobCostEntryRepository;
        this.attachmentStorageService = attachmentStorageService;
        this.activityEventService = activityEventService;
        this.jobAccountingService = jobAccountingService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobReceiptDto> listReceiptsForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Job job = loadJob(jobId, tenant);
        return attachmentRepository.findByTenantAndJobAndTagAndArchivedFalseOrderByCreatedAtDesc(tenant, job, AttachmentTag.RECEIPT)
                .stream()
                .map(this::toReceiptDto)
                .toList();
    }

    @Override
    public JobReceiptDto uploadReceiptForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, MultipartFile file, String description) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage receipts.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Job job = loadJob(jobId, tenant);
        validateReceiptFile(file);

        Attachment receipt = new Attachment();
        receipt.setTenant(tenant);
        receipt.setJob(job);
        receipt.setFileName(file.getOriginalFilename());
        receipt.setContentType(file.getContentType());
        receipt.setFileSize(file.getSize());
        receipt.setStorageProvider("LOCAL");
        receipt.setTag(AttachmentTag.RECEIPT);
        receipt.setDescription(normalizeOptionalText(description));
        AuditSupport.touchForCreate(receipt, userId);

        receipt = attachmentRepository.save(receipt);
        String tenantSlug = tenant.getSlug() != null ? tenant.getSlug() : tenant.getId().toString();
        String storageKey = attachmentStorageService.store(tenantSlug, receipt.getId(), file);
        receipt.setStorageKey(storageKey);
        receipt = attachmentRepository.save(receipt);

        recordReceiptEvent(tenant, userId, job, receipt, ActivityEventType.RECEIPT_UPLOADED, "Receipt uploaded");
        return toReceiptDto(receipt);
    }

    @Override
    public JobCostEntryDto createCostFromReceipt(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId,
                                                 CreateCostFromReceiptRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage receipts.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Attachment receipt = loadReceipt(receiptId, jobId, tenant);

        CreateJobCostEntryRequest createRequest = new CreateJobCostEntryRequest();
        createRequest.setCategory(request.getCategory());
        createRequest.setVendorName(request.getVendorName());
        createRequest.setDescription(request.getDescription());
        createRequest.setAmount(request.getAmount());
        createRequest.setIncurredAt(request.getIncurredAt());
        createRequest.setNotes(request.getNotes());

        JobCostEntryDto createdCost = jobAccountingService.createJobCostEntry(tenantId, userId, jobId, createRequest);
        JobCostEntry costEntry = loadCostEntry(createdCost.getId(), jobId, tenant);

        AuditSupport.touchForUpdate(receipt, userId);
        receipt.setJobCostEntry(costEntry);
        attachmentRepository.save(receipt);

        recordReceiptEvent(tenant, userId, receipt.getJob(), receipt, ActivityEventType.RECEIPT_LINKED_TO_COST,
                "Receipt linked to cost entry");
        return createdCost;
    }

    @Override
    public JobReceiptDto linkReceiptToCost(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId, UUID costEntryId) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage receipts.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Attachment receipt = loadReceipt(receiptId, jobId, tenant);
        JobCostEntry costEntry = loadCostEntry(costEntryId, jobId, tenant);

        AuditSupport.touchForUpdate(receipt, userId);
        receipt.setJobCostEntry(costEntry);
        Attachment saved = attachmentRepository.save(receipt);

        recordReceiptEvent(tenant, userId, receipt.getJob(), saved, ActivityEventType.RECEIPT_LINKED_TO_COST,
                "Receipt linked to cost entry");
        return toReceiptDto(saved);
    }

    @Override
    public JobReceiptDto unlinkReceiptFromCost(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage receipts.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Attachment receipt = loadReceipt(receiptId, jobId, tenant);

        AuditSupport.touchForUpdate(receipt, userId);
        receipt.setJobCostEntry(null);
        Attachment saved = attachmentRepository.save(receipt);

        recordReceiptEvent(tenant, userId, receipt.getJob(), saved, ActivityEventType.RECEIPT_UNLINKED_FROM_COST,
                "Receipt unlinked from cost entry");
        return toReceiptDto(saved);
    }

    @Override
    public void deleteReceipt(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage receipts.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Attachment receipt = loadReceipt(receiptId, jobId, tenant);

        AuditSupport.touchForUpdate(receipt, userId);
        receipt.setArchived(true);
        receipt.setArchivedAt(Instant.now());
        attachmentRepository.save(receipt);

        recordReceiptEvent(tenant, userId, receipt.getJob(), receipt, ActivityEventType.RECEIPT_DELETED, "Receipt deleted");
    }

    private Job loadJob(UUID jobId, Tenant tenant) {
        return jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private Attachment loadReceipt(UUID receiptId, UUID jobId, Tenant tenant) {
        Attachment receipt = attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found"));
        if (receipt.getTag() != AttachmentTag.RECEIPT) {
            throw new ResourceNotFoundException("Receipt not found");
        }
        return receipt;
    }

    private JobCostEntry loadCostEntry(UUID costEntryId, UUID jobId, Tenant tenant) {
        return jobCostEntryRepository.findByIdAndJobIdAndTenantAndArchivedFalse(costEntryId, jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Cost entry not found"));
    }

    private void validateReceiptFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        if (contentType.equals("application/pdf") || contentType.startsWith("image/")) {
            return;
        }
        throw new IllegalArgumentException("Receipt must be an image or PDF");
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void recordReceiptEvent(Tenant tenant,
                                    UUID userId,
                                    Job job,
                                    Attachment receipt,
                                    ActivityEventType eventType,
                                    String message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("receiptId", receipt.getId() != null ? receipt.getId().toString() : null);
        metadata.put("fileName", receipt.getFileName());
        metadata.put("linkedCostEntryId", receipt.getJobCostEntry() != null && receipt.getJobCostEntry().getId() != null
                ? receipt.getJobCostEntry().getId().toString()
                : null);
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(job.getId()), eventType, message, metadata);
    }

    private JobReceiptDto toReceiptDto(Attachment attachment) {
        JobReceiptDto dto = new JobReceiptDto();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setContentType(attachment.getContentType());
        dto.setFileSize(attachment.getFileSize());
        dto.setDescription(attachment.getDescription());
        dto.setUploadedAt(attachment.getCreatedAt());
        if (attachment.getJobCostEntry() != null) {
            dto.setLinkedCostEntryId(attachment.getJobCostEntry().getId());
            dto.setLinkedCostEntryDescription(attachment.getJobCostEntry().getDescription());
            dto.setLinkedCostEntryAmount(attachment.getJobCostEntry().getAmount());
        }
        return dto;
    }
}
