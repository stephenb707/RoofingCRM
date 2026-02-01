package com.roofingcrm.api.v1.schedule;

import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.job.JobService;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedule")
@Validated
public class ScheduleController {

    private final JobService jobService;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public ScheduleController(JobService jobService, TenantAccessService tenantAccessService) {
        this.jobService = jobService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/jobs")
    public ResponseEntity<Page<JobDto>> listScheduleJobs(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NonNull LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @NonNull LocalDate endDate,
            @RequestParam(value = "status", required = false) JobStatus status,
            @RequestParam(value = "crewName", required = false) String crewName,
            @RequestParam(value = "includeUnscheduled", defaultValue = "false") boolean includeUnscheduled,
            @PageableDefault(size = 200) @NonNull Pageable pageable) {

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Page<JobDto> page = jobService.listScheduleJobs(
                tenantId, userId, startDate, endDate, status, crewName, includeUnscheduled, pageable);
        return ResponseEntity.ok(page);
    }
}
