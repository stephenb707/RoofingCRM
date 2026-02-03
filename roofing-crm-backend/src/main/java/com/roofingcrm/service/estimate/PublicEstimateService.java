package com.roofingcrm.service.estimate;

import com.roofingcrm.api.publicapi.estimate.PublicEstimateDecisionRequest;
import com.roofingcrm.api.publicapi.estimate.PublicEstimateDto;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.EstimateConflictException;
import com.roofingcrm.service.exception.EstimateLinkExpiredException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PublicEstimateService {

    private final EstimateRepository estimateRepository;
    private final ActivityEventService activityEventService;

    public PublicEstimateService(EstimateRepository estimateRepository,
                                 ActivityEventService activityEventService) {
        this.estimateRepository = estimateRepository;
        this.activityEventService = activityEventService;
    }

    @Transactional(readOnly = true)
    public PublicEstimateDto getByToken(@NonNull String token) {
        Estimate estimate = estimateRepository
                .findByPublicTokenAndPublicEnabledTrueAndArchivedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        if (estimate.getPublicExpiresAt() != null && Instant.now().isAfter(estimate.getPublicExpiresAt())) {
            throw new EstimateLinkExpiredException("Link expired");
        }

        return toPublicDto(estimate);
    }

    @Transactional
    public PublicEstimateDto decide(@NonNull String token, @NonNull PublicEstimateDecisionRequest request) {
        Estimate estimate = estimateRepository
                .findByPublicTokenAndPublicEnabledTrueAndArchivedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        if (estimate.getPublicExpiresAt() != null && Instant.now().isAfter(estimate.getPublicExpiresAt())) {
            throw new EstimateLinkExpiredException("Link expired");
        }

        if (estimate.getStatus() == EstimateStatus.ACCEPTED || estimate.getStatus() == EstimateStatus.REJECTED) {
            throw new EstimateConflictException("Estimate has already been accepted or rejected");
        }

        if (request.getDecision() != EstimateStatus.ACCEPTED && request.getDecision() != EstimateStatus.REJECTED) {
            throw new IllegalArgumentException("decision must be ACCEPTED or REJECTED");
        }

        estimate.setStatus(request.getDecision());
        estimate.setDecisionAt(Instant.now());
        estimate.setDecisionByName(request.getSignerName() != null ? request.getSignerName().trim() : null);
        estimate.setDecisionByEmail(request.getSignerEmail() != null && !request.getSignerEmail().isBlank()
                ? request.getSignerEmail().trim() : null);

        Estimate saved = estimateRepository.save(estimate);

        Tenant tenant = estimate.getTenant();
        UUID jobId = Objects.requireNonNull(estimate.getJob().getId());
        ActivityEventType eventType = request.getDecision() == EstimateStatus.ACCEPTED
                ? ActivityEventType.ESTIMATE_ACCEPTED
                : ActivityEventType.ESTIMATE_REJECTED;
        String message = request.getDecision() == EstimateStatus.ACCEPTED
                ? "Estimate accepted"
                : "Estimate rejected";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("estimateNumber", saved.getEstimateNumber());
        metadata.put("signerName", saved.getDecisionByName());
        metadata.put("signerEmail", saved.getDecisionByEmail());

        activityEventService.recordEventWithActor(tenant, null, ActivityEntityType.JOB, jobId, eventType, message, metadata);

        return toPublicDto(saved);
    }

    private PublicEstimateDto toPublicDto(Estimate e) {
        PublicEstimateDto dto = new PublicEstimateDto();
        dto.setEstimateNumber(e.getEstimateNumber());
        dto.setStatus(e.getStatus());
        dto.setTitle(e.getTitle());
        dto.setNotes(e.getNotesForCustomer());
        dto.setIssueDate(e.getIssueDate());
        dto.setValidUntil(e.getValidUntil());
        dto.setSubtotal(e.getSubtotal());
        dto.setTotal(e.getTotal());
        dto.setPublicExpiresAt(e.getPublicExpiresAt());

        if (e.getJob() != null && e.getJob().getCustomer() != null) {
            Customer c = e.getJob().getCustomer();
            dto.setCustomerName(formatCustomerName(c.getFirstName(), c.getLastName()));
            Address addr = e.getJob().getPropertyAddress();
            if (addr == null && c.getBillingAddress() != null) {
                addr = c.getBillingAddress();
            }
            dto.setCustomerAddress(addr != null ? formatAddress(addr) : null);
        }

        List<com.roofingcrm.api.publicapi.estimate.PublicEstimateItemDto> items = new ArrayList<>();
        if (e.getItems() != null) {
            for (EstimateItem item : e.getItems()) {
                com.roofingcrm.api.publicapi.estimate.PublicEstimateItemDto itemDto = new com.roofingcrm.api.publicapi.estimate.PublicEstimateItemDto();
                itemDto.setName(item.getName());
                itemDto.setDescription(item.getDescription());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setUnit(item.getUnit());
                itemDto.setLineTotal(item.getLineTotal());
                items.add(itemDto);
            }
        }
        dto.setItems(items);

        return dto;
    }

    private static String formatCustomerName(String first, String last) {
        if (first == null && last == null) return null;
        return ((first != null ? first.trim() : "") + " " + (last != null ? last.trim() : "")).trim();
    }

    private static String formatAddress(Address a) {
        if (a == null) return null;
        List<String> parts = new ArrayList<>();
        if (a.getLine1() != null && !a.getLine1().isBlank()) parts.add(a.getLine1().trim());
        if (a.getLine2() != null && !a.getLine2().isBlank()) parts.add(a.getLine2().trim());
        StringBuilder cityState = new StringBuilder();
        if (a.getCity() != null && !a.getCity().isBlank()) cityState.append(a.getCity().trim());
        if (a.getState() != null && !a.getState().isBlank()) {
            if (cityState.length() > 0) cityState.append(", ");
            cityState.append(a.getState().trim());
        }
        if (a.getZip() != null && !a.getZip().isBlank()) {
            if (cityState.length() > 0) cityState.append(" ");
            cityState.append(a.getZip().trim());
        }
        if (cityState.length() > 0) parts.add(cityState.toString());
        return String.join(", ", parts);
    }
}
