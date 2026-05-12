package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.repository.PipelineStatusDefinitionRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Ensures CSV exports omit internal identifiers from visible columns.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ReportServiceImplCsvExportIdsTest {

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PipelineStatusDefinitionRepository definitionRepository;

    private ReportServiceImpl service;

    private UUID userId;
    private UUID tenantId;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(tenantAccessService, leadRepository, jobRepository, definitionRepository);
        userId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
        tenant = new Tenant();
        tenant.setId(tenantId);
    }

    @Test
    void exportLeadsCsv_omitsLeadIdAndConvertedJobUsesLabel() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID jobUuid = UUID.randomUUID();

        when(tenantAccessService.loadTenantForUserOrThrow(eq(tenantId), eq(userId))).thenReturn(tenant);
        PipelineStatusDefinition def = new PipelineStatusDefinition();
        def.setLabel("Qualified");

        Lead lead = new Lead();
        lead.setId(leadId);
        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Lee");
        lead.setCustomer(customer);
        lead.setPropertyAddress(new Address("10 Oak", null, "Boulder", "CO", "80301", "US"));
        lead.setSource(LeadSource.WEBSITE);
        lead.setStatusDefinition(def);
        lead.setCreatedAt(Instant.parse("2026-01-10T12:00:00Z"));
        lead.setUpdatedAt(Instant.parse("2026-01-11T12:00:00Z"));

        when(leadRepository.findByTenantAndArchivedFalse(eq(tenant), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(lead)));

        Job converted = new Job();
        converted.setId(jobUuid);
        converted.setCustomer(customer);
        converted.setJobType(JobType.REPAIR);
        converted.setPropertyAddress(new Address("10 Oak", null, "Boulder", "CO", "80301", "US"));
        PipelineStatusDefinition jobStatus = new PipelineStatusDefinition();
        jobStatus.setLabel("Scheduled");
        converted.setStatusDefinition(jobStatus);

        when(jobRepository.findByTenantAndLeadIdAndArchivedFalse(eq(tenant), eq(leadId)))
                .thenReturn(Optional.of(converted));

        byte[] csv = service.exportLeadsCsv(userId, tenantId, null, null, 100);
        String text = new String(csv, StandardCharsets.UTF_8).replace("\uFEFF", "");
        assertTrue(text.lines().findFirst().orElseThrow().startsWith("Customer Name,"));
        assertFalse(text.contains(leadId.toString()));
        assertFalse(text.contains(jobUuid.toString()));
        assertTrue(text.contains("Repair – 10 Oak, Boulder, CO"));
    }

    @Test
    void exportJobsCsv_omitsJobId() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(tenantAccessService.loadTenantForUserOrThrow(eq(tenantId), eq(userId))).thenReturn(tenant);

        Job job = new Job();
        job.setId(jobId);
        Customer customer = new Customer();
        customer.setFirstName("Kim");
        customer.setLastName("Ng");
        job.setCustomer(customer);
        job.setPropertyAddress(new Address("20 Elm", null, "Denver", "CO", "80202", "US"));
        job.setJobType(JobType.REPLACEMENT);
        PipelineStatusDefinition def = new PipelineStatusDefinition();
        def.setLabel("In Progress");
        job.setStatusDefinition(def);
        job.setCreatedAt(Instant.parse("2026-02-01T10:00:00Z"));
        job.setUpdatedAt(Instant.parse("2026-02-02T10:00:00Z"));

        when(jobRepository.findByTenantAndArchivedFalse(eq(tenant), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        byte[] csv = service.exportJobsCsv(userId, tenantId, null, 100);
        String text = new String(csv, StandardCharsets.UTF_8).replace("\uFEFF", "");
        assertTrue(text.lines().findFirst().orElseThrow().startsWith("Customer Name,"));
        assertFalse(text.contains(jobId.toString()));
        assertTrue(text.contains("Kim Ng"));
    }
}
