package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.customer WHERE j.tenant = :tenant AND j.archived = false")
    Page<Job> findByTenantAndArchivedFalse(@Param("tenant") Tenant tenant, Pageable pageable);

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.customer WHERE j.tenant = :tenant AND j.status = :status AND j.archived = false")
    Page<Job> findByTenantAndStatusAndArchivedFalse(@Param("tenant") Tenant tenant, @Param("status") JobStatus status, Pageable pageable);

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.customer WHERE j.tenant = :tenant AND j.customer.id = :customerId AND j.archived = false")
    Page<Job> findByTenantAndCustomerIdAndArchivedFalse(@Param("tenant") Tenant tenant, @Param("customerId") UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer"})
    Optional<Job> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
