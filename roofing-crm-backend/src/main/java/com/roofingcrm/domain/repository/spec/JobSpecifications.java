package com.roofingcrm.domain.repository.spec;

import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.JobStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class JobSpecifications {

    private JobSpecifications() {
    }

    /**
     * Build specification for schedule query.
     * Matches: tenant + archived=false, optional status, optional crewName (case-insensitive),
     * and (scheduled overlap with [from,to] OR unscheduled when includeUnscheduled=true).
     */
    @NonNull
    public static Specification<Job> forSchedule(
            Tenant tenant,
            LocalDate from,
            LocalDate to,
            JobStatus status,
            String crewName,
            boolean includeUnscheduled) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenant"), tenant));
            predicates.add(cb.isFalse(root.get("archived")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (crewName != null && !crewName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("assignedCrew")), "%" + crewName.toLowerCase() + "%"));
            }

            // Date predicate: scheduled overlap OR unscheduled
            // Overlap: scheduledStartDate is not null AND scheduledStartDate <= to AND (scheduledEndDate is null OR scheduledEndDate >= from)
            var hasScheduledOverlap = cb.and(
                    cb.isNotNull(root.get("scheduledStartDate")),
                    cb.lessThanOrEqualTo(root.get("scheduledStartDate"), to),
                    cb.or(
                            cb.isNull(root.get("scheduledEndDate")),
                            cb.greaterThanOrEqualTo(root.get("scheduledEndDate"), from)
                    )
            );
            var isUnscheduled = cb.isNull(root.get("scheduledStartDate"));

            if (includeUnscheduled) {
                predicates.add(cb.or(hasScheduledOverlap, isUnscheduled));
            } else {
                predicates.add(hasScheduledOverlap);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
