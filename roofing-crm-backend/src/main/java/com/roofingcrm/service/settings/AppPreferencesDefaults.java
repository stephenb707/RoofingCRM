package com.roofingcrm.service.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines default app preferences that mirror the current hard-coded UI.
 * When no tenant preferences have been saved, these values are returned
 * so the app behaves exactly as it does today.
 */
public final class AppPreferencesDefaults {

    private AppPreferencesDefaults() {}

    public static Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("widgets", List.of(
                "metrics", "quickActions", "leadPipeline", "jobPipeline",
                "nextBestActions", "recentLeads", "upcomingJobs", "openTasks"
        ));
        return m;
    }

    public static Map<String, Object> jobsList() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visibleFields", List.of(
                "type", "status", "propertyAddress",
                "scheduledStartDate", "updatedAt"
        ));
        return m;
    }

    public static Map<String, Object> leadsList() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visibleFields", List.of(
                "customer", "propertyAddress", "status",
                "source", "createdAt"
        ));
        return m;
    }

    public static Map<String, Object> customersList() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visibleFields", List.of("name", "phone", "email"));
        return m;
    }

    public static Map<String, Object> tasksList() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visibleFields", List.of(
                "title", "status", "priority", "dueAt", "assignedTo", "related"
        ));
        return m;
    }

    public static Map<String, Object> estimatesList() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("visibleFields", List.of(
                "title", "status", "total", "issueDate", "validUntil"
        ));
        return m;
    }

    public static Map<String, Object> allDefaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("dashboard", dashboard());
        m.put("jobsList", jobsList());
        m.put("leadsList", leadsList());
        m.put("customersList", customersList());
        m.put("tasksList", tasksList());
        m.put("estimatesList", estimatesList());
        return m;
    }
}
