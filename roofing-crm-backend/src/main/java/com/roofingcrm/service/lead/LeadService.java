package com.roofingcrm.service.lead;

import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.UpdateLeadRequest;
import com.roofingcrm.domain.enums.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LeadService {

    LeadDto createLead(UUID tenantId, UUID userId, CreateLeadRequest request);

    LeadDto updateLead(UUID tenantId, UUID userId, UUID leadId, UpdateLeadRequest request);

    LeadDto getLead(UUID tenantId, UUID leadId);

    Page<LeadDto> listLeads(UUID tenantId, LeadStatus statusFilter, Pageable pageable);

    LeadDto updateLeadStatus(UUID tenantId, UUID userId, UUID leadId, LeadStatus newStatus);
}
