package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"job", "job.customer", "estimate", "items"})
    Optional<Invoice> findDetailedByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"job", "job.customer", "items"})
    Optional<Invoice> findByPublicTokenAndPublicEnabledTrueAndArchivedFalse(String token);

    List<Invoice> findByTenantAndJobIdAndArchivedFalseOrderByCreatedAtDesc(Tenant tenant, UUID jobId);

    @Query("""
        select i from Invoice i
        where i.tenant = :tenant
          and i.archived = false
          and (:jobId is null or i.job.id = :jobId)
          and (:status is null or i.status = :status)
        order by i.createdAt desc
        """)
    Page<Invoice> findByTenantAndArchivedFalseWithFilters(
            @Param("tenant") Tenant tenant,
            @Param("jobId") UUID jobId,
            @Param("status") InvoiceStatus status,
            Pageable pageable);

    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(i.invoice_number FROM 5) AS bigint)), 0) FROM invoices i WHERE i.tenant_id = :tenantId", nativeQuery = true)
    long findMaxInvoiceNumberSuffix(@Param("tenantId") UUID tenantId);

    @Query("""
            select coalesce(sum(i.total), 0)
            from Invoice i
            where i.tenant = :tenant
              and i.job.id = :jobId
              and i.archived = false
              and i.status <> com.roofingcrm.domain.enums.InvoiceStatus.VOID
            """)
    BigDecimal sumNonVoidTotalForJob(@Param("tenant") Tenant tenant, @Param("jobId") UUID jobId);

    @Query("""
            select coalesce(sum(i.total), 0)
            from Invoice i
            where i.tenant = :tenant
              and i.job.id = :jobId
              and i.archived = false
              and i.status = com.roofingcrm.domain.enums.InvoiceStatus.PAID
            """)
    BigDecimal sumPaidTotalForJob(@Param("tenant") Tenant tenant, @Param("jobId") UUID jobId);

    @Query(value = """
            SELECT DISTINCT CAST(EXTRACT(YEAR FROM i.paid_at) AS int) AS year
            FROM invoices i
            WHERE i.tenant_id = :tenantId
              AND i.archived = false
              AND i.status = 'PAID'
              AND i.paid_at IS NOT NULL
            ORDER BY year DESC
            """, nativeQuery = true)
    List<Integer> findPaidInvoiceYears(@Param("tenantId") UUID tenantId);

    @Query("""
            select i from Invoice i
            join fetch i.job j
            join fetch j.customer c
            where i.tenant = :tenant
              and i.archived = false
              and i.status = com.roofingcrm.domain.enums.InvoiceStatus.PAID
              and i.paidAt >= :start
              and i.paidAt < :end
            order by i.paidAt asc, i.invoiceNumber asc
            """)
    List<Invoice> findPaidInvoicesForYear(
            @Param("tenant") Tenant tenant,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
