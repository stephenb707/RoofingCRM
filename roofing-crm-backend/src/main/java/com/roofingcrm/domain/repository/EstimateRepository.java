package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.EstimateStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstimateRepository extends JpaRepository<Estimate, UUID> {

    List<Estimate> findByJobAndArchivedFalse(Job job);

    List<Estimate> findByTenantAndStatusAndArchivedFalse(Tenant tenant, EstimateStatus status);

    Optional<Estimate> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"job", "job.customer", "items"})
    Optional<Estimate> findByPublicTokenAndPublicEnabledTrueAndArchivedFalse(String publicToken);
}
