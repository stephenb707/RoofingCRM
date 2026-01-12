package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.CommunicationLog;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunicationLogRepository extends JpaRepository<CommunicationLog, UUID> {

    List<CommunicationLog> findByTenantAndLeadOrderByOccurredAtDesc(Tenant tenant, Lead lead);

    List<CommunicationLog> findByTenantAndJobOrderByOccurredAtDesc(Tenant tenant, Job job);
}
