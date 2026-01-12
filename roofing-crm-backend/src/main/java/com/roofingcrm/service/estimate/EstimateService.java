package com.roofingcrm.service.estimate;

import com.roofingcrm.api.v1.estimate.CreateEstimateRequest;
import com.roofingcrm.api.v1.estimate.EstimateDto;
import com.roofingcrm.api.v1.estimate.UpdateEstimateRequest;
import com.roofingcrm.domain.enums.EstimateStatus;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface EstimateService {

    EstimateDto createEstimateForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, CreateEstimateRequest request);

    EstimateDto updateEstimate(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId, UpdateEstimateRequest request);

    EstimateDto getEstimate(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId);

    List<EstimateDto> listEstimatesForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    EstimateDto updateEstimateStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID estimateId, EstimateStatus newStatus);
}
