package com.roofingcrm.service.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.v1.accounting.CreateCostFromReceiptRequest;
import com.roofingcrm.api.v1.accounting.JobCostEntryDto;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.AttachmentTag;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptExtractionStatus;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.JobCostEntryRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.storage.AttachmentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class JobAccountingReceiptServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private JobCostEntryRepository jobCostEntryRepository;
    @Mock
    private AttachmentStorageService attachmentStorageService;
    @Mock
    private ActivityEventService activityEventService;
    @Mock
    private JobAccountingService jobAccountingService;
    @Mock
    private ReceiptExtractionService receiptExtractionService;

    private JobAccountingReceiptServiceImpl service;
    private UUID tenantId;
    private UUID userId;
    private UUID jobId;
    private UUID receiptId;
    private UUID costEntryId;
    private Tenant tenant;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new JobAccountingReceiptServiceImpl(
                tenantAccessService,
                attachmentRepository,
                jobRepository,
                jobCostEntryRepository,
                attachmentStorageService,
                activityEventService,
                jobAccountingService,
                receiptExtractionService,
                new ObjectMapper());

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        receiptId = UUID.randomUUID();
        costEntryId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setSlug("tenant-slug");

        job = new Job();
        job.setId(jobId);
        job.setTenant(tenant);
    }

    @Test
    void listReceiptsForJob_returnsReceiptDtos() {
        Attachment receipt = new Attachment();
        receipt.setId(receiptId);
        receipt.setTenant(tenant);
        receipt.setJob(job);
        receipt.setTag(AttachmentTag.RECEIPT);
        receipt.setFileName("receipt.pdf");
        receipt.setContentType("application/pdf");
        receipt.setFileSize(1024L);
        receipt.setCreatedAt(Instant.parse("2026-03-28T12:00:00Z"));

        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));
        when(attachmentRepository.findByTenantAndJobAndTagAndArchivedFalseOrderByCreatedAtDesc(tenant, job, AttachmentTag.RECEIPT))
                .thenReturn(List.of(receipt));

        var result = service.listReceiptsForJob(tenantId, userId, jobId);

        assertEquals(1, result.size());
        assertEquals("receipt.pdf", result.getFirst().getFileName());
    }

    @Test
    void uploadReceiptForJob_savesAttachmentAndEmitsActivity() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "pdf".getBytes());

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> {
            Attachment attachment = invocation.getArgument(0);
            if (attachment.getId() == null) {
                attachment.setId(receiptId);
                attachment.setCreatedAt(Instant.parse("2026-03-28T12:00:00Z"));
            }
            return attachment;
        });
        when(attachmentStorageService.store(anyString(), eq(receiptId), any())).thenReturn("tenant-slug/receipt.pdf");

        var result = service.uploadReceiptForJob(tenantId, userId, jobId, file, "Materials receipt");

        assertEquals(receiptId, result.getId());
        assertEquals("receipt.pdf", result.getFileName());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.RECEIPT_UPLOADED), anyString(), any());
    }

    @Test
    void createCostFromReceipt_createsCostAndLinksReceipt() {
        Attachment receipt = receipt();
        JobCostEntry costEntry = costEntry();

        CreateCostFromReceiptRequest request = new CreateCostFromReceiptRequest();
        request.setCategory(JobCostCategory.MATERIAL);
        request.setVendorName("ABC Supply");
        request.setDescription("Shingles");
        request.setAmount(new BigDecimal("350.00"));
        request.setIncurredAt(Instant.parse("2026-03-28T12:00:00Z"));

        JobCostEntryDto createdCost = new JobCostEntryDto();
        createdCost.setId(costEntryId);
        createdCost.setJobId(jobId);
        createdCost.setDescription("Shingles");
        createdCost.setAmount(new BigDecimal("350.00"));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant))
                .thenReturn(Optional.of(receipt));
        when(jobAccountingService.createJobCostEntry(eq(tenantId), eq(userId), eq(jobId), any()))
                .thenReturn(createdCost);
        when(jobCostEntryRepository.findByIdAndJobIdAndTenantAndArchivedFalse(costEntryId, jobId, tenant))
                .thenReturn(Optional.of(costEntry));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createCostFromReceipt(tenantId, userId, jobId, receiptId, request);

        assertEquals(costEntryId, result.getId());
        assertEquals(costEntryId, receipt.getJobCostEntry().getId());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.RECEIPT_LINKED_TO_COST), anyString(), any());
    }

    @Test
    void extractReceipt_successStoresDraftWithoutCreatingCost() {
        Attachment receipt = receipt();

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant))
                .thenReturn(Optional.of(receipt));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptExtractionService.extractReceipt(receipt)).thenReturn(new ReceiptExtractionService.ExtractionDraft(
                ReceiptExtractionStatus.COMPLETED,
                Instant.parse("2026-03-29T12:00:00Z"),
                null,
                "ABC Supply",
                Instant.parse("2026-03-28T12:00:00Z"),
                new BigDecimal("98.76"),
                new BigDecimal("90.00"),
                new BigDecimal("8.76"),
                new BigDecimal("98.76"),
                new BigDecimal("98.76"),
                new BigDecimal("98.76"),
                ReceiptFieldConfidence.HIGH,
                ReceiptFieldConfidence.HIGH,
                ReceiptFieldConfidence.HIGH,
                ReceiptFieldConfidence.MEDIUM,
                new BigDecimal("90.00"),
                new BigDecimal("8.76"),
                new BigDecimal("98.76"),
                new BigDecimal("98.76"),
                List.of(new BigDecimal("98.76"), new BigDecimal("95.76")),
                ReceiptAmountConfidence.MEDIUM,
                JobCostCategory.MATERIAL,
                "Store receipt for shingles",
                91,
                "TOTAL 98.76",
                "SUMMARY TOTAL 98.76",
                List.of("Multiple possible totals detected. Please confirm before saving."),
                null
        ));

        var result = service.extractReceipt(tenantId, userId, jobId, receiptId);

        assertEquals(ReceiptExtractionStatus.COMPLETED, result.getStatus());
        assertEquals("ABC Supply", result.getResult().getVendorName());
        assertEquals(new BigDecimal("98.76"), result.getResult().getAmount());
        assertEquals(new BigDecimal("90.00"), result.getResult().getExtractedSubtotal());
        assertEquals(new BigDecimal("8.76"), result.getResult().getExtractedTax());
        assertEquals(new BigDecimal("98.76"), result.getResult().getExtractedTotal());
        assertEquals(new BigDecimal("98.76"), result.getResult().getExtractedAmountPaid());
        assertEquals(new BigDecimal("98.76"), result.getResult().getComputedTotal());
        assertEquals(new BigDecimal("98.76"), result.getResult().getSummaryRegionTotal());
        assertEquals(ReceiptFieldConfidence.HIGH, result.getResult().getTotalConfidence());
        assertEquals(2, result.getResult().getAmountCandidates().size());
        assertEquals(ReceiptAmountConfidence.MEDIUM, result.getResult().getAmountConfidence());
        verify(jobAccountingService, never()).createJobCostEntry(any(), any(), any(), any());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.RECEIPT_EXTRACTION_COMPLETED), anyString(), any());
    }

    @Test
    void extractReceipt_apiResultReflectsReconciledTaxTaxRateAndDateNotSummaryRegionOnly() {
        Attachment receipt = receipt();

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant))
                .thenReturn(Optional.of(receipt));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptExtractionService.extractReceipt(receipt)).thenReturn(new ReceiptExtractionService.ExtractionDraft(
                ReceiptExtractionStatus.COMPLETED,
                Instant.parse("2026-03-29T12:00:00Z"),
                null,
                "Roof Supply Co",
                Instant.parse("2026-04-01T12:00:00Z"),
                new BigDecimal("1109.14"),
                new BigDecimal("1000.00"),
                new BigDecimal("109.14"),
                new BigDecimal("1109.14"),
                new BigDecimal("1109.14"),
                new BigDecimal("1109.14"),
                ReceiptFieldConfidence.HIGH,
                ReceiptFieldConfidence.HIGH,
                ReceiptFieldConfidence.HIGH,
                ReceiptFieldConfidence.MEDIUM,
                new BigDecimal("1000.00"),
                new BigDecimal("108.41"),
                new BigDecimal("1109.14"),
                new BigDecimal("1109.14"),
                List.of(new BigDecimal("1109.14")),
                ReceiptAmountConfidence.HIGH,
                JobCostCategory.MATERIAL,
                "Invoice",
                95,
                "FULL PAGE TEXT",
                "SUMMARY TAX 108.41",
                List.of(),
                new BigDecimal("9.85")
        ));

        var result = service.extractReceipt(tenantId, userId, jobId, receiptId);

        assertEquals(ReceiptExtractionStatus.COMPLETED, result.getStatus());
        assertEquals(Instant.parse("2026-04-01T12:00:00Z"), result.getResult().getIncurredAt());
        assertEquals(new BigDecimal("109.14"), result.getResult().getExtractedTax());
        assertEquals(new BigDecimal("108.41"), result.getResult().getSummaryRegionTax());
        assertEquals(new BigDecimal("9.85"), result.getResult().getExtractedTaxRatePercent());
        assertEquals(new BigDecimal("1000.00"), result.getResult().getExtractedSubtotal());
        assertEquals(new BigDecimal("1109.14"), result.getResult().getExtractedTotal());
    }

    @Test
    void extractReceipt_failureSetsFailedStatus() {
        Attachment receipt = receipt();

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant))
                .thenReturn(Optional.of(receipt));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(receiptExtractionService.extractReceipt(receipt)).thenReturn(new ReceiptExtractionService.ExtractionDraft(
                ReceiptExtractionStatus.FAILED,
                Instant.parse("2026-03-29T12:00:00Z"),
                "We couldn't reliably extract details from this receipt. You can retry or enter it manually.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                ReceiptFieldConfidence.UNKNOWN,
                ReceiptFieldConfidence.UNKNOWN,
                ReceiptFieldConfidence.UNKNOWN,
                ReceiptFieldConfidence.UNKNOWN,
                null,
                null,
                null,
                null,
                List.of(),
                ReceiptAmountConfidence.LOW,
                null,
                null,
                null,
                null,
                null,
                List.of("No reliable total was detected from the extracted text."),
                null
        ));

        var result = service.extractReceipt(tenantId, userId, jobId, receiptId);

        assertEquals(ReceiptExtractionStatus.FAILED, result.getStatus());
        assertEquals("We couldn't reliably extract details from this receipt. You can retry or enter it manually.",
                result.getError());
        verify(jobAccountingService, never()).createJobCostEntry(any(), any(), any(), any());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.RECEIPT_EXTRACTION_FAILED), anyString(), any());
    }

    @Test
    void confirmReceiptCost_reusesCostCreationFlow() {
        Attachment receipt = receipt();
        JobCostEntry costEntry = costEntry();

        var request = new com.roofingcrm.api.v1.accounting.ConfirmReceiptCostRequest();
        request.setCategory(JobCostCategory.MATERIAL);
        request.setVendorName("ABC Supply");
        request.setDescription("Receipt-confirmed cost");
        request.setAmount(new BigDecimal("120.00"));
        request.setIncurredAt(Instant.parse("2026-03-28T12:00:00Z"));

        JobCostEntryDto createdCost = new JobCostEntryDto();
        createdCost.setId(costEntryId);
        createdCost.setJobId(jobId);
        createdCost.setDescription("Receipt-confirmed cost");
        createdCost.setAmount(new BigDecimal("120.00"));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant))
                .thenReturn(Optional.of(receipt));
        when(jobAccountingService.createJobCostEntry(eq(tenantId), eq(userId), eq(jobId), any()))
                .thenReturn(createdCost);
        when(jobCostEntryRepository.findByIdAndJobIdAndTenantAndArchivedFalse(costEntryId, jobId, tenant))
                .thenReturn(Optional.of(costEntry));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.confirmReceiptCost(tenantId, userId, jobId, receiptId, request);

        assertEquals(costEntryId, result.getId());
        assertEquals(costEntryId, receipt.getJobCostEntry().getId());
        verify(jobAccountingService).createJobCostEntry(eq(tenantId), eq(userId), eq(jobId), any());
    }

    @Test
    void unlinkReceiptFromCost_clearsLink() {
        Attachment receipt = receipt();
        receipt.setJobCostEntry(costEntry());

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(attachmentRepository.findByIdAndJobIdAndTenantAndArchivedFalse(receiptId, jobId, tenant))
                .thenReturn(Optional.of(receipt));
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.unlinkReceiptFromCost(tenantId, userId, jobId, receiptId);

        assertNull(receipt.getJobCostEntry());
        assertNull(result.getLinkedCostEntryId());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.RECEIPT_UNLINKED_FROM_COST), anyString(), any());
    }

    @Test
    void uploadReceiptForJob_requiresMutationRole() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "pdf".getBytes());
        doThrow(new RuntimeException("denied"))
                .when(tenantAccessService).requireAnyRole(eq(tenantId), eq(userId), any(), anyString());

        assertThrows(RuntimeException.class, () -> service.uploadReceiptForJob(tenantId, userId, jobId, file, null));
    }

    private Attachment receipt() {
        Attachment receipt = new Attachment();
        receipt.setId(receiptId);
        receipt.setTenant(tenant);
        receipt.setJob(job);
        receipt.setTag(AttachmentTag.RECEIPT);
        receipt.setFileName("receipt.pdf");
        receipt.setContentType("application/pdf");
        receipt.setFileSize(1024L);
        receipt.setCreatedAt(Instant.parse("2026-03-28T12:00:00Z"));
        return receipt;
    }

    private JobCostEntry costEntry() {
        JobCostEntry costEntry = new JobCostEntry();
        costEntry.setId(costEntryId);
        costEntry.setTenant(tenant);
        costEntry.setJob(job);
        costEntry.setCategory(JobCostCategory.MATERIAL);
        costEntry.setDescription("Shingles");
        costEntry.setAmount(new BigDecimal("350.00"));
        costEntry.setIncurredAt(Instant.parse("2026-03-28T12:00:00Z"));
        return costEntry;
    }
}
