package com.roofingcrm.service.job;

import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.job.UpdateJobRequest;
import com.roofingcrm.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface JobService {

    JobDto createJob(@NonNull UUID tenantId, @NonNull UUID userId, CreateJobRequest request);

    JobDto updateJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UpdateJobRequest request);

    JobDto getJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    Page<JobDto> listJobs(@NonNull UUID tenantId, @NonNull UUID userId, JobStatus statusFilter, UUID customerIdFilter, @NonNull Pageable pageable);

    JobDto updateJobStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, JobStatus newStatus);
}
