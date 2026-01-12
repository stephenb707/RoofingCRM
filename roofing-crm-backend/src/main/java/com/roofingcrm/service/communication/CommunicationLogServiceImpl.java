package com.roofingcrm.service.communication;

import com.roofingcrm.api.v1.communication.CommunicationLogDto;
import com.roofingcrm.api.v1.communication.CreateCommunicationLogRequest;
import com.roofingcrm.domain.entity.CommunicationLog;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.CommunicationLogRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CommunicationLogServiceImpl implements CommunicationLogService {

    private final TenantAccessService tenantAccessService;
    private final CommunicationLogRepository communicationLogRepository;
    private final LeadRepository leadRepository;
    private final JobRepository jobRepository;

    public CommunicationLogServiceImpl(TenantAccessService tenantAccessService,
                                        CommunicationLogRepository communicationLogRepository,
                                        LeadRepository leadRepository,
                                        JobRepository jobRepository) {
        this.tenantAccessService = tenantAccessService;
        this.communicationLogRepository = communicationLogRepository;
        this.leadRepository = leadRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    public CommunicationLogDto addForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId, 
                                           CreateCommunicationLogRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        CommunicationLog log = createLog(tenant, userId, request);
        log.setLead(lead);

        log = communicationLogRepository.save(log);
        return toDto(log);
    }

    @Override
    public CommunicationLogDto addForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId,
                                          CreateCommunicationLogRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        CommunicationLog log = createLog(tenant, userId, request);
        log.setJob(job);

        log = communicationLogRepository.save(log);
        return toDto(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunicationLogDto> listForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        return communicationLogRepository.findByTenantAndLeadOrderByOccurredAtDesc(tenant, lead)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommunicationLogDto> listForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return communicationLogRepository.findByTenantAndJobOrderByOccurredAtDesc(tenant, job)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private CommunicationLog createLog(Tenant tenant, UUID userId, CreateCommunicationLogRequest request) {
        CommunicationLog log = new CommunicationLog();
        log.setTenant(tenant);
        log.setCreatedByUserId(userId);
        log.setUpdatedByUserId(userId);
        log.setChannel(request.getChannel());
        log.setDirection(request.getDirection());
        log.setSubject(request.getSubject());
        log.setBody(request.getBody());
        log.setOccurredAt(request.getOccurredAt() != null ? request.getOccurredAt() : Instant.now());
        return log;
    }

    private CommunicationLogDto toDto(CommunicationLog entity) {
        CommunicationLogDto dto = new CommunicationLogDto();
        dto.setId(entity.getId());
        dto.setChannel(entity.getChannel());
        dto.setDirection(entity.getDirection());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setOccurredAt(entity.getOccurredAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getLead() != null) {
            dto.setLeadId(entity.getLead().getId());
        }
        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
        }

        return dto;
    }
}
