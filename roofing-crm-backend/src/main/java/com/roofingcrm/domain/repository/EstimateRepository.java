package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.EstimateStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstimateRepository extends JpaRepository<Estimate, UUID> {

    List<Estimate> findByJobAndArchivedFalse(Job job);

    List<Estimate> findByTenantAndStatusAndArchivedFalse(Tenant tenant, EstimateStatus status);

    Optional<Estimate> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"job", "job.customer", "items"})
    Optional<Estimate> findDetailedByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"job", "job.customer", "items"})
    Optional<Estimate> findByPublicTokenAndPublicEnabledTrueAndArchivedFalse(String publicToken);

    @Query("""
            select e from Estimate e
            where e.job = :job
              and e.archived = false
              and e.status = com.roofingcrm.domain.enums.EstimateStatus.ACCEPTED
            order by
              case when e.decisionAt is null then 1 else 0 end,
              e.decisionAt desc,
              e.updatedAt desc,
              e.createdAt desc
            """)
    List<Estimate> findAcceptedForJobOrderForAccounting(@Param("job") Job job);
}
