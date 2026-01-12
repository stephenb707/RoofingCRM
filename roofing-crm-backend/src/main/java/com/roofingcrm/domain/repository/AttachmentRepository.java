package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByTenantAndLeadAndArchivedFalse(Tenant tenant, Lead lead);

    List<Attachment> findByTenantAndJobAndArchivedFalse(Tenant tenant, Job job);

    Optional<Attachment> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
