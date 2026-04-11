package com.roofingcrm.service.dashboard;

import com.roofingcrm.api.v1.dashboard.DashboardJobSnippetDto;
import com.roofingcrm.api.v1.dashboard.DashboardLeadSnippetDto;
import com.roofingcrm.api.v1.dashboard.DashboardSummaryDto;
import com.roofingcrm.api.v1.dashboard.DashboardTaskSnippetDto;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Task;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.EstimateStatus;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.repository.TaskRepository;
import com.roofingcrm.domain.repository.spec.TaskSpecifications;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final TenantAccessService tenantAccessService;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final JobRepository jobRepository;
    private final EstimateRepository estimateRepository;
    private final InvoiceRepository invoiceRepository;
    private final TaskRepository taskRepository;

    @Autowired
    public DashboardServiceImpl(
            TenantAccessService tenantAccessService,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            JobRepository jobRepository,
            EstimateRepository estimateRepository,
            InvoiceRepository invoiceRepository,
            TaskRepository taskRepository) {
        this.tenantAccessService = tenantAccessService;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.jobRepository = jobRepository;
        this.estimateRepository = estimateRepository;
        this.invoiceRepository = invoiceRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public DashboardSummaryDto getSummary(@NonNull UUID tenantId, @NonNull UUID userId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setCustomerCount(customerRepository.countByTenantAndArchivedFalse(tenant));
        dto.setLeadCount(leadRepository.countByTenantAndArchivedFalse(tenant));
        dto.setJobCount(jobRepository.countByTenantAndArchivedFalse(tenant));
        dto.setEstimateCount(estimateRepository.countByTenantAndArchivedFalse(tenant));
        dto.setInvoiceCount(invoiceRepository.countByTenantAndArchivedFalse(tenant));
        dto.setOpenTaskCount(taskRepository.countOpenByTenant(tenant));
        dto.setEstimatesSentCount(estimateRepository.countByTenantAndStatusAndArchivedFalse(tenant, EstimateStatus.SENT));
        dto.setUnpaidInvoiceCount(invoiceRepository.countUnpaidByTenant(tenant));
        dto.setActivePipelineLeadCount(leadRepository.countActivePipelineByTenant(tenant));
        dto.setUnscheduledJobsCount(jobRepository.countWithNoScheduledStartByTenant(tenant));

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        dto.setJobsScheduledThisWeek(
                jobRepository.countScheduledInDateRange(tenant, weekStart, weekEnd));

        LinkedHashMap<String, Long> byStatus = new LinkedHashMap<>();
        for (LeadStatus st : LeadStatus.values()) {
            byStatus.put(st.name(), leadRepository.countByTenantAndStatusAndArchivedFalse(tenant, st));
        }
        dto.setLeadCountByStatus(byStatus);

        dto.setRecentLeads(
                leadRepository
                        .findByTenantAndArchivedFalse(
                                tenant,
                                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "updatedAt")))
                        .map(this::toLeadSnippet)
                        .getContent());

        LocalDate horizonEnd = today.plusDays(14);
        dto.setUpcomingJobs(
                jobRepository
                        .searchSchedule(tenant, null, "", today, horizonEnd, false,
                                PageRequest.of(0, 5,
                                        Sort.by(
                                                Sort.Order.asc("scheduledStartDate").nullsLast(),
                                                Sort.Order.desc("createdAt"))))
                        .map(this::toJobSnippet)
                        .getContent());

        dto.setOpenTasks(
                taskRepository
                        .findAll(
                                TaskSpecifications.openTasksForTenant(Objects.requireNonNull(tenant)),
                                PageRequest.of(0, 5,
                                        Sort.by(
                                                Sort.Order.asc("dueAt").nullsLast(),
                                                Sort.Order.asc("createdAt"))))
                        .map(this::toTaskSnippet)
                        .getContent());

        return dto;
    }

    private DashboardLeadSnippetDto toLeadSnippet(Lead l) {
        DashboardLeadSnippetDto d = new DashboardLeadSnippetDto();
        d.setId(l.getId());
        d.setStatus(l.getStatus());
        d.setCustomerLabel(formatCustomerName(l.getCustomer()));
        if (l.getPropertyAddress() != null && l.getPropertyAddress().getLine1() != null) {
            d.setPropertyLine1(l.getPropertyAddress().getLine1());
        }
        d.setUpdatedAt(l.getUpdatedAt());
        return d;
    }

    private DashboardJobSnippetDto toJobSnippet(Job j) {
        DashboardJobSnippetDto d = new DashboardJobSnippetDto();
        d.setId(j.getId());
        d.setStatus(j.getStatus());
        d.setScheduledStartDate(j.getScheduledStartDate());
        if (j.getPropertyAddress() != null && j.getPropertyAddress().getLine1() != null) {
            d.setPropertyLine1(j.getPropertyAddress().getLine1());
        }
        d.setCustomerLabel(formatCustomerName(j.getCustomer()));
        return d;
    }

    private DashboardTaskSnippetDto toTaskSnippet(Task t) {
        DashboardTaskSnippetDto d = new DashboardTaskSnippetDto();
        d.setTaskId(t.getId());
        d.setTitle(t.getTitle());
        d.setStatus(t.getStatus());
        d.setDueAt(t.getDueAt());
        if (t.getLead() != null) {
            d.setLeadId(t.getLead().getId());
        }
        if (t.getJob() != null) {
            d.setJobId(t.getJob().getId());
        }
        if (t.getCustomer() != null) {
            d.setCustomerId(t.getCustomer().getId());
        }
        return d;
    }

    private static String formatCustomerName(Customer c) {
        if (c == null) {
            return "—";
        }
        String name = (c.getFirstName() + " " + c.getLastName()).trim();
        return name.isEmpty() ? "—" : name;
    }
}
