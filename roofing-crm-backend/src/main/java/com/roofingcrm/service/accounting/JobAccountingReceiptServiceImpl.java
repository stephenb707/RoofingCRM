package com.roofingcrm.service.accounting;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.v1.accounting.ConfirmReceiptCostRequest;
import com.roofingcrm.api.v1.accounting.CreateCostFromReceiptRequest;
import com.roofingcrm.api.v1.accounting.CreateJobCostEntryRequest;
import com.roofingcrm.api.v1.accounting.ExtractReceiptResponseDto;
import com.roofingcrm.api.v1.accounting.JobCostEntryDto;
import com.roofingcrm.api.v1.accounting.JobReceiptDto;
import com.roofingcrm.api.v1.accounting.ReceiptExtractionResultDto;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.AttachmentTag;
import com.roofingcrm.domain.enums.ReceiptExtractionStatus;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.JobCostEntryRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.audit.AuditSupport;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.storage.AttachmentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class JobAccountingReceiptServiceImpl implements JobAccountingReceiptService {

    private static final Logger log = LoggerFactory.getLogger(JobAccountingReceiptServiceImpl.class);

    private static final Set<UserRole> MUTATION_ROLES = Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES);

    private final TenantAccessService tenantAccessService;
    private final AttachmentRepository attachmentRepository;
    private final JobRepository jobRepository;
    private final JobCostEntryRepository jobCostEntryRepository;
    private final AttachmentStorageService attachmentStorageService;
    private final ActivityEventService activityEventService;
    private final JobAccountingService jobAccountingService;
    private final ReceiptExtractionService receiptExtractionService;
    private final ObjectMapper objectMapper;

    @Autowired
    public JobAccountingReceiptServiceImpl(TenantAccessService tenantAccessService,
                                           AttachmentRepository attachmentRepository,
                                           JobRepository jobRepository,
                                           JobCostEntryRepository jobCostEntryRepository,
                                           AttachmentStorageService attachmentStorageService,
                                           ActivityEventService activityEventService,
                                           JobAccountingService jobAccountingService,
                                           ReceiptExtractionService receiptExtractionService,
                                           ObjectMapper objectMapper) {
        this.tenantAccessService = tenantAccessService;
        this.attachmentRepository = attachmentRepository;
        this.jobRepository = jobRepository;
        this.jobCostEntryRepository = jobCostEntryRepository;
        this.attachmentStorageService = attachmentStorageService;
        this.activityEventService = activityEventService;
        this.jobAccountingService = jobAccountingService;
        this.receiptExtractionService = receiptExtractionService;
        this.objectMapper = objectMapper;
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
    public ExtractReceiptResponseDto extractReceipt(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage receipts.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Attachment receipt = loadReceipt(receiptId, jobId, tenant);

        AuditSupport.touchForUpdate(receipt, userId);
        receipt.setExtractionStatus(ReceiptExtractionStatus.PROCESSING);
        receipt.setExtractionError(null);
        attachmentRepository.save(receipt);

        ReceiptExtractionService.ExtractionDraft extractionDraft = receiptExtractionService.extractReceipt(receipt);
        applyExtractionDraft(receipt, extractionDraft, userId);
        if (receipt.getExtractionStatus() == ReceiptExtractionStatus.COMPLETED) {
            log.info(
                    "Receipt extraction persisted for {}: extractedSubtotal={}, extractedTax={}, extractedTotal={}, extractedTaxRatePercent={}, extractedIncurredAt={}",
                    receipt.getId(),
                    receipt.getExtractedSubtotal(),
                    receipt.getExtractedTax(),
                    receipt.getExtractedTotal(),
                    receipt.getExtractedTaxRatePercent(),
                    receipt.getExtractedIncurredAt());
        }
        Attachment saved = attachmentRepository.save(receipt);

        if (saved.getExtractionStatus() == ReceiptExtractionStatus.COMPLETED) {
            recordReceiptEvent(tenant, userId, receipt.getJob(), saved, ActivityEventType.RECEIPT_EXTRACTION_COMPLETED,
                    "Receipt details extracted");
        } else if (saved.getExtractionStatus() == ReceiptExtractionStatus.FAILED) {
            recordReceiptEvent(tenant, userId, receipt.getJob(), saved, ActivityEventType.RECEIPT_EXTRACTION_FAILED,
                    "Receipt extraction failed");
        }

        return toExtractionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ExtractReceiptResponseDto getReceiptExtraction(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Attachment receipt = loadReceipt(receiptId, jobId, tenant);
        return toExtractionResponse(receipt);
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
    public JobCostEntryDto confirmReceiptCost(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId,
                                              ConfirmReceiptCostRequest request) {
        return createCostFromReceipt(tenantId, userId, jobId, receiptId, request);
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

    private void applyExtractionDraft(Attachment receipt,
                                      ReceiptExtractionService.ExtractionDraft draft,
                                      UUID userId) {
        AuditSupport.touchForUpdate(receipt, userId);
        receipt.setExtractionStatus(draft.status());
        receipt.setExtractedAt(draft.extractedAt());
        receipt.setExtractionError(normalizeOptionalText(draft.error()));
        receipt.setExtractedVendorName(normalizeOptionalText(draft.vendorName()));
        receipt.setExtractedIncurredAt(draft.incurredAt());
        receipt.setExtractedAmount(draft.amount());
        receipt.setExtractedSubtotal(draft.extractedSubtotal());
        receipt.setExtractedTax(draft.extractedTax());
        receipt.setExtractedTotal(draft.extractedTotal());
        receipt.setExtractedAmountPaid(draft.extractedAmountPaid());
        receipt.setComputedTotal(draft.computedTotal());
        receipt.setSubtotalConfidence(draft.subtotalConfidence());
        receipt.setTaxConfidence(draft.taxConfidence());
        receipt.setTotalConfidence(draft.totalConfidence());
        receipt.setAmountPaidConfidence(draft.amountPaidConfidence());
        receipt.setSummaryRegionSubtotal(draft.summaryRegionSubtotal());
        receipt.setSummaryRegionTax(draft.summaryRegionTax());
        receipt.setSummaryRegionTotal(draft.summaryRegionTotal());
        receipt.setSummaryRegionAmountPaid(draft.summaryRegionAmountPaid());
        receipt.setExtractedAmountCandidatesJson(writeBigDecimalList(draft.amountCandidates()));
        receipt.setExtractedAmountConfidence(draft.amountConfidence());
        receipt.setExtractedSuggestedCategory(draft.suggestedCategory());
        receipt.setExtractedNotes(normalizeOptionalText(draft.notes()));
        receipt.setExtractionConfidence(draft.confidence());
        receipt.setExtractedRawText(normalizeOptionalText(draft.rawExtractedText()));
        receipt.setSummaryRegionRawText(normalizeOptionalText(draft.summaryRegionRawText()));
        receipt.setExtractedTaxRatePercent(draft.extractedTaxRatePercent());
        receipt.setExtractedWarningsJson(writeStringList(draft.extractionWarnings()));
    }

    private JobReceiptDto toReceiptDto(Attachment attachment) {
        JobReceiptDto dto = new JobReceiptDto();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setContentType(attachment.getContentType());
        dto.setFileSize(attachment.getFileSize());
        dto.setDescription(attachment.getDescription());
        dto.setUploadedAt(attachment.getCreatedAt());
        dto.setExtractionStatus(attachment.getExtractionStatus());
        dto.setExtractedAt(attachment.getExtractedAt());
        dto.setExtractionError(attachment.getExtractionError());
        dto.setExtractionConfidence(attachment.getExtractionConfidence());
        dto.setExtractionResult(toExtractionResultDto(attachment));
        if (attachment.getJobCostEntry() != null) {
            dto.setLinkedCostEntryId(attachment.getJobCostEntry().getId());
            dto.setLinkedCostEntryDescription(attachment.getJobCostEntry().getDescription());
            dto.setLinkedCostEntryAmount(attachment.getJobCostEntry().getAmount());
        }
        return dto;
    }

    private ExtractReceiptResponseDto toExtractionResponse(Attachment attachment) {
        ExtractReceiptResponseDto dto = new ExtractReceiptResponseDto();
        dto.setReceiptId(attachment.getId());
        dto.setStatus(attachment.getExtractionStatus());
        dto.setExtractedAt(attachment.getExtractedAt());
        dto.setError(attachment.getExtractionError());
        dto.setConfidence(attachment.getExtractionConfidence());
        dto.setResult(toExtractionResultDto(attachment));
        return dto;
    }

    private ReceiptExtractionResultDto toExtractionResultDto(Attachment attachment) {
        if (attachment.getExtractedVendorName() == null
                && attachment.getExtractedIncurredAt() == null
                && attachment.getExtractedAmount() == null
                && attachment.getExtractedSubtotal() == null
                && attachment.getExtractedTax() == null
                && attachment.getExtractedTotal() == null
                && attachment.getExtractedAmountPaid() == null
                && attachment.getComputedTotal() == null
                && attachment.getSubtotalConfidence() == null
                && attachment.getTaxConfidence() == null
                && attachment.getTotalConfidence() == null
                && attachment.getAmountPaidConfidence() == null
                && attachment.getSummaryRegionSubtotal() == null
                && attachment.getSummaryRegionTax() == null
                && attachment.getSummaryRegionTotal() == null
                && attachment.getSummaryRegionAmountPaid() == null
                && attachment.getExtractedAmountCandidatesJson() == null
                && attachment.getExtractedAmountConfidence() == null
                && attachment.getExtractedSuggestedCategory() == null
                && attachment.getExtractedNotes() == null
                && attachment.getExtractionConfidence() == null
                && attachment.getExtractedRawText() == null
                && attachment.getSummaryRegionRawText() == null
                && attachment.getExtractedWarningsJson() == null
                && attachment.getExtractedTaxRatePercent() == null) {
            return null;
        }
        ReceiptExtractionResultDto dto = new ReceiptExtractionResultDto();
        dto.setVendorName(attachment.getExtractedVendorName());
        dto.setIncurredAt(attachment.getExtractedIncurredAt());
        dto.setAmount(attachment.getExtractedAmount());
        dto.setExtractedSubtotal(attachment.getExtractedSubtotal());
        dto.setExtractedTax(attachment.getExtractedTax());
        dto.setExtractedTotal(attachment.getExtractedTotal());
        dto.setExtractedAmountPaid(attachment.getExtractedAmountPaid());
        dto.setComputedTotal(attachment.getComputedTotal());
        dto.setSubtotalConfidence(attachment.getSubtotalConfidence());
        dto.setTaxConfidence(attachment.getTaxConfidence());
        dto.setTotalConfidence(attachment.getTotalConfidence());
        dto.setAmountPaidConfidence(attachment.getAmountPaidConfidence());
        dto.setSummaryRegionSubtotal(attachment.getSummaryRegionSubtotal());
        dto.setSummaryRegionTax(attachment.getSummaryRegionTax());
        dto.setSummaryRegionTotal(attachment.getSummaryRegionTotal());
        dto.setSummaryRegionAmountPaid(attachment.getSummaryRegionAmountPaid());
        dto.setAmountCandidates(readBigDecimalList(attachment.getExtractedAmountCandidatesJson()));
        dto.setAmountConfidence(attachment.getExtractedAmountConfidence());
        dto.setSuggestedCategory(attachment.getExtractedSuggestedCategory());
        dto.setNotes(attachment.getExtractedNotes());
        dto.setConfidence(attachment.getExtractionConfidence());
        dto.setRawExtractedText(attachment.getExtractedRawText());
        dto.setSummaryRegionRawText(attachment.getSummaryRegionRawText());
        dto.setExtractionWarnings(readStringList(attachment.getExtractedWarningsJson()));
        dto.setExtractedTaxRatePercent(attachment.getExtractedTaxRatePercent());
        return dto;
    }

    private String writeBigDecimalList(List<java.math.BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to store extracted amount candidates.", ex);
        }
    }

    private List<java.math.BigDecimal> readBigDecimalList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<java.math.BigDecimal> parsed = objectMapper.readValue(value, new TypeReference<List<java.math.BigDecimal>>() {
            });
            return parsed == null ? List.of() : parsed;
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeStringList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to store extraction warnings.", ex);
        }
    }

    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            List<String> parsed = objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
            if (parsed == null) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (String item : parsed) {
                String normalized = normalizeOptionalText(item);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
            return Collections.unmodifiableList(result);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
