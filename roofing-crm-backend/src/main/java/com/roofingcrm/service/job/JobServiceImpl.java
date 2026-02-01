package com.roofingcrm.service.job;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.common.PickerItemDto;
import com.roofingcrm.api.v1.job.CreateJobRequest;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.job.UpdateJobRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private final TenantAccessService tenantAccessService;
    private final JobRepository jobRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final ActivityEventService activityEventService;

    @Autowired
    public JobServiceImpl(TenantAccessService tenantAccessService,
                          JobRepository jobRepository,
                          CustomerRepository customerRepository,
                          LeadRepository leadRepository,
                          ActivityEventService activityEventService) {
        this.tenantAccessService = tenantAccessService;
        this.jobRepository = jobRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.activityEventService = activityEventService;
    }

    @Override
    public JobDto createJob(@NonNull UUID tenantId, @NonNull UUID userId, CreateJobRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Customer customer;
        Lead lead = null;

        if (request.getLeadId() != null) {
            // Create job from a lead
            lead = leadRepository.findByIdAndTenantAndArchivedFalse(request.getLeadId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
            customer = lead.getCustomer();
            if (customer == null) {
                throw new IllegalArgumentException("Lead has no associated customer");
            }
        } else if (request.getCustomerId() != null) {
            // Create job directly from customer
            customer = customerRepository.findByIdAndTenantAndArchivedFalse(request.getCustomerId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        } else {
            throw new IllegalArgumentException("Either leadId or customerId must be provided");
        }

        Job job = new Job();
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setLead(lead);
        job.setCreatedByUserId(userId);
        job.setUpdatedByUserId(userId);

        job.setJobType(request.getType());
        job.setStatus(request.getScheduledStartDate() != null ? JobStatus.SCHEDULED : JobStatus.UNSCHEDULED);

        if (request.getPropertyAddress() != null) {
            Address address = new Address();
            applyAddress(address, request.getPropertyAddress());
            job.setPropertyAddress(address);
        }

        job.setScheduledStartDate(request.getScheduledStartDate());
        job.setScheduledEndDate(request.getScheduledEndDate());
        job.setJobNotes(request.getInternalNotes());
        job.setAssignedCrew(request.getCrewName());

        Job saved = jobRepository.save(job);
        return toDto(saved);
    }

    @Override
    public JobDto updateJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UpdateJobRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        LocalDate prevStart = job.getScheduledStartDate();
        LocalDate prevEnd = job.getScheduledEndDate();
        String prevCrew = job.getAssignedCrew();

        job.setUpdatedByUserId(userId);

        if (request.getType() != null) {
            job.setJobType(request.getType());
        }

        if (request.getPropertyAddress() != null) {
            Address address = job.getPropertyAddress();
            if (address == null) {
                address = new Address();
            }
            applyAddress(address, request.getPropertyAddress());
            job.setPropertyAddress(address);
        }

        if (request.getClearSchedule() != null && request.getClearSchedule()) {
            job.setScheduledStartDate(null);
            job.setScheduledEndDate(null);
        } else {
            if (request.getScheduledStartDate() != null) {
                job.setScheduledStartDate(request.getScheduledStartDate());
            }
            if (request.getScheduledEndDate() != null) {
                job.setScheduledEndDate(request.getScheduledEndDate());
            }
        }

        LocalDate newStart = job.getScheduledStartDate();
        LocalDate newEnd = job.getScheduledEndDate();
        if (newStart != null && newEnd != null && newStart.isAfter(newEnd)) {
            throw new IllegalArgumentException("scheduledStartDate must be before or equal to scheduledEndDate");
        }

        if (request.getInternalNotes() != null) {
            job.setJobNotes(request.getInternalNotes());
        }

        if (request.getCrewName() != null) {
            job.setAssignedCrew(request.getCrewName());
        }

        // Sync status with schedule: UNSCHEDULED <-> SCHEDULED based on dates
        JobStatus currentStatus = job.getStatus();
        if (currentStatus == JobStatus.SCHEDULED && newStart == null) {
            job.setStatus(JobStatus.UNSCHEDULED);
        } else if (currentStatus == JobStatus.UNSCHEDULED && newStart != null) {
            job.setStatus(JobStatus.SCHEDULED);
        }

        Job saved = jobRepository.save(job);

        boolean scheduleChanged = !Objects.equals(prevStart, saved.getScheduledStartDate())
                || !Objects.equals(prevEnd, saved.getScheduledEndDate())
                || !Objects.equals(prevCrew, saved.getAssignedCrew());

        if (scheduleChanged) {
            String message = buildScheduleChangeMessage(prevStart, prevEnd, prevCrew,
                    saved.getScheduledStartDate(), saved.getScheduledEndDate(), saved.getAssignedCrew());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fromStart", prevStart);
            metadata.put("fromEnd", prevEnd);
            metadata.put("fromCrew", prevCrew);
            metadata.put("toStart", saved.getScheduledStartDate());
            metadata.put("toEnd", saved.getScheduledEndDate());
            metadata.put("toCrew", saved.getAssignedCrew());
            activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                    Objects.requireNonNull(saved.getId()),
                    ActivityEventType.JOB_SCHEDULE_CHANGED, message, metadata);
        }

        return toDto(saved);
    }

    private static String buildScheduleChangeMessage(LocalDate prevStart, LocalDate prevEnd, String prevCrew,
                                                    LocalDate newStart, LocalDate newEnd, String newCrew) {
        boolean hadSchedule = prevStart != null;
        boolean hasSchedule = newStart != null;
        boolean crewChanged = !Objects.equals(prevCrew, newCrew);

        if (!hadSchedule && hasSchedule) {
            String range = formatRange(newStart, newEnd);
            return "Schedule set to " + range + (newCrew != null && !newCrew.isBlank() ? " (Crew: " + newCrew + ")" : "");
        }
        if (hadSchedule && !hasSchedule) {
            String oldRange = formatRange(prevStart, prevEnd);
            return "Schedule cleared (was " + oldRange + ")";
        }
        if (crewChanged && !hasSchedule) {
            return "Crew changed: " + orBlank(prevCrew) + " → " + orBlank(newCrew);
        }
        if (crewChanged && hasSchedule) {
            if (Objects.equals(prevStart, newStart) && Objects.equals(prevEnd, newEnd)) {
                return "Crew changed: " + orBlank(prevCrew) + " → " + orBlank(newCrew);
            }
            String range = formatRange(newStart, newEnd);
            return "Schedule updated: " + range + "; Crew: " + orBlank(prevCrew) + " → " + orBlank(newCrew);
        }
        String range = formatRange(newStart, newEnd);
        return "Schedule updated to " + range + (newCrew != null && !newCrew.isBlank() ? " (Crew: " + newCrew + ")" : "");
    }

    private static String formatRange(LocalDate start, LocalDate end) {
        if (start == null) return "";
        if (end == null || start.equals(end)) return start.toString();
        return start + " → " + end;
    }

    private static String orBlank(String s) {
        return (s != null && !s.isBlank()) ? s : "(none)";
    }

    @Override
    @Transactional(readOnly = true)
    public JobDto getJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return toDto(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> listJobs(@NonNull UUID tenantId, @NonNull UUID userId, JobStatus statusFilter, UUID customerIdFilter, @NonNull Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Page<Job> page;
        if (statusFilter != null && customerIdFilter != null) {
            page = jobRepository.findByTenantAndStatusAndCustomerIdAndArchivedFalse(tenant, statusFilter, customerIdFilter, pageable);
        } else if (statusFilter != null) {
            page = jobRepository.findByTenantAndStatusAndArchivedFalse(tenant, statusFilter, pageable);
        } else if (customerIdFilter != null) {
            page = jobRepository.findByTenantAndCustomerIdAndArchivedFalse(tenant, customerIdFilter, pageable);
        } else {
            page = jobRepository.findByTenantAndArchivedFalse(tenant, pageable);
        }

        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDto> listScheduleJobs(@NonNull UUID tenantId, @NonNull UUID userId,
                                         @NonNull LocalDate startDate, @NonNull LocalDate endDate,
                                         JobStatus status, String crewName, boolean includeUnscheduled,
                                         @NonNull Pageable pageable) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Page<Job> page = jobRepository.searchSchedule(
                tenant, status, crewName != null ? crewName : "", startDate, endDate, includeUnscheduled, pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickerItemDto> searchJobsForPicker(@NonNull UUID tenantId, @NonNull UUID userId, String q, int limit) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        int capped = Math.min(Math.max(limit, 1), 50);
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();
        List<Job> jobs = jobRepository.searchForPicker(tenant, qNorm, PageRequest.of(0, capped));
        return jobs.stream().map(this::jobToPickerItem).toList();
    }

    private PickerItemDto jobToPickerItem(Job j) {
        String jobTypeStr = j.getJobType() != null ? j.getJobType().name() : "";
        String addr = j.getPropertyAddress() != null && j.getPropertyAddress().getLine1() != null
                ? j.getPropertyAddress().getLine1()
                : "";
        String customerName = j.getCustomer() != null
                ? (j.getCustomer().getFirstName() + " " + j.getCustomer().getLastName()).trim()
                : "";
        String label = !addr.isEmpty() ? jobTypeStr + " – " + addr : customerName;
        if (label.isEmpty()) label = "—";
        String subLabel = !addr.isEmpty() ? customerName : jobTypeStr;
        return new PickerItemDto(j.getId(), label, subLabel);
    }

    @Override
    public JobDto updateJobStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, JobStatus newStatus) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        JobStatus prevStatus = job.getStatus();
        job.setStatus(newStatus);
        job.setUpdatedByUserId(userId);

        Job updated = jobRepository.save(job);

        if (prevStatus != newStatus) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("jobId", jobId);
            meta.put("fromStatus", prevStatus.name());
            meta.put("toStatus", newStatus.name());
            activityEventService.recordEvent(tenant, userId, ActivityEntityType.JOB,
                    Objects.requireNonNull(jobId),
                    ActivityEventType.JOB_STATUS_CHANGED, "Status: " + prevStatus + " → " + newStatus, meta);
        }

        return toDto(updated);
    }

    private void applyAddress(Address entity, AddressDto dto) {
        entity.setLine1(dto.getLine1());
        entity.setLine2(dto.getLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZip(dto.getZip());
        entity.setCountryCode(dto.getCountryCode());
    }

    private JobDto toDto(Job entity) {
        JobDto dto = new JobDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setType(entity.getJobType());
        dto.setScheduledStartDate(entity.getScheduledStartDate());
        dto.setScheduledEndDate(entity.getScheduledEndDate());
        dto.setInternalNotes(entity.getJobNotes());
        dto.setCrewName(entity.getAssignedCrew());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getCustomer() != null) {
            Customer customer = entity.getCustomer();
            dto.setCustomerId(customer.getId());
            dto.setCustomerFirstName(customer.getFirstName());
            dto.setCustomerLastName(customer.getLastName());
            dto.setCustomerEmail(customer.getEmail());
            dto.setCustomerPhone(customer.getPrimaryPhone());
        }

        if (entity.getLead() != null) {
            dto.setLeadId(entity.getLead().getId());
        }

        if (entity.getPropertyAddress() != null) {
            AddressDto a = new AddressDto();
            a.setLine1(entity.getPropertyAddress().getLine1());
            a.setLine2(entity.getPropertyAddress().getLine2());
            a.setCity(entity.getPropertyAddress().getCity());
            a.setState(entity.getPropertyAddress().getState());
            a.setZip(entity.getPropertyAddress().getZip());
            a.setCountryCode(entity.getPropertyAddress().getCountryCode());
            dto.setPropertyAddress(a);
        }

        return dto;
    }
}
