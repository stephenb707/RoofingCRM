package com.roofingcrm.service.communication;

import com.roofingcrm.api.v1.communication.CommunicationLogDto;
import com.roofingcrm.api.v1.communication.CreateCommunicationLogRequest;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface CommunicationLogService {

    CommunicationLogDto addForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId, CreateCommunicationLogRequest request);

    CommunicationLogDto addForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId, CreateCommunicationLogRequest request);

    List<CommunicationLogDto> listForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId);

    List<CommunicationLogDto> listForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId);
}
