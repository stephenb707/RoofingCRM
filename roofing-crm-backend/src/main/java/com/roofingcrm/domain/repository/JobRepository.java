package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    Page<Job> findByTenantAndStatusAndArchivedFalse(Tenant tenant, JobStatus status, Pageable pageable);

    Page<Job> findByTenantAndCustomerIdAndArchivedFalse(Tenant tenant, UUID customerId, Pageable pageable);

    Optional<Job> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
