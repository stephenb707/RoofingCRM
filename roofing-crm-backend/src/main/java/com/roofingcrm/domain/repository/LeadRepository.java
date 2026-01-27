package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    @Query("SELECT l FROM Lead l LEFT JOIN FETCH l.customer WHERE l.tenant = :tenant AND l.archived = false")
    Page<Lead> findByTenantAndArchivedFalse(@Param("tenant") Tenant tenant, Pageable pageable);

    @Query("SELECT l FROM Lead l LEFT JOIN FETCH l.customer WHERE l.tenant = :tenant AND l.status = :status AND l.archived = false")
    Page<Lead> findByTenantAndStatusAndArchivedFalse(@Param("tenant") Tenant tenant, @Param("status") LeadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Lead> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
