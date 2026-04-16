package com.roofingcrm.service.job;

import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.job.UpdateJobRequest;
import com.roofingcrm.api.v1.common.PickerItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JobService {

    JobDto createJob(@NonNull UUID tenantId, @NonNull UUID userId, CreateJobRequest request);

    JobDto updateJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UpdateJobRequest request);

    JobDto getJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    Page<JobDto> listJobs(
            @NonNull UUID tenantId,
            @NonNull UUID userId,
            UUID statusDefinitionIdFilter,
            UUID customerIdFilter,
            @NonNull Pageable pageable);

    Page<JobDto> listScheduleJobs(@NonNull UUID tenantId, @NonNull UUID userId,
                                  @NonNull LocalDate startDate, @NonNull LocalDate endDate,
                                  UUID statusDefinitionId, String crewName, boolean includeUnscheduled,
                                  @NonNull Pageable pageable);

    List<JobDto> listSchedule(@NonNull UUID tenantId, @NonNull UUID userId,
                              @NonNull LocalDate from, @NonNull LocalDate to,
                              UUID statusDefinitionId, String crewName, boolean includeUnscheduled);

    JobDto updateJobStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID newStatusDefinitionId);

    List<PickerItemDto> searchJobsForPicker(@NonNull UUID tenantId, @NonNull UUID userId, String q, int limit);
}
