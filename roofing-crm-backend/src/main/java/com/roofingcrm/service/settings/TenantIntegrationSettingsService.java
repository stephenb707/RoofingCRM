package com.roofingcrm.service.settings;

import com.roofingcrm.api.v1.settings.integrations.IntegrationSettingsDto;
import com.roofingcrm.api.v1.settings.integrations.UpdateIntegrationSettingsRequest;
import com.roofingcrm.domain.enums.IntegrationProvider;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface TenantIntegrationSettingsService {

    List<IntegrationSettingsDto> listSettings(@NonNull UUID tenantId, @NonNull UUID userId);

    IntegrationSettingsDto getSettings(@NonNull UUID tenantId, @NonNull UUID userId, IntegrationProvider provider);

    IntegrationSettingsDto updateSettings(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull IntegrationProvider provider,
                                         @NonNull UpdateIntegrationSettingsRequest request);

    IntegrationSettingsDto disableIntegration(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull IntegrationProvider provider);
}
