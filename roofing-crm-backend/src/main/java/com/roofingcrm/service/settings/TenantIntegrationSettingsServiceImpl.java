package com.roofingcrm.service.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.api.v1.settings.integrations.IntegrationSettingsDto;
import com.roofingcrm.api.v1.settings.integrations.UpdateIntegrationSettingsRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantIntegrationConnection;
import com.roofingcrm.domain.enums.IntegrationConnectionStatus;
import com.roofingcrm.domain.enums.IntegrationProvider;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantIntegrationConnectionRepository;
import com.roofingcrm.integrations.IntegrationSecretProtector;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class TenantIntegrationSettingsServiceImpl implements TenantIntegrationSettingsService {

    private static final Set<UserRole> ADMIN_ROLES = Set.of(UserRole.OWNER, UserRole.ADMIN);

    private final TenantAccessService tenantAccessService;
    private final TenantIntegrationConnectionRepository connectionRepository;
    private final IntegrationSecretProtector secretProtector;
    private final ObjectMapper objectMapper;

    @Autowired
    public TenantIntegrationSettingsServiceImpl(
            TenantAccessService tenantAccessService,
            TenantIntegrationConnectionRepository connectionRepository,
            IntegrationSecretProtector secretProtector,
            ObjectMapper objectMapper) {
        this.tenantAccessService = tenantAccessService;
        this.connectionRepository = connectionRepository;
        this.secretProtector = secretProtector;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IntegrationSettingsDto> listSettings(@NonNull UUID tenantId, @NonNull UUID userId) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Map<IntegrationProvider, TenantIntegrationConnection> existing = connectionRepository.findByTenant(tenant).stream()
                .collect(Collectors.toMap(
                        (@NonNull TenantIntegrationConnection connection) -> connection.getProvider(),
                        connection -> connection,
                        (first, duplicate) -> first));
        return Stream.of(IntegrationProvider.values())
                .map(p -> toDto(p, existing.get(p)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationSettingsDto getSettings(@NonNull UUID tenantId, @NonNull UUID userId, IntegrationProvider provider) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Optional<TenantIntegrationConnection> row = connectionRepository.findByTenantAndProvider(tenant, provider);
        return toDto(provider, row.orElse(null));
    }

    @Override
    public IntegrationSettingsDto updateSettings(@NonNull UUID tenantId, @NonNull UUID userId,
                                                 @NonNull IntegrationProvider provider,
                                                 @NonNull UpdateIntegrationSettingsRequest request) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        TenantIntegrationConnection entity = connectionRepository.findByTenantAndProvider(tenant, provider)
                .orElseGet(() -> {
                    TenantIntegrationConnection c = new TenantIntegrationConnection();
                    c.setTenant(tenant);
                    c.setProvider(provider);
                    return c;
                });

        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getDisplayName() != null) {
            entity.setDisplayName(request.getDisplayName().isBlank() ? null : request.getDisplayName().trim());
        }
        if (request.getConfig() != null && !request.getConfig().isEmpty()) {
            Map<String, Object> merged = new LinkedHashMap<>();
            if (entity.getConfigJson() != null) {
                merged.putAll(entity.getConfigJson());
            }
            merged.putAll(request.getConfig());
            entity.setConfigJson(merged);
        }
        if (request.getSecrets() != null && request.getSecrets().entrySet().stream()
                .anyMatch(e -> e.getValue() != null && !e.getValue().isBlank())) {
            Map<String, String> mergedSecrets = new LinkedHashMap<>();
            if (entity.getEncryptedSecretJson() != null && !entity.getEncryptedSecretJson().isBlank()) {
                try {
                    String prev = secretProtector.unprotect(entity.getEncryptedSecretJson());
                    mergedSecrets.putAll(objectMapper.readValue(prev, new TypeReference<Map<String, String>>() {
                    }));
                } catch (Exception ignored) {
                    /* replace on unreadable legacy */
                }
            }
            for (Map.Entry<String, String> e : request.getSecrets().entrySet()) {
                if (e.getValue() != null && !e.getValue().isBlank()) {
                    mergedSecrets.put(e.getKey(), e.getValue());
                }
            }
            try {
                String payload = objectMapper.writeValueAsString(mergedSecrets);
                entity.setEncryptedSecretJson(secretProtector.protect(payload));
            } catch (Exception ex) {
                throw new IllegalStateException("Could not persist integration secrets", ex);
            }
        }

        if (entity.getStatus() == IntegrationConnectionStatus.DISABLED && Boolean.TRUE.equals(request.getEnabled())) {
            entity.setStatus(IntegrationConnectionStatus.NOT_CONFIGURED);
        } else if (entity.getStatus() == null) {
            entity.setStatus(IntegrationConnectionStatus.NOT_CONFIGURED);
        }

        entity.setLastError(null);
        TenantIntegrationConnection saved = connectionRepository.save(entity);
        return toDto(provider, saved);
    }

    @Override
    public IntegrationSettingsDto disableIntegration(@NonNull UUID tenantId, @NonNull UUID userId,
                                                    @NonNull IntegrationProvider provider) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        TenantIntegrationConnection entity = connectionRepository.findByTenantAndProvider(tenant, provider)
                .orElseGet(() -> {
                    TenantIntegrationConnection c = new TenantIntegrationConnection();
                    c.setTenant(tenant);
                    c.setProvider(provider);
                    return c;
                });
        entity.setEnabled(false);
        entity.setStatus(IntegrationConnectionStatus.DISABLED);
        TenantIntegrationConnection saved = connectionRepository.save(entity);
        return toDto(provider, saved);
    }

    private void requireAdmin(UUID tenantId, UUID userId) {
        tenantAccessService.requireAnyRole(
                Objects.requireNonNull(tenantId),
                Objects.requireNonNull(userId),
                Objects.requireNonNull(ADMIN_ROLES),
                "You do not have permission to manage integration settings.");
    }

    private IntegrationSettingsDto toDto(IntegrationProvider provider, TenantIntegrationConnection row) {
        IntegrationSettingsDto dto = new IntegrationSettingsDto();
        dto.setProvider(provider);
        dto.setHumanLabel(humanLabel(provider));
        dto.setCategory(categoryOf(provider));
        if (row == null) {
            dto.setEnabled(false);
            dto.setStatus(IntegrationConnectionStatus.NOT_CONFIGURED);
            dto.setHasCredentials(false);
            dto.setConfig(Map.of());
            return dto;
        }
        dto.setEnabled(row.isEnabled());
        dto.setStatus(row.getStatus());
        dto.setDisplayName(row.getDisplayName());
        dto.setHasCredentials(row.getEncryptedSecretJson() != null && !row.getEncryptedSecretJson().isBlank());
        dto.setLastConnectedAt(row.getLastConnectedAt());
        dto.setLastError(row.getLastError());
        dto.setConfig(row.getConfigJson() != null ? new LinkedHashMap<>(row.getConfigJson()) : Map.of());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }

    private static String humanLabel(IntegrationProvider p) {
        return switch (p) {
            case QUICKBOOKS -> "QuickBooks";
            case TWILIO -> "SMS / Twilio";
            case HOVER -> "HOVER";
            case EAGLEVIEW -> "EagleView";
            case ROOFR -> "Roofr";
            case DOCUSIGN -> "DocuSign";
            case GOOGLE_REVIEWS -> "Google Reviews";
            case SUPPLIER_SRS -> "SRS";
            case SUPPLIER_ABC -> "ABC Supply";
            case SUPPLIER_QXO -> "QXO";
        };
    }

    private static String categoryOf(IntegrationProvider p) {
        return switch (p) {
            case QUICKBOOKS -> "Accounting";
            case TWILIO -> "Messaging";
            case HOVER, EAGLEVIEW, ROOFR -> "Measurements";
            case DOCUSIGN -> "E-sign";
            case GOOGLE_REVIEWS -> "Reputation";
            case SUPPLIER_SRS, SUPPLIER_ABC, SUPPLIER_QXO -> "Suppliers";
        };
    }
}
