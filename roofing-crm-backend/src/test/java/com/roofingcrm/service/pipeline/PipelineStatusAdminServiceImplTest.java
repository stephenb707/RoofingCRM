package com.roofingcrm.service.pipeline;

import com.roofingcrm.api.v1.settings.ReorderPipelineStatusesRequest;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.PipelineType;
import com.roofingcrm.domain.repository.PipelineStatusDefinitionRepository;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PipelineStatusAdminServiceImplTest {

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private PipelineStatusDefinitionRepository definitionRepository;

    private PipelineStatusAdminServiceImpl service;
    private Tenant tenant;
    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new PipelineStatusAdminServiceImpl(tenantAccessService, definitionRepository);
        tenant = new Tenant();
        tenantId = UUID.randomUUID();
        tenant.setId(tenantId);
        userId = UUID.randomUUID();
        lenient()
                .when(tenantAccessService.requireAnyRole(any(), any(), any(), anyString()))
                .thenReturn(null);
    }

    @Test
    void restoreDefaults_deactivateUnusedCustomFalse_ordersActiveCustomsAfterBuiltIns_preservesCustomRelativeOrder() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        List<String> keys = PipelineStatusDefaults.orderedKeys(PipelineType.LEAD);
        PipelineStatusDefinition customEarly = customDef(PipelineType.LEAD, "C_early", 0);
        PipelineStatusDefinition customLate = customDef(PipelineType.LEAD, "C_late", 1);
        List<PipelineStatusDefinition> defs = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            defs.add(builtInDef(PipelineType.LEAD, keys.get(i), 10 + i));
        }
        defs.add(customEarly);
        defs.add(customLate);
        List<PipelineStatusDefinition> dbOrder = new ArrayList<>(defs);
        Collections.shuffle(dbOrder, new Random(42));

        when(definitionRepository.findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(
                        tenant, PipelineType.LEAD))
                .thenReturn(dbOrder);

        service.restoreDefaults(tenantId, userId, PipelineType.LEAD, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PipelineStatusDefinition>> captor = ArgumentCaptor.forClass(List.class);
        verify(definitionRepository).saveAll(captor.capture());
        List<PipelineStatusDefinition> saved = captor.getValue();
        List<PipelineStatusDefinition> byOrder =
                saved.stream().sorted(Comparator.comparingInt(PipelineStatusDefinition::getSortOrder)).toList();
        for (int i = 0; i < keys.size(); i++) {
            assertEquals(keys.get(i), byOrder.get(i).getSystemKey());
            assertEquals(PipelineStatusDefaults.defaultLabel(PipelineType.LEAD, keys.get(i)), byOrder.get(i).getLabel());
        }
        assertEquals("C_early", byOrder.get(keys.size()).getSystemKey());
        assertEquals("C_late", byOrder.get(keys.size() + 1).getSystemKey());
        Set<Integer> orders = new HashSet<>();
        for (PipelineStatusDefinition d : saved) {
            assertTrue(orders.add(d.getSortOrder()), "expected unique sort orders");
        }
    }

    @Test
    void restoreDefaults_duplicateCustomSortOrder_tiebreaksByDefinitionId() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        UUID lowId = new UUID(0L, 1L);
        UUID highId = new UUID(0L, 2L);
        PipelineStatusDefinition cLow = customDefWithId(PipelineType.JOB, "C_lo", 5, lowId);
        PipelineStatusDefinition cHigh = customDefWithId(PipelineType.JOB, "C_hi", 5, highId);
        List<PipelineStatusDefinition> defs = new ArrayList<>();
        for (String key : PipelineStatusDefaults.orderedKeys(PipelineType.JOB)) {
            defs.add(builtInDef(PipelineType.JOB, key, 100));
        }
        defs.add(cHigh);
        defs.add(cLow);
        when(definitionRepository.findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(
                        tenant, PipelineType.JOB))
                .thenReturn(defs);

        service.restoreDefaults(tenantId, userId, PipelineType.JOB, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PipelineStatusDefinition>> captor = ArgumentCaptor.forClass(List.class);
        verify(definitionRepository).saveAll(captor.capture());
        List<PipelineStatusDefinition> saved = captor.getValue();
        int nBuiltin = PipelineStatusDefaults.orderedKeys(PipelineType.JOB).size();
        UUID firstCustomAfter =
                saved.stream().filter(d -> d.getSortOrder() == nBuiltin).findFirst().orElseThrow().getId();
        UUID secondCustomAfter = saved.stream()
                .filter(d -> d.getSortOrder() == nBuiltin + 1)
                .findFirst()
                .orElseThrow()
                .getId();
        assertEquals(lowId, firstCustomAfter);
        assertEquals(highId, secondCustomAfter);
    }

    @Test
    void reorder_doesNotLookupEachIdFromRepository() {
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        PipelineStatusDefinition a = builtInDef(PipelineType.JOB, "UNSCHEDULED", 0);
        PipelineStatusDefinition b = builtInDef(PipelineType.JOB, "SCHEDULED", 1);
        List<PipelineStatusDefinition> all = new ArrayList<>(List.of(a, b));
        when(definitionRepository.findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(
                        tenant, PipelineType.JOB))
                .thenReturn(all);

        ReorderPipelineStatusesRequest req = new ReorderPipelineStatusesRequest();
        req.setPipelineType(PipelineType.JOB);
        req.setOrderedDefinitionIds(List.of(b.getId(), a.getId()));

        service.reorder(tenantId, userId, req);

        verify(definitionRepository, never()).findByIdAndTenantAndArchivedFalse(any(), eq(tenant));
        assertEquals(0, b.getSortOrder());
        assertEquals(1, a.getSortOrder());
    }

    private static PipelineStatusDefinition builtInDef(PipelineType type, String key, int sort) {
        PipelineStatusDefinition d = new PipelineStatusDefinition();
        d.setId(UUID.randomUUID());
        d.setPipelineType(type);
        d.setBuiltIn(true);
        d.setSystemKey(key);
        d.setLabel("wrong-label");
        d.setSortOrder(sort);
        d.setActive(true);
        return d;
    }

    private static PipelineStatusDefinition customDef(PipelineType type, String key, int sort) {
        PipelineStatusDefinition d = new PipelineStatusDefinition();
        d.setId(UUID.randomUUID());
        d.setPipelineType(type);
        d.setBuiltIn(false);
        d.setSystemKey(key);
        d.setLabel(key);
        d.setSortOrder(sort);
        d.setActive(true);
        return d;
    }

    private static PipelineStatusDefinition customDefWithId(
            PipelineType type, String key, int sort, UUID id) {
        PipelineStatusDefinition d = customDef(type, key, sort);
        d.setId(id);
        return d;
    }
}
