package com.roofingcrm.service.lead;

import com.roofingcrm.api.v1.common.AddressDto;
import com.roofingcrm.api.v1.common.PickerItemDto;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.api.v1.lead.ConvertLeadToJobRequest;
import com.roofingcrm.api.v1.lead.CreateLeadRequest;
import com.roofingcrm.api.v1.lead.LeadDto;
import com.roofingcrm.api.v1.lead.NewLeadCustomerRequest;
import com.roofingcrm.api.v1.lead.UpdateLeadRequest;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Lead;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.domain.enums.ActivityEventType;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.PipelineType;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.domain.repository.LeadRepository;
import com.roofingcrm.domain.repository.PipelineStatusDefinitionRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.audit.AuditSupport;
import com.roofingcrm.service.exception.LeadConversionNotAllowedException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class LeadServiceImpl implements LeadService {

    private final TenantAccessService tenantAccessService;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;
    private final ActivityEventService activityEventService;
    private final PipelineStatusDefinitionRepository definitionRepository;

    @Autowired
    public LeadServiceImpl(TenantAccessService tenantAccessService,
                           LeadRepository leadRepository,
                           CustomerRepository customerRepository,
                           JobRepository jobRepository,
                           ActivityEventService activityEventService,
                           PipelineStatusDefinitionRepository definitionRepository) {
        this.tenantAccessService = tenantAccessService;
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.jobRepository = jobRepository;
        this.activityEventService = activityEventService;
        this.definitionRepository = definitionRepository;
    }

    @Override
    public LeadDto createLead(@NonNull UUID tenantId, @NonNull UUID userId, CreateLeadRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to create leads.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Customer customer = resolveCustomerForLead(tenant, userId, request);

        Lead lead = new Lead();
        lead.setTenant(tenant);
        lead.setCustomer(customer);
        AuditSupport.touchForCreate(lead, userId);

        PipelineStatusDefinition newStatus = requireLeadDefByKey(tenant, "NEW");
        lead.setStatusDefinition(newStatus);
        lead.setSource(request.getSource() != null ? request.getSource() : LeadSource.OTHER);
        lead.setLeadNotes(request.getLeadNotes());

        int maxPos = leadRepository.findMaxPipelinePositionByTenantAndStatusDefinitionAndArchivedFalse(tenant, newStatus);
        lead.setPipelinePosition(maxPos + 1);

        Address propertyAddress = new Address();
        applyAddress(propertyAddress, request.getPropertyAddress());
        lead.setPropertyAddress(propertyAddress);

        Lead saved = Objects.requireNonNull(leadRepository.save(lead));
        return toDto(saved);
    }

    @Override
    public LeadDto updateLead(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, UpdateLeadRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to edit leads.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        AuditSupport.touchForUpdate(lead, userId);

        if (request.getSource() != null) {
            lead.setSource(request.getSource());
        }

        if (request.getLeadNotes() != null) {
            lead.setLeadNotes(request.getLeadNotes());
        }

        if (request.getPropertyAddress() != null) {
            if (lead.getPropertyAddress() == null) {
                lead.setPropertyAddress(new Address());
            }
            applyAddress(lead.getPropertyAddress(), request.getPropertyAddress());
        }

        Lead saved = Objects.requireNonNull(leadRepository.save(Objects.requireNonNull(lead)));
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadDto getLead(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        UUID convertedJobId = jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, leadId)
                .map(Job::getId)
                .orElse(null);
        return toDto(lead, convertedJobId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeadDto> listLeads(
            @NonNull UUID tenantId,
            @NonNull UUID userId,
            UUID statusDefinitionIdFilter,
            UUID customerId,
            @NonNull Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Page<Lead> page;
        if (statusDefinitionIdFilter != null && customerId != null) {
            PipelineStatusDefinition def = requireLeadDef(tenant, statusDefinitionIdFilter);
            page = leadRepository.findByTenantAndStatusDefinitionAndCustomerIdAndArchivedFalse(tenant, def, customerId, pageable);
        } else if (statusDefinitionIdFilter != null) {
            PipelineStatusDefinition def = requireLeadDef(tenant, statusDefinitionIdFilter);
            page = leadRepository.findByTenantAndStatusDefinitionAndArchivedFalse(tenant, def, pageable);
        } else if (customerId != null) {
            page = leadRepository.findByTenantAndCustomerIdAndArchivedFalse(tenant, customerId, pageable);
        } else {
            page = leadRepository.findByTenantAndArchivedFalse(tenant, pageable);
        }

        return page.map(l -> toDto(l, null));
    }

    @Override
    public LeadDto updateLeadStatus(
            @NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, UUID newStatusDefinitionId, Integer position) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to edit the lead pipeline.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        PipelineStatusDefinition newDef = requireLeadDef(tenant, newStatusDefinitionId);
        if (!newDef.isActive()) {
            throw new IllegalArgumentException("Target status is not active.");
        }
        PipelineStatusDefinition oldDef = lead.getStatusDefinition();
        boolean statusChanged = !oldDef.getId().equals(newDef.getId());

        if (!statusChanged && position == null) {
            return toDto(lead);
        }

        List<Lead> oldColumn = leadRepository
                .findByTenantAndStatusDefinitionAndArchivedFalseOrderByPipelinePositionAscCreatedAtAsc(tenant, oldDef);
        List<Lead> oldList = new ArrayList<>(oldColumn);
        oldList.removeIf(l -> Objects.equals(l.getId(), leadId));

        for (int i = 0; i < oldList.size(); i++) {
            oldList.get(i).setPipelinePosition(i);
        }

        if (statusChanged) {
            lead.setStatusDefinition(newDef);
            AuditSupport.touchForUpdate(lead, userId);
            List<Lead> newColumn = leadRepository
                    .findByTenantAndStatusDefinitionAndArchivedFalseOrderByPipelinePositionAscCreatedAtAsc(tenant, newDef);
            List<Lead> newList = new ArrayList<>(newColumn);
            int insertIdx = (position != null && position >= 0 && position <= newList.size()) ? position : newList.size();
            newList.add(insertIdx, lead);
            for (int i = 0; i < newList.size(); i++) {
                newList.get(i).setPipelinePosition(i);
            }
            leadRepository.saveAll(oldList);
            leadRepository.saveAll(newList);

            Map<String, Object> meta = new HashMap<>();
            meta.put("leadId", leadId);
            meta.put("fromStatus", oldDef.getSystemKey());
            meta.put("toStatus", newDef.getSystemKey());
            activityEventService.recordEvent(tenant, userId, ActivityEntityType.LEAD,
                    Objects.requireNonNull(lead.getId()),
                    ActivityEventType.LEAD_STATUS_CHANGED,
                    "Lead status changed from " + oldDef.getLabel() + " to " + newDef.getLabel(), meta);
        } else {
            AuditSupport.touchForUpdate(lead, userId);
            List<Lead> sameList = new ArrayList<>(oldColumn);
            sameList.removeIf(l -> Objects.equals(l.getId(), leadId));
            int insertIdx = (position != null && position >= 0 && position <= sameList.size()) ? position : sameList.size();
            sameList.add(insertIdx, lead);
            for (int i = 0; i < sameList.size(); i++) {
                sameList.get(i).setPipelinePosition(i);
            }
            leadRepository.saveAll(sameList);
        }

        return toDto(lead);
    }

    @Override
    public JobDto convertLeadToJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID leadId, ConvertLeadToJobRequest request) {
        tenantAccessService.requireAnyRole(tenantId, userId, Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES)),
                "You do not have permission to convert leads.");
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));

        if ("LOST".equals(lead.getStatusDefinition().getSystemKey())) {
            throw new LeadConversionNotAllowedException("Cannot convert a lost lead to a job");
        }

        Optional<Job> existingJob = jobRepository.findByTenantAndLeadIdAndArchivedFalse(tenant, leadId);
        if (existingJob.isPresent()) {
            return toJobDto(existingJob.get());
        }

        Customer customer = lead.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Lead has no associated customer");
        }

        Job job = new Job();
        job.setTenant(tenant);
        job.setCustomer(customer);
        job.setLead(lead);
        AuditSupport.touchForCreate(job, userId);
        job.setJobType(request.getType());
        job.setScheduledStartDate(request.getScheduledStartDate());
        job.setScheduledEndDate(request.getScheduledEndDate());
        PipelineStatusDefinition jobStatus = request.getScheduledStartDate() != null
                ? requireJobDefByKey(tenant, "SCHEDULED")
                : requireJobDefByKey(tenant, "UNSCHEDULED");
        job.setStatusDefinition(jobStatus);
        job.setAssignedCrew(request.getCrewName());
        job.setJobNotes(request.getInternalNotes());

        if (lead.getPropertyAddress() != null) {
            Address propertyAddress = new Address();
            copyAddress(lead.getPropertyAddress(), propertyAddress);
            job.setPropertyAddress(propertyAddress);
        }

        Job saved = jobRepository.save(job);

        PipelineStatusDefinition won = requireLeadDefByKey(tenant, "WON");
        int maxPos = leadRepository.findMaxPipelinePositionByTenantAndStatusDefinitionAndArchivedFalse(tenant, won);
        lead.setStatusDefinition(won);
        lead.setPipelinePosition(maxPos + 1);
        AuditSupport.touchForUpdate(lead, userId);
        leadRepository.save(lead);

        Map<String, Object> meta = new HashMap<>();
        meta.put("leadId", leadId);
        meta.put("jobId", saved.getId());
        activityEventService.recordEvent(tenant, userId, ActivityEntityType.LEAD,
                Objects.requireNonNull(leadId),
                ActivityEventType.LEAD_CONVERTED_TO_JOB, "Lead converted to job", meta);

        return toJobDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickerItemDto> searchLeadsForPicker(@NonNull UUID tenantId, @NonNull UUID userId, String q, int limit) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        int capped = Math.min(Math.max(limit, 1), 50);
        String qNorm = (q == null || q.isBlank()) ? null : q.trim();
        List<Lead> leads = leadRepository.searchForPicker(tenant, qNorm, PageRequest.of(0, capped));
        return leads.stream().map(this::leadToPickerItem).toList();
    }

    private PipelineStatusDefinition requireLeadDef(Tenant tenant, UUID id) {
        return definitionRepository.findByIdAndTenantAndArchivedFalse(id, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));
    }

    private PipelineStatusDefinition requireLeadDefByKey(Tenant tenant, String systemKey) {
        return definitionRepository
                .findByTenantAndPipelineTypeAndSystemKeyAndArchivedFalse(tenant, PipelineType.LEAD, systemKey)
                .orElseThrow(() -> new ResourceNotFoundException("Lead pipeline is not configured for this tenant."));
    }

    private PipelineStatusDefinition requireJobDefByKey(Tenant tenant, String systemKey) {
        return definitionRepository
                .findByTenantAndPipelineTypeAndSystemKeyAndArchivedFalse(tenant, PipelineType.JOB, systemKey)
                .orElseThrow(() -> new ResourceNotFoundException("Job pipeline is not configured for this tenant."));
    }

    private PickerItemDto leadToPickerItem(Lead l) {
        String label = l.getCustomer() != null
                ? (l.getCustomer().getFirstName() + " " + l.getCustomer().getLastName()).trim()
                : "—";
        String subLabel = l.getPropertyAddress() != null && l.getPropertyAddress().getLine1() != null
                ? l.getPropertyAddress().getLine1()
                : "";
        return new PickerItemDto(l.getId(), label, subLabel);
    }

    private Customer resolveCustomerForLead(Tenant tenant, UUID userId, CreateLeadRequest request) {
        if (request.getCustomerId() != null) {
            return customerRepository.findByIdAndTenantAndArchivedFalse(request.getCustomerId(), tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        NewLeadCustomerRequest newCustomer = request.getNewCustomer();
        if (newCustomer != null) {
            Customer customer = new Customer();
            customer.setTenant(tenant);
            AuditSupport.touchForCreate(customer, userId);
            customer.setFirstName(newCustomer.getFirstName());
            customer.setLastName(newCustomer.getLastName());
            customer.setPrimaryPhone(newCustomer.getPrimaryPhone());
            customer.setEmail(newCustomer.getEmail());

            if (newCustomer.getPreferredContactMethod() != null) {
                customer.setPreferredContactMethod(newCustomer.getPreferredContactMethod());
            }

            if (newCustomer.getBillingAddress() != null) {
                Address billing = new Address();
                applyAddress(billing, newCustomer.getBillingAddress());
                customer.setBillingAddress(billing);
            }

            return customerRepository.save(customer);
        }

        throw new IllegalArgumentException("Either customerId or newCustomer must be provided");
    }

    private void applyAddress(Address entity, AddressDto dto) {
        entity.setLine1(dto.getLine1());
        entity.setLine2(dto.getLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZip(dto.getZip());
        entity.setCountryCode(dto.getCountryCode());
    }

    private LeadDto toDto(Lead entity) {
        return toDto(entity, null);
    }

    private LeadDto toDto(Lead entity, UUID convertedJobId) {
        LeadDto dto = new LeadDto();
        dto.setId(entity.getId());
        PipelineStatusDefinition sd = entity.getStatusDefinition();
        dto.setStatusDefinitionId(sd.getId());
        dto.setStatusKey(sd.getSystemKey());
        dto.setStatusLabel(sd.getLabel());
        dto.setSource(entity.getSource());
        dto.setPipelinePosition(entity.getPipelinePosition());
        dto.setLeadNotes(entity.getLeadNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setConvertedJobId(convertedJobId);

        if (entity.getCustomer() != null) {
            Customer customer = entity.getCustomer();
            dto.setCustomerId(customer.getId());
            dto.setCustomerFirstName(customer.getFirstName());
            dto.setCustomerLastName(customer.getLastName());
            dto.setCustomerEmail(customer.getEmail());
            dto.setCustomerPhone(customer.getPrimaryPhone());
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

    private void copyAddress(Address source, Address target) {
        target.setLine1(source.getLine1());
        target.setLine2(source.getLine2());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setZip(source.getZip());
        target.setCountryCode(source.getCountryCode());
    }

    private JobDto toJobDto(Job entity) {
        JobDto dto = new JobDto();
        dto.setId(entity.getId());
        PipelineStatusDefinition sd = entity.getStatusDefinition();
        dto.setStatusDefinitionId(sd.getId());
        dto.setStatusKey(sd.getSystemKey());
        dto.setStatusLabel(sd.getLabel());
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
