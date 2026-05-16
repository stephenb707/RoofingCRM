package com.roofingcrm.service.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical validation for tenant app preference payloads (dashboard widgets, list columns, pipeline hub).
 * Drops unknown keys, preserves order of valid entries (first occurrence wins when deduplicating),
 * and falls back to defaults when nothing usable remains.
 * Keeps in sync with frontend {@code dashboardWidgetConfig.ts}, {@code listFieldConfig.ts}, and {@code pipelineNav.ts}.
 */
public final class AppPreferencesSanitizer {

    public static final Set<String> DASHBOARD_WIDGET_KEYS = Set.of(
            "metrics",
            "quickActions",
            "leadPipeline",
            "jobPipeline",
            "nextBestActions",
            "recentLeads",
            "upcomingJobs",
            "openTasks"
    );

    public static final Set<String> JOBS_LIST_FIELD_KEYS = Set.of(
            "type",
            "status",
            "customer",
            "propertyAddress",
            "scheduledStartDate",
            "crew",
            "updatedAt"
    );

    public static final Set<String> LEADS_LIST_FIELD_KEYS = Set.of(
            "customer",
            "propertyAddress",
            "status",
            "source",
            "createdAt"
    );

    public static final Set<String> CUSTOMERS_LIST_FIELD_KEYS = Set.of(
            "name",
            "phone",
            "email",
            "preferredContact",
            "createdAt"
    );

    public static final Set<String> TASKS_LIST_FIELD_KEYS = Set.of(
            "title",
            "status",
            "priority",
            "dueAt",
            "assignedTo",
            "related"
    );

    public static final Set<String> ESTIMATES_LIST_FIELD_KEYS = Set.of(
            "title",
            "status",
            "total",
            "issueDate",
            "validUntil"
    );

    /** Allowed {@code pipeline.defaultView} values — matches frontend {@code PipelineViewId}. */
    public static final Set<String> PIPELINE_DEFAULT_VIEW_VALUES = Set.of("leads", "jobs", "combined");

    private AppPreferencesSanitizer() {
    }

    public static Map<String, Object> sanitizeDashboard(Object raw) {
        List<String> fallback = stringListCopy(AppPreferencesDefaults.dashboard().get("widgets"));
        List<String> widgets = sanitizeOrderedStringIds(extractWidgets(raw), DASHBOARD_WIDGET_KEYS);
        if (widgets.isEmpty()) {
            widgets = fallback;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("widgets", widgets);
        return out;
    }

    public static Map<String, Object> sanitizeJobsList(Object raw) {
        return sanitizeListVisibleFields(raw, JOBS_LIST_FIELD_KEYS, AppPreferencesDefaults.jobsList());
    }

    public static Map<String, Object> sanitizeLeadsList(Object raw) {
        return sanitizeListVisibleFields(raw, LEADS_LIST_FIELD_KEYS, AppPreferencesDefaults.leadsList());
    }

    public static Map<String, Object> sanitizeCustomersList(Object raw) {
        return sanitizeListVisibleFields(raw, CUSTOMERS_LIST_FIELD_KEYS, AppPreferencesDefaults.customersList());
    }

    public static Map<String, Object> sanitizeTasksList(Object raw) {
        return sanitizeListVisibleFields(raw, TASKS_LIST_FIELD_KEYS, AppPreferencesDefaults.tasksList());
    }

    public static Map<String, Object> sanitizeEstimatesList(Object raw) {
        return sanitizeListVisibleFields(raw, ESTIMATES_LIST_FIELD_KEYS, AppPreferencesDefaults.estimatesList());
    }

    /**
     * Keeps only {@code defaultView} when it is one of {@link #PIPELINE_DEFAULT_VIEW_VALUES}; drops all other keys.
     */
    public static Map<String, Object> sanitizePipeline(Object raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(raw instanceof Map<?, ?> map)) {
            return out;
        }
        Object dv = map.get("defaultView");
        if (dv instanceof String s) {
            String v = s.trim();
            if (PIPELINE_DEFAULT_VIEW_VALUES.contains(v)) {
                out.put("defaultView", v);
            }
        }
        return out;
    }

    /**
     * Copies string-keyed entries from {@code raw} into a new map; drops non-string keys and unknown entries.
     */
    public static Map<String, Object> shallowCopyStringKeyedMap(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() instanceof String k) {
                out.put(k, e.getValue());
            }
        }
        return out;
    }

    private static Object extractWidgets(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        return map.get("widgets");
    }

    private static Map<String, Object> sanitizeListVisibleFields(
            Object raw, Set<String> allowed, Map<String, Object> defaults) {
        Object vf = null;
        if (raw instanceof Map<?, ?> map) {
            vf = map.get("visibleFields");
        }
        List<String> sanitized = sanitizeOrderedStringIds(vf, allowed);
        if (sanitized.isEmpty()) {
            sanitized = stringListCopy(defaults.get("visibleFields"));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("visibleFields", sanitized);
        return out;
    }

    /**
     * Keeps only allowed string identifiers in first-seen order; drops duplicates, nulls, non-strings, and unknown ids.
     */
    static List<String> sanitizeOrderedStringIds(Object rawList, Set<String> allowed) {
        List<String> out = new ArrayList<>();
        if (!(rawList instanceof List<?> list)) {
            return out;
        }
        Set<String> seen = new HashSet<>();
        for (Object o : list) {
            if (o instanceof String s && allowed.contains(s) && seen.add(s)) {
                out.add(s);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<String> stringListCopy(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> copy = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof String s) {
                copy.add(s);
            }
        }
        return copy;
    }
}
