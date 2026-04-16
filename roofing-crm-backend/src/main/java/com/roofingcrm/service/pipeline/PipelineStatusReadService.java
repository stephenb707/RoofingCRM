package com.roofingcrm.service.pipeline;

import com.roofingcrm.api.v1.settings.PipelineStatusDefinitionDto;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.PipelineType;
import com.roofingcrm.domain.repository.PipelineStatusDefinitionRepository;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PipelineStatusReadService {

    private final TenantAccessService tenantAccessService;
    private final PipelineStatusDefinitionRepository definitionRepository;

    @Autowired
    public PipelineStatusReadService(
            TenantAccessService tenantAccessService,
            PipelineStatusDefinitionRepository definitionRepository) {
        this.tenantAccessService = tenantAccessService;
        this.definitionRepository = definitionRepository;
    }

    public List<PipelineStatusDefinitionDto> listActive(
            @NonNull UUID tenantId, @NonNull UUID userId, @NonNull PipelineType type) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        return definitionRepository
                .findByTenantAndPipelineTypeAndActiveTrueAndArchivedFalseOrderBySortOrderAsc(tenant, type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PipelineStatusDefinitionDto toDto(PipelineStatusDefinition d) {
        PipelineStatusDefinitionDto dto = new PipelineStatusDefinitionDto();
        dto.setId(d.getId());
        dto.setPipelineType(d.getPipelineType());
        dto.setSystemKey(d.getSystemKey());
        dto.setLabel(d.getLabel());
        dto.setSortOrder(d.getSortOrder());
        dto.setBuiltIn(d.isBuiltIn());
        dto.setActive(d.isActive());
        return dto;
    }
}
