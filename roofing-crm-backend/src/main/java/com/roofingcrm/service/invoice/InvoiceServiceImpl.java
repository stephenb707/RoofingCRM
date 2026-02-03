package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.CreateInvoiceFromEstimateRequest;
import com.roofingcrm.api.v1.invoice.InvoiceDto;
import com.roofingcrm.api.v1.invoice.InvoiceItemDto;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.InvoiceItem;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.InvoiceConflictException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final TenantAccessService tenantAccessService;
    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final ActivityEventService activityEventService;

    @Autowired
    public InvoiceServiceImpl(TenantAccessService tenantAccessService,
                              InvoiceRepository invoiceRepository,
                              EstimateRepository estimateRepository,
                              ActivityEventService activityEventService) {
        this.tenantAccessService = tenantAccessService;
        this.invoiceRepository = invoiceRepository;
        this.estimateRepository = estimateRepository;
        this.activityEventService = activityEventService;
    }

    @Override
    public InvoiceDto createFromEstimate(@NonNull UUID tenantId, @NonNull UUID userId, CreateInvoiceFromEstimateRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(com.roofingcrm.domain.enums.UserRole.OWNER, com.roofingcrm.domain.enums.UserRole.ADMIN, com.roofingcrm.domain.enums.UserRole.SALES)),
                "You do not have permission to create invoices.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Estimate estimate = estimateRepository.findByIdAndTenantAndArchivedFalse(request.getEstimateId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

        if (estimate.getStatus() != EstimateStatus.ACCEPTED) {
            throw new InvoiceConflictException("Estimate must be ACCEPTED to create an invoice");
        }

        long suffix = invoiceRepository.findMaxInvoiceNumberSuffix(tenant.getId());
        String invoiceNumber = "INV-" + (suffix + 1);

        Invoice invoice = new Invoice();
        invoice.setTenant(tenant);
        invoice.setJob(estimate.getJob());
        invoice.setEstimate(estimate);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssuedAt(Instant.now());
        invoice.setDueAt(request.getDueAt());
        invoice.setNotes(request.getNotes());
        invoice.setCreatedByUserId(userId);

        BigDecimal total = estimate.getTotal() != null ? estimate.getTotal() : BigDecimal.ZERO;
        invoice.setTotal(total);

        List<InvoiceItem> items = new ArrayList<>();
        int sortOrder = 0;
        for (EstimateItem ei : estimate.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setName(ei.getName());
            item.setDescription(ei.getDescription());
            item.setQuantity(ei.getQuantity());
            item.setUnitPrice(ei.getUnitPrice());
            item.setLineTotal(ei.getLineTotal());
            item.setSortOrder(sortOrder++);
            items.add(item);
        }
        invoice.setItems(items);

        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", saved.getId().toString());
        metadata.put("invoiceNumber", saved.getInvoiceNumber());
        metadata.put("total", saved.getTotal() != null ? saved.getTotal().toString() : null);
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(saved.getJob().getId()),
                ActivityEventType.INVOICE_CREATED, "Invoice " + saved.getInvoiceNumber() + " created", metadata);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceDto> listInvoices(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, InvoiceStatus status, @NonNull Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        return invoiceRepository.findByTenantAndArchivedFalseWithFilters(tenant, jobId, status, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDto> listInvoicesForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        List<Invoice> invoices = invoiceRepository.findByTenantAndJobIdAndArchivedFalseOrderByCreatedAtDesc(tenant, jobId);
        return invoices.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Invoice invoice = invoiceRepository.findByIdAndTenantAndArchivedFalse(invoiceId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toDto(invoice);
    }

    @Override
    public InvoiceDto updateStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId, InvoiceStatus newStatus) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(com.roofingcrm.domain.enums.UserRole.OWNER, com.roofingcrm.domain.enums.UserRole.ADMIN, com.roofingcrm.domain.enums.UserRole.SALES)),
                "You do not have permission to update invoice status.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Invoice invoice = invoiceRepository.findByIdAndTenantAndArchivedFalse(invoiceId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.isTerminal()) {
            throw new InvoiceConflictException("Cannot change status of a PAID or VOID invoice");
        }

        InvoiceStatus fromStatus = invoice.getStatus();
        validateTransition(fromStatus, newStatus);

        Instant now = Instant.now();
        if (newStatus == InvoiceStatus.SENT && invoice.getSentAt() == null) {
            invoice.setSentAt(now);
        }
        if (newStatus == InvoiceStatus.PAID && invoice.getPaidAt() == null) {
            invoice.setPaidAt(now);
        }
        invoice.setStatus(newStatus);

        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", saved.getId().toString());
        metadata.put("invoiceNumber", saved.getInvoiceNumber());
        metadata.put("fromStatus", fromStatus != null ? fromStatus.name() : null);
        metadata.put("toStatus", newStatus.name());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(saved.getJob().getId()),
                ActivityEventType.INVOICE_STATUS_CHANGED, "Invoice " + saved.getInvoiceNumber() + " status: " + fromStatus + " → " + newStatus, metadata);

        return toDto(saved);
    }

    private void validateTransition(InvoiceStatus from, InvoiceStatus to) {
        if (from == InvoiceStatus.DRAFT) {
            if (to != InvoiceStatus.SENT && to != InvoiceStatus.VOID) {
                throw new InvoiceConflictException("DRAFT can only transition to SENT or VOID");
            }
        } else if (from == InvoiceStatus.SENT) {
            if (to != InvoiceStatus.PAID && to != InvoiceStatus.VOID) {
                throw new InvoiceConflictException("SENT can only transition to PAID or VOID");
            }
        } else {
            throw new InvoiceConflictException("Invalid status transition from " + from);
        }
    }

    private InvoiceDto toDto(Invoice i) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(i.getId());
        dto.setInvoiceNumber(i.getInvoiceNumber());
        dto.setStatus(i.getStatus());
        dto.setIssuedAt(i.getIssuedAt());
        dto.setSentAt(i.getSentAt());
        dto.setDueAt(i.getDueAt());
        dto.setPaidAt(i.getPaidAt());
        dto.setTotal(i.getTotal());
        dto.setNotes(i.getNotes());
        dto.setJobId(i.getJob() != null ? i.getJob().getId() : null);
        dto.setEstimateId(i.getEstimate() != null ? i.getEstimate().getId() : null);
        dto.setCreatedAt(i.getCreatedAt());
        dto.setUpdatedAt(i.getUpdatedAt());

        if (i.getItems() != null) {
            List<InvoiceItemDto> itemDtos = new ArrayList<>();
            for (InvoiceItem it : i.getItems()) {
                InvoiceItemDto idto = new InvoiceItemDto();
                idto.setId(it.getId());
                idto.setName(it.getName());
                idto.setDescription(it.getDescription());
                idto.setQuantity(it.getQuantity());
                idto.setUnitPrice(it.getUnitPrice());
                idto.setLineTotal(it.getLineTotal());
                idto.setSortOrder(it.getSortOrder());
                itemDtos.add(idto);
            }
            dto.setItems(itemDtos);
        }
        return dto;
    }
}
