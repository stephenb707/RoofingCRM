package com.roofingcrm.service.pipeline;

import com.roofingcrm.api.v1.settings.CreatePipelineStatusRequest;
import com.roofingcrm.api.v1.settings.PipelineStatusDefinitionDto;
import com.roofingcrm.api.v1.settings.ReorderPipelineStatusesRequest;
import com.roofingcrm.api.v1.settings.UpdatePipelineStatusRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.PipelineType;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface PipelineStatusAdminService {

    List<PipelineStatusDefinitionDto> list(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull PipelineType type);

    PipelineStatusDefinitionDto create(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull CreatePipelineStatusRequest request);

    PipelineStatusDefinitionDto update(
            @NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID definitionId, @NonNull UpdatePipelineStatusRequest request);

    void reorder(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull ReorderPipelineStatusesRequest request);

    void restoreDefaults(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull PipelineType type, boolean deactivateUnusedCustom);

    void deactivate(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID definitionId);

    /** Idempotent seed for a brand-new tenant (registration). Flyway already seeds existing DBs. */
    void seedDefaultsForNewTenant(@NonNull Tenant tenant);
}
