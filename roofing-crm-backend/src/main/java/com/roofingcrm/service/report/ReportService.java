package com.roofingcrm.service.report;

import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;

import java.util.UUID;

public interface ReportService {

    /**
     * Exports leads as CSV, optionally filtered by status and source.
     */
    byte[] exportLeadsCsv(UUID userId, UUID tenantId, LeadStatus status, LeadSource source, int limit);

    /**
     * Exports jobs as CSV, optionally filtered by status.
     */
    byte[] exportJobsCsv(UUID userId, UUID tenantId, JobStatus status, int limit);
}
