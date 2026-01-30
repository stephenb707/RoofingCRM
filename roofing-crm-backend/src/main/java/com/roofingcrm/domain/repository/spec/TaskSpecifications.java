package com.roofingcrm.domain.repository.spec;

import com.roofingcrm.domain.entity.Task;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.TaskStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> forTenantAndFilters(
            Tenant tenant,
            TaskStatus status,
            UUID assignedToUserId,
            UUID leadId,
            UUID jobId,
            UUID customerId,
            Instant dueBefore,
            Instant dueAfter
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenant"), tenant));
            predicates.add(cb.isFalse(root.get("archived")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (assignedToUserId != null) {
                var assignedToJoin = root.join("assignedTo", JoinType.INNER);
                predicates.add(cb.equal(assignedToJoin.get("id"), assignedToUserId));
            }

            if (leadId != null) {
                var leadJoin = root.join("lead", JoinType.INNER);
                predicates.add(cb.equal(leadJoin.get("id"), leadId));
            }

            if (jobId != null) {
                var jobJoin = root.join("job", JoinType.INNER);
                predicates.add(cb.equal(jobJoin.get("id"), jobId));
            }

            if (customerId != null) {
                var customerJoin = root.join("customer", JoinType.INNER);
                predicates.add(cb.equal(customerJoin.get("id"), customerId));
            }

            if (dueBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueAt"), dueBefore));
            }

            if (dueAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueAt"), dueAfter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
