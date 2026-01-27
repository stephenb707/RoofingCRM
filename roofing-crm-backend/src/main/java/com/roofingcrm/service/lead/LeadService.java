package com.roofingcrm.service.lead;

import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.UpdateLeadRequest;
import com.roofingcrm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface LeadService {

    LeadDto createLead(@NonNull UUID tenantId, @NonNull UUID userId, CreateLeadRequest request);

    LeadDto updateLead(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, UpdateLeadRequest request);

    LeadDto getLead(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId);

    Page<LeadDto> listLeads(@NonNull UUID tenantId, @NonNull UUID userId, LeadStatus statusFilter, UUID customerId, Pageable pageable);

    LeadDto updateLeadStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, LeadStatus newStatus);
}
