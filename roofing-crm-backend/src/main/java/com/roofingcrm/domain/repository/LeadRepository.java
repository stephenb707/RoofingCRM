package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    Page<Lead> findByTenantAndStatusAndArchivedFalse(Tenant tenant, LeadStatus status, Pageable pageable);

    Optional<Lead> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
