package com.roofingcrm.service.settings;

import com.roofingcrm.api.v1.settings.AppPreferencesDto;
import com.roofingcrm.api.v1.settings.UpdateAppPreferencesRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantAppPreferences;
import com.roofingcrm.domain.repository.TenantAppPreferencesRepository;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AppPreferencesServiceImplTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private TenantAppPreferencesRepository preferencesRepository;

    private AppPreferencesServiceImpl service;

    private Tenant tenant;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AppPreferencesServiceImpl(tenantAccessService, preferencesRepository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme Roofing");
    }

    @Test
    void getPreferences_returnsDefaults_whenNoneSaved() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.empty());

        AppPreferencesDto dto = service.getPreferences(tenantId, userId);

        assertNotNull(dto);
        assertNull(dto.getUpdatedAt());
        assertNotNull(dto.getDashboard());
        assertNotNull(dto.getJobsList());
        assertNotNull(dto.getLeadsList());
        assertNotNull(dto.getCustomersList());
        assertNotNull(dto.getTasksList());
        assertNotNull(dto.getEstimatesList());
        assertNotNull(dto.getPipeline());

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertTrue(widgets.contains("metrics"));
        assertTrue(widgets.contains("recentLeads"));

        @SuppressWarnings("unchecked")
        List<String> jobFields = (List<String>) dto.getJobsList().get("visibleFields");
        assertTrue(jobFields.contains("type"));
        assertTrue(jobFields.contains("status"));
    }

    @Test
    void getPreferences_returnsSavedPreferences() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        TenantAppPreferences entity = new TenantAppPreferences();
        entity.setTenant(tenant);
        entity.setPreferences(Map.of(
                "dashboard", Map.of("widgets", List.of("metrics")),
                "jobsList", Map.of("visibleFields", List.of("status"))
        ));
        entity.setUpdatedAt(Instant.now());

        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.of(entity));

        AppPreferencesDto dto = service.getPreferences(tenantId, userId);

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(List.of("metrics"), widgets);

        @SuppressWarnings("unchecked")
        List<String> jobFields = (List<String>) dto.getJobsList().get("visibleFields");
        assertEquals(List.of("status"), jobFields);

        assertNotNull(dto.getLeadsList());
        assertNotNull(dto.getPipeline());
    }

    @Test
    void updatePreferences_createsNewEntity_whenNoneSaved() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setDashboard(Map.of("widgets", List.of("metrics", "openTasks")));

        AppPreferencesDto dto = service.updatePreferences(tenantId, userId, request);

        ArgumentCaptor<TenantAppPreferences> cap = ArgumentCaptor.forClass(TenantAppPreferences.class);
        verify(preferencesRepository).save(cap.capture());
        TenantAppPreferences saved = cap.getValue();
        assertEquals(tenant, saved.getTenant());
        assertNotNull(saved.getPreferences().get("dashboard"));

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(List.of("metrics", "openTasks"), widgets);
    }

    @Test
    void updatePreferences_mergesPartialUpdate() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        TenantAppPreferences existing = new TenantAppPreferences();
        existing.setTenant(tenant);
        existing.setPreferences(new LinkedHashMap<>(Map.of(
                "dashboard", Map.of("widgets", List.of("metrics")),
                "jobsList", Map.of("visibleFields", List.of("type", "status"))
        )));
        existing.setUpdatedAt(Instant.now());

        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setJobsList(Map.of("visibleFields", List.of("status", "propertyAddress")));

        AppPreferencesDto dto = service.updatePreferences(tenantId, userId, request);

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(List.of("metrics"), widgets);

        @SuppressWarnings("unchecked")
        List<String> jobFields = (List<String>) dto.getJobsList().get("visibleFields");
        assertEquals(List.of("status", "propertyAddress"), jobFields);
    }

    @Test
    void updatePreferences_requiresAdminRole() {
        doThrow(new SecurityException("forbidden"))
                .when(tenantAccessService)
                .requireAnyRole(eq(tenantId), eq(userId), anySet(), anyString());

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setDashboard(Map.of("widgets", List.of()));

        assertThrows(SecurityException.class,
                () -> service.updatePreferences(tenantId, userId, request));

        verify(preferencesRepository, never()).save(any());
    }
}
