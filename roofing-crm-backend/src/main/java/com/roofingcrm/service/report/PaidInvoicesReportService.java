package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.service.exception.NoPaidInvoicesForYearException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class PaidInvoicesReportService {

    private final TenantAccessService tenantAccessService;
    private final InvoiceRepository invoiceRepository;
    private final PaidInvoicesPdfGenerator paidInvoicesPdfGenerator;

    public PaidInvoicesReportService(TenantAccessService tenantAccessService,
                                     InvoiceRepository invoiceRepository,
                                     PaidInvoicesPdfGenerator paidInvoicesPdfGenerator) {
        this.tenantAccessService = tenantAccessService;
        this.invoiceRepository = invoiceRepository;
        this.paidInvoicesPdfGenerator = paidInvoicesPdfGenerator;
    }

    @Transactional(readOnly = true)
    public List<Integer> getPaidInvoiceYears(UUID tenantId, UUID userId) {
        UUID safeTenantId = Objects.requireNonNull(tenantId);
        UUID safeUserId = Objects.requireNonNull(userId);
        tenantAccessService.requireAnyRole(safeTenantId, safeUserId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN)),
                "You do not have permission to generate reports.");
        tenantAccessService.loadTenantForUserOrThrow(safeTenantId, safeUserId);
        return invoiceRepository.findPaidInvoiceYears(safeTenantId);
    }

    @Transactional(readOnly = true)
    public byte[] generatePaidInvoicesYearPdf(UUID tenantId, UUID userId, int year) {
        UUID safeTenantId = Objects.requireNonNull(tenantId);
        UUID safeUserId = Objects.requireNonNull(userId);
        tenantAccessService.requireAnyRole(safeTenantId, safeUserId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN)),
                "You do not have permission to generate reports.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(safeTenantId, safeUserId);

        Instant start = Year.of(year).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = Year.of(year + 1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Invoice> invoices = invoiceRepository.findPaidInvoicesForYear(tenant, start, end);
        if (invoices.isEmpty()) {
            throw new NoPaidInvoicesForYearException(year);
        }
        return paidInvoicesPdfGenerator.generate(tenant, year, invoices);
    }
}
