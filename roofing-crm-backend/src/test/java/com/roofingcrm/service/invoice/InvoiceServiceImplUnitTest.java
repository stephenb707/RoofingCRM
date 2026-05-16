package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.CreateInvoiceFromEstimateRequest;
import com.roofingcrm.api.v1.invoice.SendInvoiceEmailRequest;
import com.roofingcrm.api.v1.invoice.ShareInvoiceRequest;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.InvoiceItem;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.security.PublicShareTokenHasher;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.InvoiceConflictException;
import com.roofingcrm.service.mail.EmailService;
import com.roofingcrm.service.mail.PublicUrlProperties;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
class InvoiceServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private EstimateRepository estimateRepository;
    @Mock
    private ActivityEventService activityEventService;
    @Mock
    private EmailService emailService;

    private InvoiceServiceImpl service;
    private PublicUrlProperties publicUrlProperties;
    private Tenant tenant;
    private Job job;
    private Estimate estimate;
    private UUID tenantId;
    private UUID userId;
    private UUID estimateId;

    @BeforeEach
    void setUp() {
        service = new InvoiceServiceImpl(
                tenantAccessService,
                invoiceRepository,
                estimateRepository,
                activityEventService,
                new InvoiceMapper(),
                emailService,
                publicUrlProperties = new PublicUrlProperties());
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        estimateId = UUID.randomUUID();
        publicUrlProperties.setPublicBaseUrl("https://crm.example.com");

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
    void createFromEstimate_allowsDraftEstimate() {
        estimate.setStatus(EstimateStatus.DRAFT);

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
        verify(invoiceRepository).save(any());
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
        var invoice = new Invoice();
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
        Invoice detailed = withSingleInvoiceItem(invoice);
        when(invoiceRepository.findDetailedByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(detailed));

        var result = service.updateStatus(tenantId, userId, invoice.getId(), InvoiceStatus.SENT);

        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertNotNull(invoice.getSentAt());
        assertEquals(userId, invoice.getUpdatedByUserId());
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
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
        var invoice = new Invoice();
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
        Invoice detailed = withSingleInvoiceItem(invoice);
        when(invoiceRepository.findDetailedByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(detailed));

        var result = service.updateStatus(tenantId, userId, invoice.getId(), InvoiceStatus.PAID);

        assertEquals(InvoiceStatus.PAID, result.getStatus());
        assertNotNull(invoice.getPaidAt());
        assertEquals(userId, invoice.getUpdatedByUserId());
        assertNotNull(result.getItems());
        assertEquals(1, result.getItems().size());
    }

    private Invoice withSingleInvoiceItem(Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setInvoice(invoice);
        item.setName("Mapped Item");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setLineTotal(new BigDecimal("10.00"));
        item.setSortOrder(0);
        invoice.setItems(new ArrayList<>(List.of(item)));
        return invoice;
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

    @Test
    void shareInvoice_whenDraft_transitionsToSent_setsSentAt_setsToken_andExpires() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-200");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssuedAt(Instant.now());
        invoice.setTotal(BigDecimal.valueOf(1200));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareInvoiceRequest request = new ShareInvoiceRequest();
        request.setExpiresInDays(14);
        var response = service.shareInvoice(tenantId, userId, invoice.getId(), request);

        assertNotNull(response.getToken());
        assertNotNull(response.getExpiresAt());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
        assertNotNull(invoice.getSentAt());
        assertTrue(invoice.isPublicEnabled());
        assertEquals(userId, invoice.getUpdatedByUserId());
        assertEquals(
                PublicShareTokenHasher.sha256HexUtf8(response.getToken()),
                invoice.getPublicTokenHash());
        verify(invoiceRepository).save(eq(invoice));
        ArgumentCaptor<Map<String, Object>> sharedMetaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(job.getId()),
                eq(ActivityEventType.INVOICE_SHARED), anyString(), sharedMetaCaptor.capture());
        Map<String, Object> sharedMeta = sharedMetaCaptor.getValue();
        assertEquals(invoice.getId().toString(), sharedMeta.get("invoiceId"));
        assertEquals("INV-200", sharedMeta.get("invoiceNumber"));
        assertNotNull(sharedMeta.get("expiresAt"));
        assertEquals(Boolean.TRUE, sharedMeta.get("publicLinkSent"));
        assertEquals("LINK", sharedMeta.get("deliveryMethod"));
        assertFalse(sharedMeta.containsKey("publicUrl"));
        assertFalse(sharedMeta.containsKey("publicToken"));
        assertFalse(sharedMeta.containsKey("token"));
    }

    @Test
    void shareInvoice_whenTokenPresentNotExpired_rotatesToken_updatesExpires() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-201");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedAt(Instant.now());
        invoice.setTotal(BigDecimal.valueOf(1200));
        invoice.setPublicTokenHash(PublicShareTokenHasher.sha256HexUtf8("existing-token"));
        invoice.setPublicEnabled(true);
        invoice.setPublicExpiresAt(Instant.now().plusSeconds(3600));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareInvoiceRequest request = new ShareInvoiceRequest();
        request.setExpiresInDays(30);
        var response = service.shareInvoice(tenantId, userId, invoice.getId(), request);

        assertNotNull(response.getToken());
        assertNotEquals(
                PublicShareTokenHasher.sha256HexUtf8("existing-token"),
                invoice.getPublicTokenHash());
        assertEquals(
                PublicShareTokenHasher.sha256HexUtf8(response.getToken()),
                invoice.getPublicTokenHash());
        assertNotNull(invoice.getPublicExpiresAt());
        verify(invoiceRepository).save(eq(invoice));
    }

    @Test
    void shareInvoice_secondConsecutiveShare_returnsDifferentToken() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-210");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedAt(Instant.now());
        invoice.setTotal(BigDecimal.valueOf(1200));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var first = service.shareInvoice(tenantId, userId, invoice.getId(), new ShareInvoiceRequest());
        String hashAfterFirst = invoice.getPublicTokenHash();
        var second = service.shareInvoice(tenantId, userId, invoice.getId(), new ShareInvoiceRequest());

        assertNotNull(first.getToken());
        assertNotNull(second.getToken());
        assertNotEquals(first.getToken(), second.getToken());
        assertNotEquals(hashAfterFirst, invoice.getPublicTokenHash());
        assertEquals(PublicShareTokenHasher.sha256HexUtf8(second.getToken()), invoice.getPublicTokenHash());
    }

    @Test
    void shareInvoice_whenExpired_generatesNewToken() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-202");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedAt(Instant.now());
        invoice.setTotal(BigDecimal.valueOf(1200));
        invoice.setPublicTokenHash(PublicShareTokenHasher.sha256HexUtf8("expired-token"));
        invoice.setPublicEnabled(true);
        invoice.setPublicExpiresAt(Instant.now().minusSeconds(60));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ShareInvoiceRequest request = new ShareInvoiceRequest();
        request.setExpiresInDays(14);
        var response = service.shareInvoice(tenantId, userId, invoice.getId(), request);

        assertNotNull(response.getToken());
        assertNotEquals("expired-token", response.getToken());
        assertEquals(
                PublicShareTokenHasher.sha256HexUtf8(response.getToken()),
                invoice.getPublicTokenHash());
        verify(invoiceRepository).save(eq(invoice));
    }

    @Test
    void sendInvoiceEmail_whenDraft_transitionsToSent_setsSentAt_andSendsEmail() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-300");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssuedAt(Instant.now());
        invoice.setTotal(BigDecimal.valueOf(1200));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SendInvoiceEmailRequest request = new SendInvoiceEmailRequest();
        request.setRecipientEmail("customer@example.com");
        request.setRecipientName("Jane");

        var response = service.sendInvoiceEmail(tenantId, userId, invoice.getId(), request);

        assertTrue(response.isSuccess());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
        assertNotNull(invoice.getSentAt());
        assertTrue(response.getPublicUrl().contains("/invoice/"));
        verify(emailService).send(argThat(message ->
                "customer@example.com".equals(message.toEmail())
                        && message.subject().contains("INV-300")
                        && message.text().contains("View Invoice")
        ));
        ArgumentCaptor<Map<String, Object>> emailMetaCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityEventService).recordEvent(eq(tenant), eq(userId), eq(ActivityEntityType.JOB), eq(job.getId()),
                eq(ActivityEventType.INVOICE_EMAIL_SENT), anyString(), emailMetaCaptor.capture());
        Map<String, Object> emailMeta = emailMetaCaptor.getValue();
        assertEquals(invoice.getId().toString(), emailMeta.get("invoiceId"));
        assertEquals("INV-300", emailMeta.get("invoiceNumber"));
        assertEquals("customer@example.com", emailMeta.get("recipientEmail"));
        assertEquals(Boolean.TRUE, emailMeta.get("publicLinkSent"));
        assertEquals("EMAIL", emailMeta.get("deliveryMethod"));
        assertFalse(emailMeta.containsKey("publicUrl"));
        assertFalse(emailMeta.containsKey("publicToken"));
        assertFalse(emailMeta.containsKey("token"));
    }

    @Test
    void sendInvoiceEmail_whenTokenValid_rotatesTokenForEmailBody() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setTenant(tenant);
        invoice.setJob(job);
        invoice.setInvoiceNumber("INV-301");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setIssuedAt(Instant.now());
        invoice.setTotal(BigDecimal.valueOf(1200));
        invoice.setPublicTokenHash(PublicShareTokenHasher.sha256HexUtf8("existing-token"));
        invoice.setPublicEnabled(true);
        invoice.setPublicExpiresAt(Instant.now().plusSeconds(3600));

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findByIdAndTenantAndArchivedFalse(invoice.getId(), tenant)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SendInvoiceEmailRequest request = new SendInvoiceEmailRequest();
        request.setRecipientEmail("customer@example.com");

        var response = service.sendInvoiceEmail(tenantId, userId, invoice.getId(), request);

        assertTrue(response.getPublicUrl().contains("/invoice/"));
        assertFalse(response.getPublicUrl().endsWith("/invoice/existing-token"));
        String suffix = response.getPublicUrl().substring(response.getPublicUrl().lastIndexOf('/') + 1);
        assertEquals(PublicShareTokenHasher.sha256HexUtf8(suffix), invoice.getPublicTokenHash());
        verify(emailService).send(any());
    }
}
