package com.roofingcrm.service.accounting;

import com.roofingcrm.api.v1.accounting.CreateJobCostEntryRequest;
import com.roofingcrm.api.v1.accounting.UpdateJobCostEntryRequest;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.domain.repository.JobCostEntryRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class JobAccountingServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private EstimateRepository estimateRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private JobCostEntryRepository jobCostEntryRepository;
    @Mock
    private ActivityEventService activityEventService;

    private JobAccountingServiceImpl service;
    private UUID tenantId;
    private UUID userId;
    private UUID jobId;
    private UUID costEntryId;
    private Tenant tenant;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new JobAccountingServiceImpl(
                tenantAccessService,
                jobRepository,
                estimateRepository,
                invoiceRepository,
                jobCostEntryRepository,
                activityEventService);

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        costEntryId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);

        job = new Job();
        job.setId(jobId);
        job.setTenant(tenant);
    }

    @Test
    void getJobAccountingSummary_combinesAcceptedEstimateInvoicesAndCosts() {
        Estimate acceptedEstimate = new Estimate();
        acceptedEstimate.setId(UUID.randomUUID());
        acceptedEstimate.setTenant(tenant);
        acceptedEstimate.setJob(job);
        acceptedEstimate.setTotal(new BigDecimal("12000.00"));

        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));
        when(estimateRepository.findAcceptedForJobOrderForAccounting(job)).thenReturn(List.of(acceptedEstimate));
        when(invoiceRepository.sumNonVoidTotalForJob(tenant, jobId)).thenReturn(new BigDecimal("15000.00"));
        when(invoiceRepository.sumPaidTotalForJob(tenant, jobId)).thenReturn(new BigDecimal("9000.00"));
        when(jobCostEntryRepository.sumAmountForJob(tenant, jobId)).thenReturn(new BigDecimal("5500.00"));
        when(jobCostEntryRepository.sumAmountsByCategoryForJob(tenant, jobId)).thenReturn(List.of(
                categoryTotal(JobCostCategory.MATERIAL, "3500.00"),
                categoryTotal(JobCostCategory.LABOR, "1200.00")
        ));

        var result = service.getJobAccountingSummary(tenantId, userId, jobId);

        assertEquals(new BigDecimal("12000.00"), result.getAgreedAmount());
        assertEquals(new BigDecimal("15000.00"), result.getInvoicedAmount());
        assertEquals(new BigDecimal("9000.00"), result.getPaidAmount());
        assertEquals(new BigDecimal("5500.00"), result.getTotalCosts());
        assertEquals(new BigDecimal("3500.00"), result.getGrossProfit());
        assertEquals(new BigDecimal("38.89"), result.getMarginPercent());
        assertEquals(new BigDecimal("6500.00"), result.getProjectedProfit());
        assertEquals(new BigDecimal("3500.00"), result.getActualProfit());
        assertEquals(new BigDecimal("54.17"), result.getProjectedMarginPercent());
        assertEquals(new BigDecimal("38.89"), result.getActualMarginPercent());
        assertTrue(result.isHasAcceptedEstimate());
        assertEquals(new BigDecimal("3500.00"), result.getCategoryTotals().get(JobCostCategory.MATERIAL));
        assertEquals(new BigDecimal("1200.00"), result.getCategoryTotals().get(JobCostCategory.LABOR));
        assertEquals(BigDecimal.ZERO, result.getCategoryTotals().get(JobCostCategory.OTHER));
    }

    @Test
    void getJobAccountingSummary_withoutAcceptedEstimateKeepsAgreedValuesNull() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));
        when(estimateRepository.findAcceptedForJobOrderForAccounting(job)).thenReturn(List.of());
        when(invoiceRepository.sumNonVoidTotalForJob(tenant, jobId)).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.sumPaidTotalForJob(tenant, jobId)).thenReturn(BigDecimal.ZERO);
        when(jobCostEntryRepository.sumAmountForJob(tenant, jobId)).thenReturn(new BigDecimal("200.00"));
        when(jobCostEntryRepository.sumAmountsByCategoryForJob(tenant, jobId)).thenReturn(List.of());

        var result = service.getJobAccountingSummary(tenantId, userId, jobId);

        assertNull(result.getAgreedAmount());
        assertEquals(new BigDecimal("-200.00"), result.getGrossProfit());
        assertNull(result.getProjectedProfit());
        assertNull(result.getProjectedMarginPercent());
        assertNull(result.getMarginPercent());
        assertFalse(result.isHasAcceptedEstimate());
    }

    @Test
    void createJobCostEntry_setsAuditFieldsAndRecordsActivity() {
        CreateJobCostEntryRequest request = new CreateJobCostEntryRequest();
        request.setCategory(JobCostCategory.MATERIAL);
        request.setVendorName("ABC Supply");
        request.setDescription("Shingles");
        request.setAmount(new BigDecimal("3500.00"));
        request.setIncurredAt(Instant.parse("2026-03-10T12:00:00Z"));
        request.setNotes("Delivered");

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)).thenReturn(Optional.of(job));
        when(jobCostEntryRepository.save(any(JobCostEntry.class))).thenAnswer(invocation -> {
            JobCostEntry entry = invocation.getArgument(0);
            entry.setId(costEntryId);
            entry.setCreatedAt(Instant.parse("2026-03-10T12:00:00Z"));
            entry.setUpdatedAt(Instant.parse("2026-03-10T12:00:00Z"));
            return entry;
        });

        var result = service.createJobCostEntry(tenantId, userId, jobId, request);

        assertEquals(costEntryId, result.getId());
        assertEquals(jobId, result.getJobId());
        assertEquals(JobCostCategory.MATERIAL, result.getCategory());
        assertEquals("ABC Supply", result.getVendorName());
        assertEquals("Shingles", result.getDescription());
        assertEquals(new BigDecimal("3500.00"), result.getAmount());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.COST_ENTRY_CREATED), anyString(), any());
    }

    @Test
    void updateJobCostEntry_updatesExistingEntry() {
        JobCostEntry entry = new JobCostEntry();
        entry.setId(costEntryId);
        entry.setTenant(tenant);
        entry.setJob(job);
        entry.setCategory(JobCostCategory.OTHER);
        entry.setDescription("Dumpster");
        entry.setAmount(new BigDecimal("400.00"));
        entry.setIncurredAt(Instant.parse("2026-03-08T12:00:00Z"));

        UpdateJobCostEntryRequest request = new UpdateJobCostEntryRequest();
        request.setCategory(JobCostCategory.TRANSPORTATION);
        request.setVendorName("Fleet");
        request.setDescription("Truck rental");
        request.setAmount(new BigDecimal("625.00"));
        request.setIncurredAt(Instant.parse("2026-03-11T12:00:00Z"));
        request.setNotes("Two days");

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobCostEntryRepository.findByIdAndJobIdAndTenantAndArchivedFalse(costEntryId, jobId, tenant))
                .thenReturn(Optional.of(entry));
        when(jobCostEntryRepository.save(any(JobCostEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateJobCostEntry(tenantId, userId, jobId, costEntryId, request);

        assertEquals(JobCostCategory.TRANSPORTATION, result.getCategory());
        assertEquals("Fleet", result.getVendorName());
        assertEquals("Truck rental", result.getDescription());
        assertEquals(new BigDecimal("625.00"), result.getAmount());
        assertEquals(userId, entry.getUpdatedByUserId());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.COST_ENTRY_UPDATED), anyString(), any());
    }

    @Test
    void deleteJobCostEntry_softDeletesAndRecordsActivity() {
        JobCostEntry entry = new JobCostEntry();
        entry.setId(costEntryId);
        entry.setTenant(tenant);
        entry.setJob(job);
        entry.setCategory(JobCostCategory.LABOR);
        entry.setDescription("Install crew");
        entry.setAmount(new BigDecimal("900.00"));
        entry.setIncurredAt(Instant.parse("2026-03-09T12:00:00Z"));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(jobCostEntryRepository.findByIdAndJobIdAndTenantAndArchivedFalse(costEntryId, jobId, tenant))
                .thenReturn(Optional.of(entry));
        when(jobCostEntryRepository.save(any(JobCostEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteJobCostEntry(tenantId, userId, jobId, costEntryId);

        assertTrue(entry.isArchived());
        assertNotNull(entry.getArchivedAt());
        assertEquals(userId, entry.getUpdatedByUserId());
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(jobId),
                eq(ActivityEventType.COST_ENTRY_DELETED), anyString(), any());
    }

    private JobCostEntryRepository.CategoryTotalView categoryTotal(JobCostCategory category, String amount) {
        return new JobCostEntryRepository.CategoryTotalView() {
            @Override
            public JobCostCategory getCategory() {
                return category;
            }

            @Override
            public BigDecimal getTotalAmount() {
                return new BigDecimal(amount);
            }
        };
    }
}
