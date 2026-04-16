package com.roofingcrm.api.v1.settings;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class AppPreferencesDto {
    private Map<String, Object> dashboard;
    private Map<String, Object> jobsList;
    private Map<String, Object> leadsList;
    private Map<String, Object> customersList;
    private Map<String, Object> tasksList;
    private Map<String, Object> estimatesList;
    /** Pipeline UI preferences (e.g. default combined vs lead-only view). */
    private Map<String, Object> pipeline;
    private Instant updatedAt;
}
