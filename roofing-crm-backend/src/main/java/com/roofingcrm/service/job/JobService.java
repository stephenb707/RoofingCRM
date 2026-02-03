package com.roofingcrm.service.job;

import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.job.UpdateJobRequest;
import com.roofingcrm.domain.enums.JobStatus;
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

    Page<JobDto> listJobs(@NonNull UUID tenantId, @NonNull UUID userId, JobStatus statusFilter, UUID customerIdFilter, @NonNull Pageable pageable);

    Page<JobDto> listScheduleJobs(@NonNull UUID tenantId, @NonNull UUID userId,
                                  @NonNull LocalDate startDate, @NonNull LocalDate endDate,
                                  JobStatus status, String crewName, boolean includeUnscheduled,
                                  @NonNull Pageable pageable);

    /** Returns jobs for schedule view (non-paged, sorted by scheduledStartDate asc nulls last, createdAt desc). */
    List<JobDto> listSchedule(@NonNull UUID tenantId, @NonNull UUID userId,
                              @NonNull LocalDate from, @NonNull LocalDate to,
                              JobStatus status, String crewName, boolean includeUnscheduled);

    JobDto updateJobStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, JobStatus newStatus);

    List<PickerItemDto> searchJobsForPicker(@NonNull UUID tenantId, @NonNull UUID userId, String q, int limit);
}
