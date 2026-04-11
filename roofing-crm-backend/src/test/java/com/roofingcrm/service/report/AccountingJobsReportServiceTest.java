package com.roofingcrm.service.report;

import com.roofingcrm.api.v1.accounting.JobAccountingSummaryDto;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.JobType;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.accounting.JobAccountingService;
import com.roofingcrm.service.tenant.TenantAccessDeniedException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AccountingJobsReportServiceTest {

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobAccountingService jobAccountingService;

    private final AccountingJobsExcelExporter excelExporter = new AccountingJobsExcelExporter();

    private AccountingJobsReportService service;

    private UUID tenantId;
    private UUID userId;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        service = new AccountingJobsReportService(tenantAccessService, jobRepository, jobAccountingService, excelExporter);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jobId = UUID.randomUUID();
    }

    @Test
    void generateAccountingJobsXlsx_requiresOwnerOrAdmin() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenThrow(new TenantAccessDeniedException("You do not have permission to generate reports."));

        assertThrows(TenantAccessDeniedException.class,
                () -> service.generateAccountingJobsXlsx(tenantId, userId));
    }

    @Test
    void generateAccountingJobsXlsx_writesHeadersAndAccountingRow() throws Exception {
        stubAccessOk();
        Tenant tenant = mock(Tenant.class);
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);

        Job job = buildSampleJob();
        when(jobRepository.findByTenantAndArchivedFalse(eq(tenant), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job)));

        JobAccountingSummaryDto dto = buildSampleSummary();
        when(jobAccountingService.getJobAccountingSummary(tenantId, userId, jobId)).thenReturn(dto);

        byte[] xlsx = service.generateAccountingJobsXlsx(tenantId, userId);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            assertEquals("Job ID", header.getCell(0).getStringCellValue());
            assertEquals("Agreed Amount", header.getCell(CellReference.convertColStringToIndex("E")).getStringCellValue());
            assertEquals("Materials Cost", header.getCell(CellReference.convertColStringToIndex("M")).getStringCellValue());

            Row data = sheet.getRow(1);
            assertEquals(jobId.toString(), data.getCell(0).getStringCellValue());
            assertEquals(new BigDecimal("1000.00"), BigDecimal.valueOf(data.getCell(4).getNumericCellValue()).setScale(2, java.math.RoundingMode.UNNECESSARY));
            assertEquals(new BigDecimal("100.00"), BigDecimal.valueOf(data.getCell(12).getNumericCellValue()).setScale(2, java.math.RoundingMode.UNNECESSARY));
        }

        verify(jobAccountingService).getJobAccountingSummary(tenantId, userId, jobId);
    }

    private void stubAccessOk() {
        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString()))
                .thenReturn(mock(com.roofingcrm.domain.entity.TenantUserMembership.class));
    }

    private Job buildSampleJob() {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        var customer = mock(com.roofingcrm.domain.entity.Customer.class);
        when(customer.getFirstName()).thenReturn("Jane");
        when(customer.getLastName()).thenReturn("Doe");
        when(job.getCustomer()).thenReturn(customer);
        when(job.getJobType()).thenReturn(JobType.REPAIR);
        when(job.getStatus()).thenReturn(JobStatus.COMPLETED);
        Address addr = new Address();
        addr.setLine1("100 Pine Rd");
        addr.setCity("Denver");
        addr.setState("CO");
        when(job.getPropertyAddress()).thenReturn(addr);
        when(job.getUpdatedAt()).thenReturn(Instant.parse("2026-03-15T14:30:00Z"));
        return job;
    }

    private JobAccountingSummaryDto buildSampleSummary() {
        JobAccountingSummaryDto dto = new JobAccountingSummaryDto();
        dto.setAgreedAmount(new BigDecimal("1000.00"));
        dto.setInvoicedAmount(new BigDecimal("1000.00"));
        dto.setPaidAmount(new BigDecimal("900.00"));
        dto.setTotalCosts(new BigDecimal("400.00"));
        dto.setActualProfit(new BigDecimal("500.00"));
        dto.setProjectedProfit(new BigDecimal("600.00"));
        dto.setActualMarginPercent(new BigDecimal("55.56"));
        dto.setProjectedMarginPercent(new BigDecimal("60.00"));
        Map<JobCostCategory, BigDecimal> totals = new EnumMap<>(JobCostCategory.class);
        for (JobCostCategory c : JobCostCategory.values()) {
            totals.put(c, BigDecimal.ZERO);
        }
        totals.put(JobCostCategory.MATERIAL, new BigDecimal("100.00"));
        dto.setCategoryTotals(totals);
        dto.setHasAcceptedEstimate(true);
        return dto;
    }
}
