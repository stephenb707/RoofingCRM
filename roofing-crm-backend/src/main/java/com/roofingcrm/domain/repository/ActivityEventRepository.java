package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.ActivityEvent;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.ActivityEntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    @EntityGraph(attributePaths = {"createdBy"})
    Page<ActivityEvent> findByTenantAndEntityTypeAndEntityIdAndArchivedFalseOrderByCreatedAtDesc(
            Tenant tenant,
            ActivityEntityType entityType,
            UUID entityId,
            Pageable pageable);
}
