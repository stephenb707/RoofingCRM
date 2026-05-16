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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

        @SuppressWarnings("unchecked")
        List<String> leadsDefaults = (List<String>) dto.getLeadsList().get("visibleFields");
        assertEquals(AppPreferencesDefaults.leadsList().get("visibleFields"), leadsDefaults);

        assertNotNull(dto.getPipeline());
    }

    @Test
    void getPreferences_sanitizesStaleDashboardWidgetsFromDatabase() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        TenantAppPreferences entity = new TenantAppPreferences();
        entity.setTenant(tenant);
        entity.setPreferences(Map.of(
                "dashboard", Map.of("widgets", List.of("recentLeads", "retiredWidget", "metrics")),
                "jobsList", Map.of("visibleFields", List.of("type"))
        ));
        entity.setUpdatedAt(Instant.now());

        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.of(entity));

        AppPreferencesDto dto = service.getPreferences(tenantId, userId);

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(List.of("recentLeads", "metrics"), widgets);
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
    void updatePreferences_filtersUnknownDashboardWidgets() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setDashboard(Map.of("widgets", List.of("openTasks", "unknownWidget", "metrics")));

        AppPreferencesDto dto = service.updatePreferences(tenantId, userId, request);

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(List.of("openTasks", "metrics"), widgets);
    }

    @Test
    void updatePreferences_defaultsDashboardWhenEveryWidgetUnknown() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setDashboard(Map.of("widgets", List.of("bad", "worse")));

        AppPreferencesDto dto = service.updatePreferences(tenantId, userId, request);

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(AppPreferencesDefaults.dashboard().get("widgets"), widgets);
    }

    @Test
    void updatePreferences_filtersUnknownJobsVisibleFields() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setJobsList(Map.of("visibleFields", List.of("status", "nope", "crew")));

        AppPreferencesDto dto = service.updatePreferences(tenantId, userId, request);

        @SuppressWarnings("unchecked")
        List<String> jobFields = (List<String>) dto.getJobsList().get("visibleFields");
        assertEquals(List.of("status", "crew"), jobFields);
    }

    @Test
    void updatePreferences_removesUnknownTopLevelKeysWhenSaving() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        TenantAppPreferences existing = new TenantAppPreferences();
        existing.setTenant(tenant);
        LinkedHashMap<String, Object> prefs = new LinkedHashMap<>();
        prefs.put("junkKey", Map.of("x", 1));
        prefs.put("dashboard", Map.of("widgets", List.of("metrics")));
        existing.setPreferences(prefs);
        existing.setUpdatedAt(Instant.now());

        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.of(existing));
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setJobsList(Map.of("visibleFields", List.of("type")));

        service.updatePreferences(tenantId, userId, request);

        ArgumentCaptor<TenantAppPreferences> cap = ArgumentCaptor.forClass(TenantAppPreferences.class);
        verify(preferencesRepository).save(cap.capture());
        Map<String, Object> stored = cap.getValue().getPreferences();
        assertFalse(stored.containsKey("junkKey"));
        assertTrue(stored.containsKey("dashboard"));
        assertTrue(stored.containsKey("jobsList"));
    }

    @Test
    void getPreferences_sanitizesPipelineSectionFromDatabase() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        LinkedHashMap<String, Object> pipe = new LinkedHashMap<>();
        pipe.put("defaultView", "combined");
        pipe.put("unknownFlag", true);

        TenantAppPreferences entity = new TenantAppPreferences();
        entity.setTenant(tenant);
        entity.setPreferences(new LinkedHashMap<>(Map.of("pipeline", pipe)));
        entity.setUpdatedAt(Instant.now());

        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.of(entity));

        AppPreferencesDto dto = service.getPreferences(tenantId, userId);
        assertEquals(Map.of("defaultView", "combined"), dto.getPipeline());
    }

    @Test
    void getPreferences_dropsInvalidPipelineDefaultViewFromDatabase() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        TenantAppPreferences entity = new TenantAppPreferences();
        entity.setTenant(tenant);
        entity.setPreferences(Map.of(
                "pipeline", Map.of("defaultView", "not-a-view")));
        entity.setUpdatedAt(Instant.now());

        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.of(entity));

        assertTrue(service.getPreferences(tenantId, userId).getPipeline().isEmpty());
    }

    @Test
    void updatePreferences_deduplicatesDashboardWidgetsAndSanitizesPipeline() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(preferencesRepository.findByTenant(tenant)).thenReturn(Optional.empty());
        when(preferencesRepository.save(any())).thenAnswer(inv -> {
            TenantAppPreferences e = inv.getArgument(0);
            e.setUpdatedAt(Instant.now());
            return e;
        });

        UpdateAppPreferencesRequest request = new UpdateAppPreferencesRequest();
        request.setDashboard(Map.of("widgets", List.of("metrics", "openTasks", "metrics")));
        LinkedHashMap<String, Object> pipe = new LinkedHashMap<>();
        pipe.put("defaultView", "leads");
        pipe.put("extra", "drop-me");
        request.setPipeline(pipe);

        AppPreferencesDto dto = service.updatePreferences(tenantId, userId, request);

        @SuppressWarnings("unchecked")
        List<String> widgets = (List<String>) dto.getDashboard().get("widgets");
        assertEquals(List.of("metrics", "openTasks"), widgets);
        assertEquals(Map.of("defaultView", "leads"), dto.getPipeline());

        ArgumentCaptor<TenantAppPreferences> cap = ArgumentCaptor.forClass(TenantAppPreferences.class);
        verify(preferencesRepository).save(cap.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> storedPipe = (Map<String, Object>) cap.getValue().getPreferences().get("pipeline");
        assertEquals(Map.of("defaultView", "leads"), storedPipe);
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
