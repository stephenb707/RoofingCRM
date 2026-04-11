package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Task;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    @Query("""
            select count(t) from Task t
            where t.tenant = :tenant
              and t.archived = false
              and t.status in (com.roofingcrm.domain.enums.TaskStatus.TODO, com.roofingcrm.domain.enums.TaskStatus.IN_PROGRESS)
            """)
    long countOpenByTenant(@Param("tenant") Tenant tenant);

    @EntityGraph(attributePaths = {"assignedTo", "lead", "job", "customer"})
    @NonNull
    Page<Task> findAll(@NonNull Specification<Task> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"assignedTo", "lead", "job", "customer"})
    Optional<Task> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);
}
