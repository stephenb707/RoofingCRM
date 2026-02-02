package com.roofingcrm.service.estimate;

import com.roofingcrm.api.v1.estimate.CreateEstimateRequest;
import com.roofingcrm.api.v1.estimate.EstimateDto;
import com.roofingcrm.api.v1.estimate.EstimateItemDto;
import com.roofingcrm.api.v1.estimate.EstimateItemRequest;
import com.roofingcrm.api.v1.estimate.ShareEstimateRequest;
import com.roofingcrm.api.v1.estimate.ShareEstimateResponse;
import com.roofingcrm.api.v1.estimate.UpdateEstimateRequest;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Transactional
public class EstimateServiceImpl implements EstimateService {

    private final TenantAccessService tenantAccessService;
    private final JobRepository jobRepository;
    private final EstimateRepository estimateRepository;
    private final ActivityEventService activityEventService;

    private static final SecureRandom secureRandom = new SecureRandom();

    // Simple counter for generating estimate numbers (in production, use a more robust approach)
    private static final AtomicLong estimateCounter = new AtomicLong(System.currentTimeMillis());

    @Autowired
    public EstimateServiceImpl(TenantAccessService tenantAccessService,
                               JobRepository jobRepository,
                               EstimateRepository estimateRepository,
                               ActivityEventService activityEventService) {
        this.tenantAccessService = tenantAccessService;
        this.jobRepository = jobRepository;
        this.estimateRepository = estimateRepository;
        this.activityEventService = activityEventService;
    }

    @Override
    public EstimateDto createEstimateForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, CreateEstimateRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        Estimate estimate = new Estimate();
        estimate.setTenant(tenant);
        estimate.setJob(job);
        estimate.setCreatedByUserId(userId);
        estimate.setUpdatedByUserId(userId);

        // Generate a unique estimate number
        estimate.setEstimateNumber("EST-" + estimateCounter.incrementAndGet());

        estimate.setStatus(request.getStatus() != null ? request.getStatus() : EstimateStatus.DRAFT);
        estimate.setTitle(request.getTitle());
        estimate.setNotesForCustomer(request.getNotes());
        estimate.setIssueDate(request.getIssueDate());
        estimate.setValidUntil(request.getValidUntil());

        // Create items
        List<EstimateItem> items = new ArrayList<>();
        for (EstimateItemRequest itemReq : request.getItems()) {
            EstimateItem item = toItem(tenant, userId, estimate, itemReq);
            items.add(item);
        }
        estimate.setItems(items);

        // Compute totals
        BigDecimal subtotal = computeSubtotal(items);
        estimate.setSubtotal(subtotal);
        estimate.setTotal(subtotal); // No tax calculation for now

        Estimate saved = estimateRepository.save(estimate);
        return toDto(saved);
    }

    @Override
    public EstimateDto updateEstimate(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId, UpdateEstimateRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Estimate estimate = estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        estimate.setUpdatedByUserId(userId);

        if (request.getTitle() != null) {
            estimate.setTitle(request.getTitle());
        }

        if (request.getNotes() != null) {
            estimate.setNotesForCustomer(request.getNotes());
        }

        if (request.getIssueDate() != null) {
            estimate.setIssueDate(request.getIssueDate());
        }

        if (request.getValidUntil() != null) {
            estimate.setValidUntil(request.getValidUntil());
        }

        if (request.getStatus() != null) {
            estimate.setStatus(request.getStatus());
        }

        // Replace items if provided
        if (request.getItems() != null) {
            estimate.getItems().clear();
            for (EstimateItemRequest itemReq : request.getItems()) {
                EstimateItem item = toItem(tenant, userId, estimate, itemReq);
                estimate.getItems().add(item);
            }

            // Recompute totals
            BigDecimal subtotal = computeSubtotal(estimate.getItems());
            estimate.setSubtotal(subtotal);
            estimate.setTotal(subtotal);
        }

        Estimate saved = estimateRepository.save(estimate);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateDto getEstimate(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Estimate estimate = estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        return toDto(estimate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstimateDto> listEstimatesForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        List<Estimate> estimates = estimateRepository.findByJobAndArchivedFalse(job);
        return estimates.stream().map(this::toDto).toList();
    }

    @Override
    public EstimateDto updateEstimateStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId, EstimateStatus newStatus) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Estimate estimate = estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        estimate.setStatus(newStatus);
        estimate.setUpdatedByUserId(userId);

        Estimate saved = estimateRepository.save(estimate);
        return toDto(saved);
    }

    @Override
    public ShareEstimateResponse shareEstimate(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId, ShareEstimateRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to share estimates.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Estimate estimate = estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        int days = (request != null && request.getExpiresInDays() != null) ? request.getExpiresInDays() : 14;
        days = Math.min(365, Math.max(1, days));

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(days * 86400L);

        boolean needNewToken = estimate.getPublicToken() == null || estimate.getPublicToken().isBlank()
                || (estimate.getPublicExpiresAt() != null && estimate.getPublicExpiresAt().isBefore(now));

        if (needNewToken) {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            estimate.setPublicToken(token);
        }

        estimate.setPublicEnabled(true);
        estimate.setPublicExpiresAt(expiresAt);
        estimate.setPublicLastSharedAt(now);
        estimate.setUpdatedByUserId(userId);
        Estimate saved = estimateRepository.save(estimate);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("estimateId", saved.getId().toString());
        metadata.put("estimateNumber", saved.getEstimateNumber());
        metadata.put("expiresAt", expiresAt.toString());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(saved.getJob().getId()),
                ActivityEventType.ESTIMATE_SHARED, "Estimate shared via public link", metadata);

        ShareEstimateResponse response = new ShareEstimateResponse();
        response.setToken(saved.getPublicToken());
        response.setExpiresAt(expiresAt);
        return response;
    }

    private EstimateItem toItem(Tenant tenant, UUID userId, Estimate estimate, EstimateItemRequest req) {
        EstimateItem item = new EstimateItem();
        item.setTenant(tenant);
        item.setEstimate(estimate);
        item.setCreatedByUserId(userId);
        item.setUpdatedByUserId(userId);
        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setQuantity(req.getQuantity());
        item.setUnitPrice(req.getUnitPrice());
        item.setUnit(req.getUnit());
        item.setLineTotal(req.getQuantity().multiply(req.getUnitPrice()));
        return item;
    }

    private BigDecimal computeSubtotal(List<EstimateItem> items) {
        return items.stream()
                .map(EstimateItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private EstimateDto toDto(Estimate entity) {
        EstimateDto dto = new EstimateDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setTitle(entity.getTitle());
        dto.setNotes(entity.getNotesForCustomer());
        dto.setIssueDate(entity.getIssueDate());
        dto.setValidUntil(entity.getValidUntil());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
            if (entity.getJob().getCustomer() != null) {
                dto.setCustomerId(entity.getJob().getCustomer().getId());
            }
        }

        // Map items
        List<EstimateItemDto> itemDtos = new ArrayList<>();
        if (entity.getItems() != null) {
            for (EstimateItem item : entity.getItems()) {
                EstimateItemDto itemDto = new EstimateItemDto();
                itemDto.setId(item.getId());
                itemDto.setName(item.getName());
                itemDto.setDescription(item.getDescription());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setUnit(item.getUnit());
                itemDtos.add(itemDto);
            }
        }
        dto.setItems(itemDtos);

        // Compute totals from items
        BigDecimal subtotal = entity.getItems() != null ? computeSubtotal(entity.getItems()) : BigDecimal.ZERO;
        dto.setSubtotal(subtotal);
        dto.setTotal(subtotal); // No tax calculation for now

        return dto;
    }
}
