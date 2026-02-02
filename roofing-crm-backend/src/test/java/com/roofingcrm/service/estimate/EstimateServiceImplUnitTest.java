package com.roofingcrm.service.estimate;

import com.roofingcrm.api.v1.estimate.ShareEstimateRequest;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class EstimateServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private EstimateRepository estimateRepository;
    @Mock
    private ActivityEventService activityEventService;

    private EstimateServiceImpl service;
    private Tenant tenant;
    private Estimate estimate;
    private Job job;
    private UUID tenantId;
    private UUID userId;
    private UUID estimateId;

    @BeforeEach
    void setUp() {
        service = new EstimateServiceImpl(tenantAccessService, jobRepository, estimateRepository, activityEventService);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        estimateId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);

        job = new Job();
        job.setId(UUID.randomUUID());

        estimate = new Estimate();
        estimate.setId(estimateId);
        estimate.setTenant(tenant);
        estimate.setJob(job);
        estimate.setEstimateNumber("EST-1001");
        estimate.setStatus(EstimateStatus.SENT);
    }

    @Test
    void shareEstimate_whenFieldTech_throws403() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenThrow(new TenantAccessDeniedException("You do not have permission to share estimates."));

        ShareEstimateRequest req = new ShareEstimateRequest();
        req.setExpiresInDays(14);

        assertThrows(TenantAccessDeniedException.class, () ->
                service.shareEstimate(tenantId, userId, estimateId, req));

        verify(estimateRepository, never()).save(any());
    }

    @Test
    void shareEstimate_whenEstimateNotFound_throws404() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(estimateRepository.findByIdAndTenantAndArchivedFalse(estimateId, tenant))
                .thenReturn(Optional.empty());

        ShareEstimateRequest req = new ShareEstimateRequest();

        assertThrows(ResourceNotFoundException.class, () ->
                service.shareEstimate(tenantId, userId, estimateId, req));

        verify(estimateRepository, never()).save(any());
    }
}
