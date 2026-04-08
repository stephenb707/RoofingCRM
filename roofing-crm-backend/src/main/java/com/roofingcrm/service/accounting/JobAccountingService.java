package com.roofingcrm.service.accounting;

import com.roofingcrm.api.v1.accounting.CreateJobCostEntryRequest;
import com.roofingcrm.api.v1.accounting.JobAccountingSummaryDto;
import com.roofingcrm.api.v1.accounting.JobCostEntryDto;
import com.roofingcrm.api.v1.accounting.UpdateJobCostEntryRequest;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface JobAccountingService {

    JobAccountingSummaryDto getJobAccountingSummary(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    List<JobCostEntryDto> listJobCostEntries(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    JobCostEntryDto createJobCostEntry(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, CreateJobCostEntryRequest request);

    JobCostEntryDto updateJobCostEntry(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID costEntryId, UpdateJobCostEntryRequest request);

    void deleteJobCostEntry(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID costEntryId);
}
