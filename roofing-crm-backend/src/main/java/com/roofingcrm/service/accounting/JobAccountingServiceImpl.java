package com.roofingcrm.service.accounting;

import com.roofingcrm.api.v1.accounting.CreateJobCostEntryRequest;
import com.roofingcrm.api.v1.accounting.JobAccountingSummaryDto;
import com.roofingcrm.api.v1.accounting.JobCostEntryDto;
import com.roofingcrm.api.v1.accounting.UpdateJobCostEntryRequest;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.domain.repository.JobCostEntryRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.audit.AuditSupport;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class JobAccountingServiceImpl implements JobAccountingService {

    private static final Set<UserRole> MUTATION_ROLES = Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES);

    private final TenantAccessService tenantAccessService;
    private final JobRepository jobRepository;
    private final EstimateRepository estimateRepository;
    private final InvoiceRepository invoiceRepository;
    private final JobCostEntryRepository jobCostEntryRepository;
    private final ActivityEventService activityEventService;

    @Autowired
    public JobAccountingServiceImpl(TenantAccessService tenantAccessService,
                                    JobRepository jobRepository,
                                    EstimateRepository estimateRepository,
                                    InvoiceRepository invoiceRepository,
                                    JobCostEntryRepository jobCostEntryRepository,
                                    ActivityEventService activityEventService) {
        this.tenantAccessService = tenantAccessService;
        this.jobRepository = jobRepository;
        this.estimateRepository = estimateRepository;
        this.invoiceRepository = invoiceRepository;
        this.jobCostEntryRepository = jobCostEntryRepository;
        this.activityEventService = activityEventService;
    }

    @Override
    @Transactional(readOnly = true)
    public JobAccountingSummaryDto getJobAccountingSummary(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Job job = loadJob(jobId, tenant);

        Estimate acceptedEstimate = estimateRepository.findAcceptedForJobOrderForAccounting(job).stream()
                .findFirst()
                .orElse(null);

        BigDecimal agreedAmount = acceptedEstimate != null ? acceptedEstimate.getTotal() : null;
        BigDecimal invoicedAmount = defaultMoney(invoiceRepository.sumNonVoidTotalForJob(tenant, jobId));
        BigDecimal paidAmount = defaultMoney(invoiceRepository.sumPaidTotalForJob(tenant, jobId));
        BigDecimal totalCosts = defaultMoney(jobCostEntryRepository.sumAmountForJob(tenant, jobId));
        BigDecimal actualProfit = paidAmount.subtract(totalCosts);
        BigDecimal projectedProfit = agreedAmount != null ? agreedAmount.subtract(totalCosts) : null;
        BigDecimal actualMarginPercent = calculateMarginPercent(actualProfit, paidAmount);
        BigDecimal projectedMarginPercent = calculateMarginPercent(projectedProfit, agreedAmount);

        JobAccountingSummaryDto dto = new JobAccountingSummaryDto();
        dto.setAgreedAmount(agreedAmount);
        dto.setInvoicedAmount(invoicedAmount);
        dto.setPaidAmount(paidAmount);
        dto.setTotalCosts(totalCosts);
        dto.setGrossProfit(actualProfit);
        dto.setMarginPercent(actualMarginPercent);
        dto.setProjectedProfit(projectedProfit);
        dto.setActualProfit(actualProfit);
        dto.setProjectedMarginPercent(projectedMarginPercent);
        dto.setActualMarginPercent(actualMarginPercent);
        dto.setCategoryTotals(buildCategoryTotals(tenant, jobId));
        dto.setHasAcceptedEstimate(acceptedEstimate != null);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobCostEntryDto> listJobCostEntries(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Job job = loadJob(jobId, tenant);
        return jobCostEntryRepository.findByJobAndTenantAndArchivedFalseOrderByIncurredAtDescCreatedAtDesc(job, tenant)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public JobCostEntryDto createJobCostEntry(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, CreateJobCostEntryRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage job costs.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Job job = loadJob(jobId, tenant);

        JobCostEntry entry = new JobCostEntry();
        entry.setTenant(tenant);
        entry.setJob(job);
        AuditSupport.touchForCreate(entry, userId);
        applyCreateRequest(entry, request);

        JobCostEntry saved = jobCostEntryRepository.save(entry);
        recordCostEvent(tenant, userId, job, saved, ActivityEventType.COST_ENTRY_CREATED, "Cost entry added");
        return toDto(saved);
    }

    @Override
    public JobCostEntryDto updateJobCostEntry(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID costEntryId, UpdateJobCostEntryRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage job costs.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        JobCostEntry entry = Objects.requireNonNull(loadCostEntry(costEntryId, jobId, tenant));

        AuditSupport.touchForUpdate(entry, userId);
        applyUpdateRequest(entry, request);

        JobCostEntry saved = jobCostEntryRepository.save(entry);
        recordCostEvent(tenant, userId, entry.getJob(), saved, ActivityEventType.COST_ENTRY_UPDATED, "Cost entry updated");
        return toDto(saved);
    }

    @Override
    public void deleteJobCostEntry(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID costEntryId) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(MUTATION_ROLES),
                "You do not have permission to manage job costs.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        JobCostEntry entry = loadCostEntry(costEntryId, jobId, tenant);

        AuditSupport.touchForUpdate(entry, userId);
        entry.setArchived(true);
        entry.setArchivedAt(Instant.now());
        jobCostEntryRepository.save(entry);

        recordCostEvent(tenant, userId, entry.getJob(), entry, ActivityEventType.COST_ENTRY_DELETED, "Cost entry deleted");
    }

    private Job loadJob(UUID jobId, Tenant tenant) {
        return jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private JobCostEntry loadCostEntry(UUID costEntryId, UUID jobId, Tenant tenant) {
        return jobCostEntryRepository.findByIdAndJobIdAndTenantAndArchivedFalse(costEntryId, jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Cost entry not found"));
    }

    private void applyCreateRequest(JobCostEntry entry, CreateJobCostEntryRequest request) {
        entry.setCategory(Objects.requireNonNull(request.getCategory(), "category is required"));
        entry.setDescription(normalizeRequiredText(request.getDescription(), "description is required"));
        entry.setAmount(requireNonNegative(request.getAmount()));
        entry.setIncurredAt(Objects.requireNonNull(request.getIncurredAt(), "incurredAt is required"));
        entry.setVendorName(normalizeOptionalText(request.getVendorName()));
        entry.setNotes(normalizeOptionalText(request.getNotes()));
    }

    private void applyUpdateRequest(JobCostEntry entry, UpdateJobCostEntryRequest request) {
        if (request.getCategory() != null) {
            entry.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            entry.setDescription(normalizeRequiredText(request.getDescription(), "description must not be blank"));
        }
        if (request.getAmount() != null) {
            entry.setAmount(requireNonNegative(request.getAmount()));
        }
        if (request.getIncurredAt() != null) {
            entry.setIncurredAt(request.getIncurredAt());
        }
        if (request.getVendorName() != null) {
            entry.setVendorName(normalizeOptionalText(request.getVendorName()));
        }
        if (request.getNotes() != null) {
            entry.setNotes(normalizeOptionalText(request.getNotes()));
        }
    }

    private BigDecimal requireNonNegative(BigDecimal amount) {
        BigDecimal value = Objects.requireNonNull(amount, "amount is required");
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must be greater than or equal to 0");
        }
        return value;
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal calculateMarginPercent(BigDecimal profit, BigDecimal baseAmount) {
        if (profit == null || baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return profit.multiply(BigDecimal.valueOf(100))
                .divide(baseAmount, 2, RoundingMode.HALF_UP);
    }

    private Map<JobCostCategory, BigDecimal> buildCategoryTotals(Tenant tenant, UUID jobId) {
        Map<JobCostCategory, BigDecimal> totals = new LinkedHashMap<>();
        for (JobCostCategory category : JobCostCategory.values()) {
            totals.put(category, BigDecimal.ZERO);
        }
        for (JobCostEntryRepository.CategoryTotalView totalView : jobCostEntryRepository.sumAmountsByCategoryForJob(tenant, jobId)) {
            totals.put(totalView.getCategory(), defaultMoney(totalView.getTotalAmount()));
        }
        return totals;
    }

    private void recordCostEvent(Tenant tenant,
                                 UUID userId,
                                 Job job,
                                 JobCostEntry entry,
                                 ActivityEventType eventType,
                                 String message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("costEntryId", entry.getId() != null ? entry.getId().toString() : null);
        metadata.put("category", entry.getCategory() != null ? entry.getCategory().name() : null);
        metadata.put("amount", entry.getAmount() != null ? entry.getAmount().toPlainString() : null);
        metadata.put("description", entry.getDescription());
        metadata.put("incurredAt", entry.getIncurredAt() != null ? entry.getIncurredAt().toString() : null);
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(job.getId()),
                eventType, message, metadata);
    }

    private JobCostEntryDto toDto(JobCostEntry entry) {
        JobCostEntryDto dto = new JobCostEntryDto();
        dto.setId(entry.getId());
        dto.setJobId(entry.getJob() != null ? entry.getJob().getId() : null);
        dto.setCategory(entry.getCategory());
        dto.setVendorName(entry.getVendorName());
        dto.setDescription(entry.getDescription());
        dto.setAmount(entry.getAmount());
        dto.setIncurredAt(entry.getIncurredAt());
        dto.setNotes(entry.getNotes());
        dto.setCreatedAt(entry.getCreatedAt());
        dto.setUpdatedAt(entry.getUpdatedAt());
        return dto;
    }
}
