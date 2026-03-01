package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.NoPaidInvoicesForYearException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaidInvoicesReportServiceTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private InvoiceRepository invoiceRepository;

    private PaidInvoicesReportService service;
    private Tenant tenant;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PaidInvoicesReportService(
                tenantAccessService,
                invoiceRepository,
                new PaidInvoicesPdfGenerator()
        );
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme Roofing");
    }

    @Test
    void generatePaidInvoicesYearPdf_whenNoInvoices_throwsNotFoundException() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), any()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findPaidInvoicesForYear(eq(tenant), any(), any())).thenReturn(List.of());

        assertThrows(NoPaidInvoicesForYearException.class, () ->
                service.generatePaidInvoicesYearPdf(tenantId, userId, 2026));
    }

    @Test
    void generatePaidInvoicesYearPdf_whenInvoicesExist_returnsPdfBytes() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), any()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findPaidInvoicesForYear(eq(tenant), any(), any())).thenReturn(List.of(sampleInvoice()));

        byte[] pdf = service.generatePaidInvoicesYearPdf(tenantId, userId, 2026);

        assertTrue(pdf.length > 4);
        String prefix = new String(pdf, 0, 4);
        assertTrue(prefix.startsWith("%PDF"));
        verify(tenantAccessService).requireAnyRole(eq(tenantId), eq(userId), any(), any());
    }

    @Test
    void getPaidInvoiceYears_requiresOwnerOrAdminRole() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), any()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(invoiceRepository.findPaidInvoiceYears(tenantId)).thenReturn(List.of(2026, 2025));

        service.getPaidInvoiceYears(tenantId, userId);

        verify(tenantAccessService).requireAnyRole(eq(tenantId), eq(userId), any(), any());
    }

    private Invoice sampleInvoice() {
        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Homeowner");

        Job job = new Job();
        job.setCustomer(customer);
        job.setPropertyAddress(new Address("123 Main St", null, "Denver", "CO", "80202", "US"));

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setJob(job);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setInvoiceNumber("INV-100");
        invoice.setPaidAt(Instant.parse("2026-03-10T12:00:00Z"));
        invoice.setTotal(new BigDecimal("1234.56"));
        return invoice;
    }
}
