package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.PipelineStatusDefinition;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.PipelineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PipelineStatusDefinitionRepository extends JpaRepository<PipelineStatusDefinition, UUID> {

    List<PipelineStatusDefinition> findByTenantAndPipelineTypeAndArchivedFalseOrderBySortOrderAsc(
            Tenant tenant, PipelineType pipelineType);

    List<PipelineStatusDefinition> findByTenantAndPipelineTypeAndActiveTrueAndArchivedFalseOrderBySortOrderAsc(
            Tenant tenant, PipelineType pipelineType);

    Optional<PipelineStatusDefinition> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    Optional<PipelineStatusDefinition> findByTenantAndPipelineTypeAndSystemKeyAndArchivedFalse(
            Tenant tenant, PipelineType pipelineType, String systemKey);

    @Query("select count(l) from Lead l where l.tenant = :tenant and l.archived = false and l.statusDefinition.id = :defId")
    long countLeadsUsingDefinition(@Param("tenant") Tenant tenant, @Param("defId") UUID defId);

    @Query("select count(j) from Job j where j.tenant = :tenant and j.archived = false and j.statusDefinition.id = :defId")
    long countJobsUsingDefinition(@Param("tenant") Tenant tenant, @Param("defId") UUID defId);
}
