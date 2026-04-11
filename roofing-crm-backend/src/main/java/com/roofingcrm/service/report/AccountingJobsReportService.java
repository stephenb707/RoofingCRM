package com.roofingcrm.service.report;

import com.roofingcrm.api.v1.accounting.JobAccountingSummaryDto;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.accounting.JobAccountingService;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AccountingJobsReportService {

    private static final int MAX_JOBS = 5000;
    private static final Set<UserRole> REPORT_ROLES = Set.of(UserRole.OWNER, UserRole.ADMIN);

    private final TenantAccessService tenantAccessService;
    private final JobRepository jobRepository;
    private final JobAccountingService jobAccountingService;
    private final AccountingJobsExcelExporter excelExporter;

    public AccountingJobsReportService(TenantAccessService tenantAccessService,
                                       JobRepository jobRepository,
                                       JobAccountingService jobAccountingService,
                                       AccountingJobsExcelExporter excelExporter) {
        this.tenantAccessService = tenantAccessService;
        this.jobRepository = jobRepository;
        this.jobAccountingService = jobAccountingService;
        this.excelExporter = excelExporter;
    }

    @Transactional(readOnly = true)
    public byte[] generateAccountingJobsXlsx(@NonNull UUID tenantId, @NonNull UUID userId) {
        UUID safeTenantId = Objects.requireNonNull(tenantId);
        UUID safeUserId = Objects.requireNonNull(userId);
        tenantAccessService.requireAnyRole(safeTenantId, safeUserId, Objects.requireNonNull(REPORT_ROLES),
                "You do not have permission to generate reports.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(safeTenantId, safeUserId);

        Pageable pageable = PageRequest.of(0, MAX_JOBS, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Job> jobs = jobRepository.findByTenantAndArchivedFalse(tenant, pageable).getContent();

        List<AccountingJobExportRow> rows = new ArrayList<>(jobs.size());
        for (Job job : jobs) {
            JobAccountingSummaryDto summary = jobAccountingService.getJobAccountingSummary(safeTenantId, safeUserId, job.getId());
            rows.add(new AccountingJobExportRow(job, summary));
        }
        return excelExporter.toXlsxBytes(rows);
    }

    record AccountingJobExportRow(Job job, JobAccountingSummaryDto summary) {
    }

    static String customerName(Job job) {
        if (job.getCustomer() == null) {
            return "";
        }
        String first = safe(job.getCustomer().getFirstName());
        String last = safe(job.getCustomer().getLastName());
        return (first + " " + last).trim();
    }

    static String jobLabel(Job job) {
        String type = humanizeEnum(job.getJobType());
        String addr = formatAddressOneLine(job.getPropertyAddress());
        if (type.isEmpty()) {
            return addr;
        }
        if (addr.isEmpty()) {
            return type;
        }
        return type + " – " + addr;
    }

    static String humanizeEnum(Object enumValue) {
        if (enumValue == null) {
            return "";
        }
        String raw = enumValue.toString();
        if (raw.isBlank()) {
            return "";
        }
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    static String formatAddressOneLine(Address addr) {
        if (addr == null) {
            return "";
        }
        String line1 = safe(addr.getLine1());
        String line2 = safe(addr.getLine2());
        String city = safe(addr.getCity());
        String state = safe(addr.getState());
        String zip = safe(addr.getZip());
        String country = safe(addr.getCountryCode());

        StringBuilder out = new StringBuilder();
        appendIfPresent(out, line1);
        appendIfPresent(out, line2);
        appendIfPresent(out, city);

        String stateZip = (state + " " + zip).trim();
        appendIfPresent(out, stateZip);
        appendIfPresent(out, country);

        return out.toString();
    }

    private static void appendIfPresent(StringBuilder out, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(", ");
        }
        out.append(value.trim());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static BigDecimal categoryAmount(Map<JobCostCategory, BigDecimal> totals, JobCostCategory category) {
        if (totals == null) {
            return null;
        }
        return totals.get(category);
    }
}
