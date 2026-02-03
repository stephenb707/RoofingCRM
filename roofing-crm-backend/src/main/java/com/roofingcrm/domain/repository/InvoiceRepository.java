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
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    @EntityGraph(attributePaths = {"job", "estimate", "items"})
    Optional<Invoice> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

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
}
