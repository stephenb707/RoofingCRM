package com.roofingcrm.service.pipeline;

import com.roofingcrm.api.v1.settings.CreatePipelineStatusRequest;
import com.roofingcrm.api.v1.settings.PipelineStatusDefinitionDto;
import com.roofingcrm.api.v1.settings.ReorderPipelineStatusesRequest;
import com.roofingcrm.api.v1.settings.UpdatePipelineStatusRequest;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.PipelineType;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.PipelineStatusDefinitionRepository;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PipelineStatusAdminServiceImpl implements PipelineStatusAdminService {

    private final TenantAccessService tenantAccessService;
    private final PipelineStatusDefinitionRepository definitionRepository;

    @Autowired
    public PipelineStatusAdminServiceImpl(
            TenantAccessService tenantAccessService,
            PipelineStatusDefinitionRepository definitionRepository) {
        this.tenantAccessService = tenantAccessService;
        this.definitionRepository = definitionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PipelineStatusDefinitionDto> list(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull PipelineType type) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        return definitionRepository
                .findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(tenant, type)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public PipelineStatusDefinitionDto create(
            @NonNull UUID tenantId, @NonNull UUID userId, @NonNull CreatePipelineStatusRequest request) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        PipelineType type = request.getPipelineType();
        String label = request.getLabel().trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("Label is required");
        }
        String systemKey = "C_" + UUID.randomUUID().toString().replace("-", "");
        int nextOrder = definitionRepository
                .findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(tenant, type)
                .stream()
                .mapToInt(PipelineStatusDefinition::getSortOrder)
                .max()
                .orElse(-1) + 1;

        PipelineStatusDefinition def = new PipelineStatusDefinition();
        def.setTenant(tenant);
        def.setPipelineType(type);
        def.setSystemKey(systemKey);
        def.setLabel(label);
        def.setSortOrder(nextOrder);
        def.setBuiltIn(false);
        def.setActive(true);
        PipelineStatusDefinition saved = definitionRepository.save(def);
        return toDto(saved);
    }

    @Override
    public PipelineStatusDefinitionDto update(
            @NonNull UUID tenantId,
            @NonNull UUID userId,
            @NonNull UUID definitionId,
            @NonNull UpdatePipelineStatusRequest request) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        PipelineStatusDefinition def = definitionRepository
                .findByIdAndTenantAndArchivedFalse(definitionId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Status definition not found"));
        String label = request.getLabel().trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("Label is required");
        }
        def.setLabel(label);
        return toDto(definitionRepository.save(def));
    }

    @Override
    public void reorder(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull ReorderPipelineStatusesRequest request) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        PipelineType type = request.getPipelineType();
        List<PipelineStatusDefinition> all =
                definitionRepository.findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(tenant, type);
        Set<UUID> expected = new HashSet<>();
        for (PipelineStatusDefinition d : all) {
            expected.add(d.getId());
        }
        if (!expected.equals(new HashSet<>(request.getOrderedDefinitionIds()))) {
            throw new IllegalArgumentException("Reorder list must include every status definition for this pipeline exactly once.");
        }
        List<PipelineStatusDefinition> byId = new ArrayList<>();
        for (UUID id : request.getOrderedDefinitionIds()) {
            PipelineStatusDefinition d = definitionRepository
                    .findByIdAndTenantAndArchivedFalse(id, tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Status definition not found"));
            if (d.getPipelineType() != type) {
                throw new IllegalArgumentException("Mismatched pipeline type");
            }
            byId.add(d);
        }
        for (int i = 0; i < byId.size(); i++) {
            byId.get(i).setSortOrder(i);
        }
        definitionRepository.saveAll(byId);
    }

    @Override
    public void restoreDefaults(
            @NonNull UUID tenantId, @NonNull UUID userId, @NonNull PipelineType type, boolean deactivateUnusedCustom) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        List<PipelineStatusDefinition> all =
                definitionRepository.findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(tenant, type);
        List<String> required = PipelineStatusDefaults.requiredBuiltInKeys(type);
        for (PipelineStatusDefinition def : all) {
            if (!def.isBuiltIn()) {
                continue;
            }
            String key = def.getSystemKey();
            if (!required.contains(key)) {
                continue;
            }
            def.setLabel(PipelineStatusDefaults.defaultLabel(type, key));
            def.setSortOrder(PipelineStatusDefaults.defaultSortOrder(type, key));
            def.setActive(true);
        }
        if (deactivateUnusedCustom) {
            for (PipelineStatusDefinition def : all) {
                if (def.isBuiltIn() || !def.isActive()) {
                    continue;
                }
                long usage = usageCount(tenant, type, def.getId());
                if (usage == 0) {
                    def.setActive(false);
                } else {
                    throw new IllegalStateException(
                            "Cannot deactivate custom status \"" + def.getLabel()
                                    + "\" because it is still in use. Reassign those records first.");
                }
            }
        }
        definitionRepository.saveAll(Objects.requireNonNull(all));
    }

    @Override
    public void deactivate(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID definitionId) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        PipelineStatusDefinition def = definitionRepository
                .findByIdAndTenantAndArchivedFalse(definitionId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Status definition not found"));
        if (def.isBuiltIn()) {
            throw new IllegalArgumentException("Built-in statuses cannot be removed.");
        }
        long usage = usageCount(tenant, def.getPipelineType(), def.getId());
        if (usage > 0) {
            throw new IllegalStateException("Cannot remove a status that is still assigned to leads or jobs.");
        }
        def.setActive(false);
        definitionRepository.save(def);
    }

    @Override
    public void seedDefaultsForNewTenant(@NonNull Tenant tenant) {
        for (PipelineType type : PipelineType.values()) {
            long existing = definitionRepository
                    .findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(tenant, type)
                    .size();
            if (existing > 0) {
                continue;
            }
            int order = 0;
            for (String key : PipelineStatusDefaults.orderedKeys(type)) {
                PipelineStatusDefinition def = new PipelineStatusDefinition();
                def.setTenant(tenant);
                def.setPipelineType(type);
                def.setSystemKey(key);
                def.setLabel(PipelineStatusDefaults.defaultLabel(type, key));
                def.setSortOrder(order++);
                def.setBuiltIn(true);
                def.setActive(true);
                definitionRepository.save(def);
            }
        }
    }

    private long usageCount(Tenant tenant, PipelineType type, UUID definitionId) {
        return type == PipelineType.LEAD
                ? definitionRepository.countLeadsUsingDefinition(tenant, definitionId)
                : definitionRepository.countJobsUsingDefinition(tenant, definitionId);
    }

    private void requireAdmin(UUID tenantId, UUID userId) {
        tenantAccessService.requireAnyRole(
                Objects.requireNonNull(tenantId),
                Objects.requireNonNull(userId),
                Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN)),
                "You do not have permission to manage pipeline settings.");
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
