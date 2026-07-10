package com.roofingcrm.domain.repository;

import com.roofingcrm.background.BackgroundJobStatus;
import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationBackgroundJobRepository extends JpaRepository<IntegrationBackgroundJob, UUID> {

    List<IntegrationBackgroundJob> findByTenantAndStatus(Tenant tenant, BackgroundJobStatus status);

    Optional<IntegrationBackgroundJob> findByIdAndTenant(UUID id, Tenant tenant);
}
