package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobCostCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobCostEntryRepository extends JpaRepository<JobCostEntry, UUID> {

    List<JobCostEntry> findByJobAndTenantAndArchivedFalseOrderByIncurredAtDescCreatedAtDesc(Job job, Tenant tenant);

    Optional<JobCostEntry> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    Optional<JobCostEntry> findByIdAndJobIdAndTenantAndArchivedFalse(UUID id, UUID jobId, Tenant tenant);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from JobCostEntry e
            where e.tenant = :tenant
              and e.job.id = :jobId
              and e.archived = false
            """)
    BigDecimal sumAmountForJob(@Param("tenant") Tenant tenant, @Param("jobId") UUID jobId);

    @Query("""
            select e.category as category, coalesce(sum(e.amount), 0) as totalAmount
            from JobCostEntry e
            where e.tenant = :tenant
              and e.job.id = :jobId
              and e.archived = false
            group by e.category
            """)
    List<CategoryTotalView> sumAmountsByCategoryForJob(@Param("tenant") Tenant tenant, @Param("jobId") UUID jobId);

    interface CategoryTotalView {
        JobCostCategory getCategory();

        BigDecimal getTotalAmount();
    }
}
