package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndStatusAndArchivedFalse(Tenant tenant, LeadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndCustomerIdAndArchivedFalse(Tenant tenant, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Page<Lead> findByTenantAndStatusAndCustomerIdAndArchivedFalse(Tenant tenant, LeadStatus status, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Lead> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
