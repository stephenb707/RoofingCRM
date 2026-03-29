package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.JobCostEntry;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.AttachmentTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
