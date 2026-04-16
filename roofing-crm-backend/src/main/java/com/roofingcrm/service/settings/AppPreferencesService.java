package com.roofingcrm.service.settings;

import com.roofingcrm.api.v1.settings.AppPreferencesDto;
import com.roofingcrm.api.v1.settings.UpdateAppPreferencesRequest;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface AppPreferencesService {

    AppPreferencesDto getPreferences(@NonNull UUID tenantId, @NonNull UUID userId);

    AppPreferencesDto updatePreferences(@NonNull UUID tenantId, @NonNull UUID userId,
                                        @NonNull UpdateAppPreferencesRequest request);
}
