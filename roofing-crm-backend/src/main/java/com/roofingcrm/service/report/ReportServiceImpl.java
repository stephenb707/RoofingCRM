package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.tenant.TenantAccessService;
import com.roofingcrm.util.CsvUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;

@Service
public class ReportServiceImpl implements ReportService {

    private final TenantAccessService tenantAccessService;
    private final LeadRepository leadRepository;
    private final JobRepository jobRepository;

    @Autowired
    public ReportServiceImpl(TenantAccessService tenantAccessService,
                             LeadRepository leadRepository,
                             JobRepository jobRepository) {
        this.tenantAccessService = tenantAccessService;
        this.leadRepository = leadRepository;
        this.jobRepository = jobRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportLeadsCsv(UUID userId, UUID tenantId, LeadStatus status, LeadSource source, int limit) {
        int capped = Math.min(Math.max(limit, 1), 5000);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(Objects.requireNonNull(tenantId), Objects.requireNonNull(userId));

        Pageable pageable = PageRequest.of(0, capped, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Lead> leads;
        if (status != null && source != null) {
            leads = leadRepository.findByTenantAndStatusAndSourceAndArchivedFalse(tenant, status, source, pageable).getContent();
        } else if (status != null) {
            leads = leadRepository.findByTenantAndStatusAndArchivedFalse(tenant, status, pageable).getContent();
        } else if (source != null) {
            leads = leadRepository.findByTenantAndSourceAndArchivedFalse(tenant, source, pageable).getContent();
        } else {
            leads = leadRepository.findByTenantAndArchivedFalse(tenant, pageable).getContent();
        }

        // Professional CSV output with improved readability:
        // - Human-readable headers with clear labeling
        // - Friendly date/time formatting (e.g., "Jan 15, 2024 at 2:30 PM")
        // - Consistent placeholder for empty values
        // - Formatted phone numbers
        // - Logical column grouping: ID → Customer Info → Property → Status → Dates
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.UTF8_BOM);
        sb.append("Lead ID,Customer Name,Email,Phone,Property Address,Lead Source,Status,Created,Last Updated,Converted to Job\n");

        for (Lead lead : leads) {
            String customerName = emptyPlaceholder(customerNameFromLead(lead));
            String customerEmail = emptyPlaceholder(lead.getCustomer() != null ? lead.getCustomer().getEmail() : null);
            String customerPhone = formatPhone(lead.getCustomer() != null ? lead.getCustomer().getPrimaryPhone() : null);

            String propertyAddress = emptyPlaceholder(formatAddress(lead.getPropertyAddress()));

            String convertedJobId = "—";
            if (lead.getId() != null) {
                convertedJobId = jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, lead.getId())
                        .map(j -> CsvUtils.cell(j.getId()))
                        .orElse("—");
            }

            sb.append(CsvUtils.cell(lead.getId())).append(",")
                    .append(CsvUtils.cell(customerName)).append(",")
                    .append(CsvUtils.cell(customerEmail)).append(",")
                    .append(CsvUtils.cell(customerPhone)).append(",")
                    .append(CsvUtils.cell(propertyAddress)).append(",")
                    .append(CsvUtils.cell(humanizeEnum(lead.getSource()))).append(",")
                    .append(CsvUtils.cell(humanizeEnum(lead.getStatus()))).append(",")
                    .append(CsvUtils.cell(toFriendlyTimestamp(lead.getCreatedAt()))).append(",")
                    .append(CsvUtils.cell(toFriendlyTimestamp(lead.getUpdatedAt()))).append(",")
                    .append(convertedJobId)
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportJobsCsv(UUID userId, UUID tenantId, JobStatus status, int limit) {
        int capped = Math.min(Math.max(limit, 1), 5000);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(Objects.requireNonNull(tenantId), Objects.requireNonNull(userId));

        Pageable pageable = PageRequest.of(0, capped, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Job> jobs;
        if (status != null) {
            jobs = jobRepository.findByTenantAndStatusAndArchivedFalse(tenant, status, pageable).getContent();
        } else {
            jobs = jobRepository.findByTenantAndArchivedFalse(tenant, pageable).getContent();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.UTF8_BOM);
        sb.append("Job ID,Customer Name,Email,Phone,Property Address,Job Type,Status,Scheduled Start,Scheduled End,Actual Start,Actual End,Assigned Crew,Roof Type,Created,Last Updated\n");

        for (Job job : jobs) {
            String customerName = emptyPlaceholder(customerNameFromJob(job));
            String customerEmail = emptyPlaceholder(job.getCustomer() != null ? job.getCustomer().getEmail() : null);
            String customerPhone = formatPhone(job.getCustomer() != null ? job.getCustomer().getPrimaryPhone() : null);
            String propertyAddress = emptyPlaceholder(formatAddress(job.getPropertyAddress()));

            String schedStart = toFriendlyDate(job.getScheduledStartDate());
            String schedEnd = toFriendlyDate(job.getScheduledEndDate());
            String actualStart = toFriendlyDate(job.getActualStartDate());
            String actualEnd = toFriendlyDate(job.getActualEndDate());

            sb.append(CsvUtils.cell(job.getId())).append(",")
                    .append(CsvUtils.cell(customerName)).append(",")
                    .append(CsvUtils.cell(customerEmail)).append(",")
                    .append(CsvUtils.cell(customerPhone)).append(",")
                    .append(CsvUtils.cell(propertyAddress)).append(",")
                    .append(CsvUtils.cell(humanizeEnum(job.getJobType()))).append(",")
                    .append(CsvUtils.cell(humanizeEnum(job.getStatus()))).append(",")
                    .append(CsvUtils.cell(schedStart)).append(",")
                    .append(CsvUtils.cell(schedEnd)).append(",")
                    .append(CsvUtils.cell(actualStart)).append(",")
                    .append(CsvUtils.cell(actualEnd)).append(",")
                    .append(CsvUtils.cell(emptyPlaceholder(job.getAssignedCrew()))).append(",")
                    .append(CsvUtils.cell(emptyPlaceholder(job.getRoofType()))).append(",")
                    .append(CsvUtils.cell(toFriendlyTimestamp(job.getCreatedAt()))).append(",")
                    .append(CsvUtils.cell(toFriendlyTimestamp(job.getUpdatedAt())))
                    .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String customerNameFromLead(Lead lead) {
        if (lead.getCustomer() == null) return "";
        String first = safe(lead.getCustomer().getFirstName());
        String last = safe(lead.getCustomer().getLastName());
        return (first + " " + last).trim();
    }

    private String customerNameFromJob(Job job) {
        if (job.getCustomer() == null) return "";
        String first = safe(job.getCustomer().getFirstName());
        String last = safe(job.getCustomer().getLastName());
        return (first + " " + last).trim();
    }

    private String formatAddress(Address addr) {
        if (addr == null) return "";
        // One compact, readable line (Excel-friendly). Examples:
        // "123 Main St, Apt 4, Denver, CO 80202, US"
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

    private void appendIfPresent(StringBuilder out, String value) {
        if (value == null || value.isBlank()) return;
        if (!out.isEmpty()) out.append(", ");
        out.append(value.trim());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String humanizeEnum(Object enumValue) {
        if (enumValue == null) return "";
        // Handles enums and other objects whose toString is a constant-like value.
        String raw = enumValue.toString();
        if (raw.isBlank()) return "";
        // Convert e.g. "IN_PROGRESS" -> "In Progress", "ROOF_REPLACEMENT" -> "Roof Replacement".
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return out.toString();
    }

    private String toFriendlyTimestamp(Object temporal) {
        if (temporal == null) return "—";
        // Format: "Jan 15, 2024 at 2:30 PM" - human-friendly and still sortable by date
        DateTimeFormatter friendlyFmt = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");
        
        if (temporal instanceof java.time.Instant i) {
            return friendlyFmt.format(i.atOffset(ZoneOffset.UTC));
        }
        if (temporal instanceof java.time.OffsetDateTime odt) {
            return friendlyFmt.format(odt.withOffsetSameInstant(ZoneOffset.UTC));
        }
        if (temporal instanceof java.time.ZonedDateTime zdt) {
            return friendlyFmt.format(zdt.withZoneSameInstant(ZoneOffset.UTC));
        }
        if (temporal instanceof java.time.LocalDateTime ldt) {
            return friendlyFmt.format(ldt.atOffset(ZoneOffset.UTC));
        }
        // Fallback
        String raw = temporal.toString();
        return raw.isBlank() ? "—" : raw;
    }

    private String toFriendlyDate(LocalDate date) {
        if (date == null) return "—";
        // Format: "Jan 15, 2024" - clean and readable
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private String emptyPlaceholder(String value) {
        // Use em-dash for missing values - cleaner than blank cells
        return (value == null || value.isBlank()) ? "—" : value;
    }

    private String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) return "—";
        // Clean up phone number for consistent display
        String digits = phone.replaceAll("[^0-9+]", "");
        if (digits.length() == 10) {
            // Format as (xxx) xxx-xxxx for US numbers
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6));
        } else if (digits.length() == 11 && digits.startsWith("1")) {
            // Format as +1 (xxx) xxx-xxxx for US numbers with country code
            return String.format("+1 (%s) %s-%s",
                    digits.substring(1, 4),
                    digits.substring(4, 7),
                    digits.substring(7));
        }
        // Return original if not a standard format
        return phone;
    }
}