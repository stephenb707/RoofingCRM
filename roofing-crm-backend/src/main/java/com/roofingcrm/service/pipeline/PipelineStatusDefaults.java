package com.roofingcrm.service.pipeline;

import com.roofingcrm.domain.enums.PipelineType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Canonical built-in keys, order, and default English labels (used for seeding and restore).
 */
public final class PipelineStatusDefaults {

    private PipelineStatusDefaults() {
    }

    private static final LinkedHashMap<String, String> LEAD = new LinkedHashMap<>();

    static {
        LEAD.put("NEW", "New");
        LEAD.put("CONTACTED", "Contacted");
        LEAD.put("INSPECTION_SCHEDULED", "Inspection Scheduled");
        LEAD.put("QUOTE_SENT", "Quote Sent");
        LEAD.put("WON", "Won");
        LEAD.put("LOST", "Lost");
    }

    private static final LinkedHashMap<String, String> JOB = new LinkedHashMap<>();

    static {
        JOB.put("UNSCHEDULED", "Unscheduled");
        JOB.put("SCHEDULED", "Scheduled");
        JOB.put("IN_PROGRESS", "In Progress");
        JOB.put("COMPLETED", "Completed");
        JOB.put("INVOICED", "Invoiced");
    }

    public static Map<String, String> defaultLabels(PipelineType type) {
        return type == PipelineType.LEAD ? Map.copyOf(LEAD) : Map.copyOf(JOB);
    }

    public static List<String> orderedKeys(PipelineType type) {
        return type == PipelineType.LEAD
                ? List.copyOf(LEAD.keySet())
                : List.copyOf(JOB.keySet());
    }

    public static int defaultSortOrder(PipelineType type, String systemKey) {
        List<String> keys = orderedKeys(type);
        int i = keys.indexOf(systemKey);
        return i >= 0 ? i : keys.size();
    }

    public static String defaultLabel(PipelineType type, String systemKey) {
        return defaultLabels(type).getOrDefault(systemKey, systemKey);
    }

    /** Keys that must exist for a healthy tenant (all built-ins). */
    public static List<String> requiredBuiltInKeys(PipelineType type) {
        return orderedKeys(type);
    }
}
