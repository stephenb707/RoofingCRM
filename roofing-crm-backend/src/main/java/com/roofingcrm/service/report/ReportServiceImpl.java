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

        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.UTF8_BOM);
        sb.append("leadId,customerName,customerEmail,customerPhone,propertyAddress1,propertyAddress2,propertyCity,propertyState,propertyZip,propertyCountryCode,source,status,createdAt,updatedAt,convertedJobId\n");

        for (Lead lead : leads) {
            String customerName = formatCustomerName(lead);
            String customerEmail = lead.getCustomer() != null ? CsvUtils.cell(lead.getCustomer().getEmail()) : "";
            String customerPhone = lead.getCustomer() != null ? CsvUtils.cell(lead.getCustomer().getPrimaryPhone()) : "";
            Address addr = lead.getPropertyAddress();
            String prop1 = addr != null ? CsvUtils.cell(addr.getLine1()) : "";
            String prop2 = addr != null ? CsvUtils.cell(addr.getLine2()) : "";
            String city = addr != null ? CsvUtils.cell(addr.getCity()) : "";
            String state = addr != null ? CsvUtils.cell(addr.getState()) : "";
            String zip = addr != null ? CsvUtils.cell(addr.getZip()) : "";
            String country = addr != null ? CsvUtils.cell(addr.getCountryCode()) : "";
            String convertedJobId = "";
            if (lead.getId() != null) {
                convertedJobId = jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, lead.getId())
                        .map(j -> CsvUtils.cell(j.getId()))
                        .orElse("");
            }

            sb.append(CsvUtils.cell(lead.getId())).append(",")
                    .append(CsvUtils.cell(customerName)).append(",")
                    .append(customerEmail).append(",")
                    .append(customerPhone).append(",")
                    .append(prop1).append(",")
                    .append(prop2).append(",")
                    .append(city).append(",")
                    .append(state).append(",")
                    .append(zip).append(",")
                    .append(country).append(",")
                    .append(CsvUtils.cell(lead.getSource())).append(",")
                    .append(CsvUtils.cell(lead.getStatus())).append(",")
                    .append(CsvUtils.cell(lead.getCreatedAt())).append(",")
                    .append(CsvUtils.cell(lead.getUpdatedAt())).append(",")
                    .append(convertedJobId).append("\n");
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
        sb.append("jobId,customerName,customerEmail,customerPhone,propertyAddress1,propertyAddress2,propertyCity,propertyState,propertyZip,propertyCountryCode,jobType,status,scheduledStartDate,scheduledEndDate,actualStartDate,actualEndDate,assignedCrew,roofType,createdAt,updatedAt\n");

        DateTimeFormatter dateFmt = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Job job : jobs) {
            String customerName = formatCustomerName(job);
            String customerEmail = job.getCustomer() != null ? CsvUtils.cell(job.getCustomer().getEmail()) : "";
            String customerPhone = job.getCustomer() != null ? CsvUtils.cell(job.getCustomer().getPrimaryPhone()) : "";
            Address addr = job.getPropertyAddress();
            String prop1 = addr != null ? CsvUtils.cell(addr.getLine1()) : "";
            String prop2 = addr != null ? CsvUtils.cell(addr.getLine2()) : "";
            String city = addr != null ? CsvUtils.cell(addr.getCity()) : "";
            String state = addr != null ? CsvUtils.cell(addr.getState()) : "";
            String zip = addr != null ? CsvUtils.cell(addr.getZip()) : "";
            String country = addr != null ? CsvUtils.cell(addr.getCountryCode()) : "";
            String schedStart = formatDate(job.getScheduledStartDate(), dateFmt);
            String schedEnd = formatDate(job.getScheduledEndDate(), dateFmt);
            String actualStart = formatDate(job.getActualStartDate(), dateFmt);
            String actualEnd = formatDate(job.getActualEndDate(), dateFmt);

            sb.append(CsvUtils.cell(job.getId())).append(",")
                    .append(CsvUtils.cell(customerName)).append(",")
                    .append(customerEmail).append(",")
                    .append(customerPhone).append(",")
                    .append(prop1).append(",")
                    .append(prop2).append(",")
                    .append(city).append(",")
                    .append(state).append(",")
                    .append(zip).append(",")
                    .append(country).append(",")
                    .append(CsvUtils.cell(job.getJobType())).append(",")
                    .append(CsvUtils.cell(job.getStatus())).append(",")
                    .append(schedStart).append(",")
                    .append(schedEnd).append(",")
                    .append(actualStart).append(",")
                    .append(actualEnd).append(",")
                    .append(CsvUtils.cell(job.getAssignedCrew())).append(",")
                    .append(CsvUtils.cell(job.getRoofType())).append(",")
                    .append(CsvUtils.cell(job.getCreatedAt())).append(",")
                    .append(CsvUtils.cell(job.getUpdatedAt())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatCustomerName(Lead lead) {
        if (lead.getCustomer() == null) return "";
        String first = lead.getCustomer().getFirstName() != null ? lead.getCustomer().getFirstName() : "";
        String last = lead.getCustomer().getLastName() != null ? lead.getCustomer().getLastName() : "";
        return CsvUtils.cell((first + " " + last).trim());
    }

    private String formatCustomerName(Job job) {
        if (job.getCustomer() == null) return "";
        String first = job.getCustomer().getFirstName() != null ? job.getCustomer().getFirstName() : "";
        String last = job.getCustomer().getLastName() != null ? job.getCustomer().getLastName() : "";
        return CsvUtils.cell((first + " " + last).trim());
    }

    private String formatDate(LocalDate date, DateTimeFormatter fmt) {
        return date != null ? CsvUtils.cell(date.format(fmt)) : "";
    }
}
