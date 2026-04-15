package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.AttachmentTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByTenantAndLeadAndArchivedFalse(Tenant tenant, Lead lead);

    List<Attachment> findByTenantAndJobAndArchivedFalse(Tenant tenant, Job job);

    @EntityGraph(attributePaths = {"jobCostEntry"})
    List<Attachment> findByTenantAndJobAndTagAndArchivedFalseOrderByCreatedAtDesc(Tenant tenant, Job job, AttachmentTag tag);

    Optional<Attachment> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"jobCostEntry"})
    Optional<Attachment> findByIdAndJobIdAndTenantAndArchivedFalse(UUID id, UUID jobId, Tenant tenant);

    List<Attachment> findByTenantAndJobCostEntryAndArchivedFalse(Tenant tenant, JobCostEntry jobCostEntry);

    @Query("""
            select a from Attachment a
            where a.tenant = :tenant and a.archived = false
              and (
                (a.job is not null and a.job.archived = false and a.job.id = :jobId)
                or (a.lead is not null and exists (
                    select 1 from Job j
                    where j.tenant = :tenant and j.archived = false and j.id = :jobId
                      and j.lead is not null and j.lead.id = a.lead.id))
              )
            order by a.createdAt desc
            """)
    List<Attachment> findReportableForJob(@Param("tenant") Tenant tenant, @Param("jobId") UUID jobId);

    @Query("""
            select a from Attachment a
            where a.tenant = :tenant and a.archived = false
              and (
                (a.job is not null and a.job.archived = false and a.job.customer.id = :customerId)
                or (a.lead is not null and a.lead.archived = false and a.lead.customer.id = :customerId)
              )
            order by a.createdAt desc
            """)
    List<Attachment> findReportableForCustomer(@Param("tenant") Tenant tenant, @Param("customerId") UUID customerId);
}
