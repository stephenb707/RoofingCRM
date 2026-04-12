package com.roofingcrm.service.report;

import com.roofingcrm.domain.enums.LeadSource;

import java.util.UUID;

public interface ReportService {

    byte[] exportLeadsCsv(UUID userId, UUID tenantId, UUID statusDefinitionId, LeadSource source, int limit);

    byte[] exportJobsCsv(UUID userId, UUID tenantId, UUID statusDefinitionId, int limit);
}
