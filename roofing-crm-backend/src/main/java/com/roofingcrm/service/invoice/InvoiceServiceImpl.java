package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.CreateInvoiceFromEstimateRequest;
import com.roofingcrm.api.v1.invoice.InvoiceDto;
import com.roofingcrm.api.v1.invoice.InvoiceSummaryDto;
import com.roofingcrm.api.v1.invoice.SendInvoiceEmailRequest;
import com.roofingcrm.api.v1.invoice.SendInvoiceEmailResponse;
import com.roofingcrm.api.v1.invoice.ShareInvoiceRequest;
import com.roofingcrm.api.v1.invoice.ShareInvoiceResponse;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.InvoiceItem;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.audit.AuditSupport;
import com.roofingcrm.service.exception.InvoiceConflictException;
import com.roofingcrm.service.exception.MailConfigurationException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.mail.EmailService;
import com.roofingcrm.service.mail.InvoiceEmailTemplateBuilder;
import com.roofingcrm.service.mail.PublicUrlProperties;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@Service
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final TenantAccessService tenantAccessService;
    private final InvoiceRepository invoiceRepository;
    private final EstimateRepository estimateRepository;
    private final ActivityEventService activityEventService;
    private final InvoiceMapper invoiceMapper;
    private final EmailService emailService;
    private final PublicUrlProperties publicUrlProperties;
    private final InvoiceEmailTemplateBuilder invoiceEmailTemplateBuilder;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public InvoiceServiceImpl(TenantAccessService tenantAccessService,
                              InvoiceRepository invoiceRepository,
                              EstimateRepository estimateRepository,
                              ActivityEventService activityEventService,
                              InvoiceMapper invoiceMapper,
                              EmailService emailService,
                              PublicUrlProperties publicUrlProperties) {
        this.tenantAccessService = tenantAccessService;
        this.invoiceRepository = invoiceRepository;
        this.estimateRepository = estimateRepository;
        this.activityEventService = activityEventService;
        this.invoiceMapper = invoiceMapper;
        this.emailService = emailService;
        this.publicUrlProperties = publicUrlProperties;
        this.invoiceEmailTemplateBuilder = new InvoiceEmailTemplateBuilder();
    }

    @Override
    public InvoiceDto createFromEstimate(@NonNull UUID tenantId, @NonNull UUID userId, CreateInvoiceFromEstimateRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(com.roofingcrm.domain.enums.UserRole.OWNER, com.roofingcrm.domain.enums.UserRole.ADMIN, com.roofingcrm.domain.enums.UserRole.SALES)),
                "You do not have permission to create invoices.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Estimate estimate = estimateRepository.findByIdAndTenantAndArchivedFalse(request.getEstimateId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Estimate not found"));

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
        AuditSupport.touchForCreate(invoice, userId);

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

        return invoiceMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceSummaryDto> listInvoices(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, InvoiceStatus status, @NonNull Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        return invoiceRepository.findByTenantAndArchivedFalseWithFilters(tenant, jobId, status, pageable)
                .map(invoiceMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceSummaryDto> listInvoicesForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        List<Invoice> invoices = invoiceRepository.findByTenantAndJobIdAndArchivedFalseOrderByCreatedAtDesc(tenant, jobId);
        return invoices.stream().map(invoiceMapper::toSummaryDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoice(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Invoice invoice = invoiceRepository.findDetailedByIdAndTenantAndArchivedFalse(invoiceId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return invoiceMapper.toDto(invoice);
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
        AuditSupport.touchForUpdate(invoice, userId);

        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", saved.getId().toString());
        metadata.put("invoiceNumber", saved.getInvoiceNumber());
        metadata.put("fromStatus", fromStatus != null ? fromStatus.name() : null);
        metadata.put("toStatus", newStatus.name());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(saved.getJob().getId()),
                ActivityEventType.INVOICE_STATUS_CHANGED, "Invoice " + saved.getInvoiceNumber() + " status: " + fromStatus + " → " + newStatus, metadata);

        Invoice detailed = invoiceRepository.findDetailedByIdAndTenantAndArchivedFalse(saved.getId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found after status update"));
        return invoiceMapper.toDto(detailed);
    }

    @Override
    public ShareInvoiceResponse shareInvoice(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId, ShareInvoiceRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to share invoices.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Invoice invoice = invoiceRepository.findByIdAndTenantAndArchivedFalse(invoiceId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new InvoiceConflictException("Cannot share a VOID invoice");
        }

        int days = (request != null && request.getExpiresInDays() != null) ? request.getExpiresInDays() : 14;
        Instant now = Instant.now();
        TokenState tokenState = ensurePublicShare(invoice, userId, now, days);
        Invoice saved = invoiceRepository.save(invoice);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", saved.getId().toString());
        metadata.put("invoiceNumber", saved.getInvoiceNumber());
        metadata.put("expiresAt", tokenState.expiresAt().toString());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(saved.getJob().getId()),
                ActivityEventType.INVOICE_SHARED, "Invoice shared via public link", metadata);

        ShareInvoiceResponse response = new ShareInvoiceResponse();
        response.setToken(saved.getPublicToken());
        response.setExpiresAt(tokenState.expiresAt());
        return response;
    }

    @Override
    public SendInvoiceEmailResponse sendInvoiceEmail(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId, SendInvoiceEmailRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to email invoices.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Invoice invoice = invoiceRepository.findByIdAndTenantAndArchivedFalse(invoiceId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new InvoiceConflictException("Cannot email a VOID invoice");
        }

        int days = request.getExpiresInDays() != null ? request.getExpiresInDays() : 14;
        Instant now = Instant.now();
        TokenState tokenState = ensurePublicShare(invoice, userId, now, days);
        Invoice saved = invoiceRepository.save(invoice);
        String publicUrl = buildPublicInvoiceUrl(saved.getPublicToken());

        emailService.send(invoiceEmailTemplateBuilder.build(
                tenant,
                saved,
                request.getRecipientEmail(),
                request.getRecipientName(),
                request.getSubject(),
                request.getMessage(),
                publicUrl
        ));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", saved.getId().toString());
        metadata.put("invoiceNumber", saved.getInvoiceNumber());
        metadata.put("recipientEmail", request.getRecipientEmail());
        metadata.put("publicUrl", publicUrl);
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                Objects.requireNonNull(saved.getJob().getId()),
                ActivityEventType.INVOICE_EMAIL_SENT, "Invoice emailed to " + request.getRecipientEmail(), metadata);

        SendInvoiceEmailResponse response = new SendInvoiceEmailResponse();
        response.setSuccess(true);
        response.setSentAt(now);
        response.setPublicUrl(publicUrl);
        response.setReusedExistingToken(tokenState.reusedExistingToken());
        return response;
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

    private TokenState ensurePublicShare(Invoice invoice, UUID userId, Instant now, int days) {
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoice.setStatus(InvoiceStatus.SENT);
            if (invoice.getSentAt() == null) {
                invoice.setSentAt(now);
            }
        }

        int normalizedDays = Math.min(365, Math.max(1, days));
        Instant expiresAt = now.plusSeconds(normalizedDays * 86400L);

        boolean needNewToken = invoice.getPublicToken() == null || invoice.getPublicToken().isBlank()
                || (invoice.getPublicExpiresAt() != null && invoice.getPublicExpiresAt().isBefore(now));
        if (needNewToken) {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            invoice.setPublicToken(token);
        }

        invoice.setPublicEnabled(true);
        invoice.setPublicExpiresAt(expiresAt);
        invoice.setPublicLastSharedAt(now);
        AuditSupport.touchForUpdate(invoice, userId);
        return new TokenState(expiresAt, !needNewToken);
    }

    private String buildPublicInvoiceUrl(String token) {
        String baseUrl = publicUrlProperties.getPublicBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new MailConfigurationException("Public base URL is missing. Set APP_PUBLIC_BASE_URL before sending invoice emails.");
        }
        return baseUrl.replaceAll("/+$", "") + "/invoice/" + token;
    }

    private record TokenState(Instant expiresAt, boolean reusedExistingToken) {
    }

}
