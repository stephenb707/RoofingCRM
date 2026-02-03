package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.CreateInvoiceFromEstimateRequest;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.InvoiceConflictException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class InvoiceServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private EstimateRepository estimateRepository;
    @Mock
    private ActivityEventService activityEventService;

    private InvoiceServiceImpl service;
    private Tenant tenant;
    private Job job;
    private Estimate estimate;
    private UUID tenantId;
    private UUID userId;
    private UUID estimateId;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(tenantAccessService, invoiceRepository, estimateRepository, activityEventService);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        estimateId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);

        job = new Job();
        job.setId(UUID.randomUUID());
        job.setTenant(tenant);

        EstimateItem item = new EstimateItem();
        item.setName("Shingles");
        item.setQuantity(new BigDecimal("100"));
        item.setUnitPrice(new BigDecimal("50"));
        item.setLineTotal(new BigDecimal("5000"));

        estimate = new Estimate();
        estimate.setId(estimateId);
        estimate.setTenant(tenant);
        estimate.setJob(job);
        estimate.setStatus(EstimateStatus.ACCEPTED);
        estimate.setEstimateNumber("EST-1001");
        estimate.setTotal(new BigDecimal("5000"));
        estimate.setItems(new ArrayList<>(List.of(item)));
    }

    @Test
    void createFromEstimate_requiresAccepted_throws409WhenDraft() {
        estimate.setStatus(EstimateStatus.DRAFT);

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant)).thenReturn(Optional.of(estimate));

        CreateInvoiceFromEstimateRequest req = new CreateInvoiceFromEstimateRequest();
        req.setEstimateId(estimateId);

        assertThrows(InvoiceConflictException.class, () -> service.createFromEstimate(tenantId, userId, req));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void createFromEstimate_createsInvoiceAndEmitsActivity() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant)).thenReturn(Optional.of(estimate));
        when(invoiceRepository.findMaxInvoiceNumberSuffix(tenantId)).thenReturn(0L);
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            com.roofingcrm.domain.entity.Invoice invEntity = inv.getArgument(0);
            invEntity.setId(UUID.randomUUID());
            return invEntity;
        });

        CreateInvoiceFromEstimateRequest req = new CreateInvoiceFromEstimateRequest();
        req.setEstimateId(estimateId);

        var result = service.createFromEstimate(tenantId, userId, req);

        assertNotNull(result);
        assertEquals("INV-1", result.getInvoiceNumber());
        assertEquals(InvoiceStatus.DRAFT, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("Shingles", result.getItems().get(0).getName());

        ArgumentCaptor<com.roofingcrm.domain.enums.ActivityEventType> typeCaptor = ArgumentCaptor.forClass(com.roofingcrm.domain.enums.ActivityEventType.class);
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(com.roofingcrm.domain.enums.ActivityEntityType.JOB), any(), typeCaptor.capture(), any(), any());
        assertEquals(com.roofingcrm.domain.enums.ActivityEventType.INVOICE_CREATED, typeCaptor.getValue());
    }

    @Test
    void updateStatus_draftToSent_ok() {
        var invoice = new com.roofingcrm.domain.entity.Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-1");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotal(BigDecimal.valueOf(5000));
        invoice.setIssuedAt(Instant.now());

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStatus(tenantId, userId, invoice.getId(), InvoiceStatus.SENT);

        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertNotNull(invoice.getSentAt());
    }

    @Test
    void updateStatus_draftToPaid_rejected() {
        var invoice = new com.roofingcrm.domain.entity.Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-1");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotal(BigDecimal.valueOf(5000));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));

        assertThrows(InvoiceConflictException.class, () -> service.updateStatus(tenantId, userId, invoice.getId(), InvoiceStatus.PAID));
    }

    @Test
    void updateStatus_sentToPaid_ok() {
        var invoice = new com.roofingcrm.domain.entity.Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-1");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setTotal(BigDecimal.valueOf(5000));
        invoice.setSentAt(Instant.now());

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.updateStatus(tenantId, userId, invoice.getId(), InvoiceStatus.PAID);

        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertNotNull(invoice.getPaidAt());
    }

    @Test
    void updateStatus_paidTerminal_throws409() {
        var invoice = new com.roofingcrm.domain.entity.Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-1");
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setTotal(BigDecimal.valueOf(5000));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));

        assertThrows(InvoiceConflictException.class, () -> service.updateStatus(tenantId, userId, invoice.getId(), InvoiceStatus.SENT));
        verify(invoiceRepository, never()).save(any());
    }
}
