package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Task;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = {"assignedTo", "lead", "job", "customer"})
    @NonNull
    Page<Task> findAll(@NonNull Specification<Task> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"assignedTo", "lead", "job", "customer"})
    Optional<Task> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
