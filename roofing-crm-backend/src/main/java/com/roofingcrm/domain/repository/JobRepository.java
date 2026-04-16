package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    long countByTenantAndArchivedFalse(Tenant tenant);

    long countByTenantAndStatusDefinitionAndArchivedFalse(Tenant tenant, PipelineStatusDefinition statusDefinition);

    @Query("""
            select count(j) from Job j
            where j.tenant = :tenant
              and j.archived = false
              and j.scheduledStartDate is null
            """)
    long countWithNoScheduledStartByTenant(@Param("tenant") Tenant tenant);

    @Query("""
            select count(j) from Job j
            where j.tenant = :tenant
              and j.archived = false
              and j.scheduledStartDate is not null
              and j.scheduledStartDate <= :rangeEnd
              and coalesce(j.scheduledEndDate, j.scheduledStartDate) >= :rangeStart
            """)
    long countScheduledInDateRange(
            @Param("tenant") Tenant tenant,
            @Param("rangeStart") LocalDate rangeStart,
            @Param("rangeEnd") LocalDate rangeEnd);

    @EntityGraph(attributePaths = {"customer", "lead", "statusDefinition"})
    @NonNull List<Job> findAll(@NonNull Specification<Job> spec, @NonNull Sort sort);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Job> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Job> findByTenantAndStatusDefinitionAndArchivedFalse(
            Tenant tenant, PipelineStatusDefinition statusDefinition, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Job> findByTenantAndCustomerIdAndArchivedFalse(Tenant tenant, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    Page<Job> findByTenantAndStatusDefinitionAndCustomerIdAndArchivedFalse(
            Tenant tenant, PipelineStatusDefinition statusDefinition, UUID customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "lead", "statusDefinition"})
    Optional<Job> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    @EntityGraph(attributePaths = {"customer", "lead", "statusDefinition"})
    Optional<Job> findByTenantAndLeadIdAndArchivedFalse(Tenant tenant, UUID leadId);

    @EntityGraph(attributePaths = {"customer", "statusDefinition"})
    @Query("""
        select j from Job j
        where j.tenant = :tenant
          and j.archived = false
          and coalesce(:statusDefinitionId, j.statusDefinition.id) = j.statusDefinition.id
          and (coalesce(:crewName, '') = '' or lower(j.assignedCrew) like lower(concat('%', :crewName, '%')))
          and (
            (j.scheduledStartDate is not null
              and j.scheduledStartDate <= :endDate
              and coalesce(j.scheduledEndDate, j.scheduledStartDate) >= :startDate
            )
            or (:includeUnscheduled = true and j.scheduledStartDate is null)
          )
        """)
    Page<Job> searchSchedule(
            @Param("tenant") Tenant tenant,
            @Param("statusDefinitionId") UUID statusDefinitionId,
            @Param("crewName") String crewName,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("includeUnscheduled") boolean includeUnscheduled,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"customer"})
    @Query("""
        select j from Job j
        left join j.customer c
        where j.tenant = :tenant
          and j.archived = false
          and (:q is null or :q = '' or
               lower(coalesce(c.firstName, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.lastName, '')) like lower(concat('%', :q, '%'))
               or lower(concat(coalesce(c.firstName, ''), ' ', coalesce(c.lastName, ''))) like lower(concat('%', :q, '%'))
               or lower(coalesce(j.propertyAddress.line1, '')) like lower(concat('%', :q, '%')))
        order by j.createdAt desc
        """)
    List<Job> searchForPicker(@Param("tenant") Tenant tenant, @Param("q") String q, Pageable pageable);
}
