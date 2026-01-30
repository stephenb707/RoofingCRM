package com.roofingcrm.service.task;

import com.roofingcrm.api.v1.task.CreateTaskRequest;
import com.roofingcrm.api.v1.task.TaskDto;
import com.roofingcrm.api.v1.task.UpdateTaskRequest;
import com.roofingcrm.domain.entity.*;
import com.roofingcrm.domain.enums.TaskPriority;
import com.roofingcrm.domain.enums.TaskStatus;
import com.roofingcrm.domain.repository.*;
import com.roofingcrm.domain.repository.spec.TaskSpecifications;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TenantAccessService tenantAccessService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TenantUserMembershipRepository tenantUserMembershipRepository;
    private final LeadRepository leadRepository;
    private final JobRepository jobRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public TaskServiceImpl(TenantAccessService tenantAccessService,
                           TaskRepository taskRepository,
                           UserRepository userRepository,
                           TenantUserMembershipRepository tenantUserMembershipRepository,
                           LeadRepository leadRepository,
                           JobRepository jobRepository,
                           CustomerRepository customerRepository) {
        this.tenantAccessService = tenantAccessService;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.tenantUserMembershipRepository = tenantUserMembershipRepository;
        this.leadRepository = leadRepository;
        this.jobRepository = jobRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    public TaskDto createTask(@NonNull UUID tenantId, @NonNull UUID userId, CreateTaskRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Task task = new Task();
        task.setTenant(tenant);
        task.setCreatedByUserId(userId);
        task.setUpdatedByUserId(userId);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setDueAt(request.getDueAt());

        resolveAssignedTo(task, tenant, request.getAssignedToUserId());
        resolveLead(task, tenant, request.getLeadId());
        resolveJob(task, tenant, request.getJobId());
        resolveCustomer(task, tenant, request.getCustomerId());

        if (task.getStatus() == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
            task.setCompletedAt(Instant.now());
        }

        Task saved = taskRepository.save(task);
        return toDto(saved);
    }

    @Override
    public TaskDto updateTask(@NonNull UUID tenantId, @NonNull UUID userId, UUID taskId, UpdateTaskRequest request) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Task task = taskRepository.findByIdAndTenantAndArchivedFalse(taskId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        task.setUpdatedByUserId(userId);

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank()) {
                throw new IllegalArgumentException("Title cannot be blank");
            }
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            TaskStatus newStatus = request.getStatus();
            task.setStatus(newStatus);
            if (newStatus == TaskStatus.COMPLETED && task.getCompletedAt() == null) {
                task.setCompletedAt(Instant.now());
            } else if (newStatus != TaskStatus.COMPLETED) {
                task.setCompletedAt(null);
            }
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getDueAt() != null) {
            task.setDueAt(request.getDueAt());
        }

        if (request.getAssignedToUserId() != null) {
            resolveAssignedTo(task, tenant, request.getAssignedToUserId());
        }
        if (request.getLeadId() != null) {
            resolveLead(task, tenant, request.getLeadId());
        }
        if (request.getJobId() != null) {
            resolveJob(task, tenant, request.getJobId());
        }
        if (request.getCustomerId() != null) {
            resolveCustomer(task, tenant, request.getCustomerId());
        }

        Task saved = taskRepository.save(task);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDto getTask(@NonNull UUID tenantId, @NonNull UUID userId, UUID taskId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Task task = taskRepository.findByIdAndTenantAndArchivedFalse(taskId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        return toDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskDto> listTasks(@NonNull UUID tenantId, @NonNull UUID userId,
            TaskStatus status, UUID assignedToUserId, UUID leadId, UUID jobId, UUID customerId,
            Instant dueBefore, Instant dueAfter, Pageable pageable) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Specification<Task> spec = TaskSpecifications.forTenantAndFilters(
                tenant, status, assignedToUserId, leadId, jobId, customerId, dueBefore, dueAfter);

        return taskRepository.findAll(spec, pageable).map(this::toDto);
    }

    private void resolveAssignedTo(Task task, Tenant tenant, UUID assignedToUserId) {
        if (assignedToUserId == null) {
            task.setAssignedTo(null);
            return;
        }
        User user = userRepository.findById(assignedToUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigned user not found"));
        tenantUserMembershipRepository.findByTenantAndUser(tenant, user)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this tenant"));
        task.setAssignedTo(user);
    }

    private void resolveLead(Task task, Tenant tenant, UUID leadId) {
        if (leadId == null) {
            task.setLead(null);
            return;
        }
        Lead lead = leadRepository.findByIdAndTenantAndArchivedFalse(leadId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        task.setLead(lead);
    }

    private void resolveJob(Task task, Tenant tenant, UUID jobId) {
        if (jobId == null) {
            task.setJob(null);
            return;
        }
        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        task.setJob(job);
    }

    private void resolveCustomer(Task task, Tenant tenant, UUID customerId) {
        if (customerId == null) {
            task.setCustomer(null);
            return;
        }
        Customer customer = customerRepository.findByIdAndTenantAndArchivedFalse(customerId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        task.setCustomer(customer);
    }

    private TaskDto toDto(Task entity) {
        TaskDto dto = new TaskDto();
        dto.setTaskId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setPriority(entity.getPriority());
        dto.setDueAt(entity.getDueAt());
        dto.setCompletedAt(entity.getCompletedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getAssignedTo() != null) {
            User u = entity.getAssignedTo();
            dto.setAssignedToUserId(u.getId());
            dto.setAssignedToName(formatFullName(u.getFullName()));
        }
        if (entity.getLead() != null) {
            dto.setLeadId(entity.getLead().getId());
        }
        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
        }
        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
        }

        return dto;
    }

    private static String formatFullName(String fullName) {
        return (fullName != null && !fullName.isBlank()) ? fullName.trim() : null;
    }
}
