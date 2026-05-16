package com.roofingcrm.service.settings;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("null")
class AppPreferencesSanitizerTest {

    @Test
    void sanitizeDashboard_preservesOrderAndDropsUnknownWidgets() {
        Map<String, Object> raw = Map.of(
                "widgets", List.of("recentLeads", "aliens", "metrics"),
                "extra", "ignored");
        Map<String, Object> out = AppPreferencesSanitizer.sanitizeDashboard(raw);
        assertEquals(List.of("recentLeads", "metrics"), out.get("widgets"));
        assertEquals(1, out.size());
    }

    @Test
    void sanitizeDashboard_fallsBackToDefaultsWhenNothingValid() {
        List<Object> widgets = Arrays.asList("nope", 123, null);
        Map<String, Object> raw = Map.of("widgets", widgets);
        Map<String, Object> out = AppPreferencesSanitizer.sanitizeDashboard(raw);
        assertEquals(AppPreferencesDefaults.dashboard().get("widgets"), out.get("widgets"));
    }

    @Test
    void sanitizeDashboard_deduplicatesWidgetsPreservingFirstOccurrence() {
        Map<String, Object> raw = Map.of(
                "widgets", List.of("metrics", "openTasks", "metrics", "recentLeads", "openTasks"));
        Map<String, Object> out = AppPreferencesSanitizer.sanitizeDashboard(raw);
        assertEquals(List.of("metrics", "openTasks", "recentLeads"), out.get("widgets"));
    }

    @Test
    void sanitizeJobsList_preservesOrderAndDropsUnknownFields() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("visibleFields", List.of("status", "hack", "type"));
        raw.put("noise", true);
        Map<String, Object> out = AppPreferencesSanitizer.sanitizeJobsList(raw);
        assertEquals(List.of("status", "type"), out.get("visibleFields"));
        assertEquals(1, out.size());
    }

    @Test
    void sanitizeJobsList_fallsBackToDefaultsWhenAllFieldsInvalid() {
        Map<String, Object> raw = Map.of("visibleFields", List.of("xxx"));
        Map<String, Object> out = AppPreferencesSanitizer.sanitizeJobsList(raw);
        assertEquals(AppPreferencesDefaults.jobsList().get("visibleFields"), out.get("visibleFields"));
    }

    @Test
    void sanitizeJobsList_deduplicatesVisibleFieldsPreservingOrder() {
        Map<String, Object> raw = Map.of(
                "visibleFields", List.of("status", "type", "status", "crew", "type"));
        Map<String, Object> out = AppPreferencesSanitizer.sanitizeJobsList(raw);
        assertEquals(List.of("status", "type", "crew"), out.get("visibleFields"));
    }

    @Test
    void sanitizeOrderedStringIds_skipsNonStrings() {
        List<String> out = AppPreferencesSanitizer.sanitizeOrderedStringIds(
                List.of("title", 99, "bogus", "status"),
                AppPreferencesSanitizer.ESTIMATES_LIST_FIELD_KEYS);
        assertEquals(List.of("title", "status"), out);
    }

    @Test
    void sanitizeOrderedStringIds_deduplicates() {
        assertEquals(
                List.of("title", "status"),
                AppPreferencesSanitizer.sanitizeOrderedStringIds(
                        List.of("title", "title", "bogus", "status"),
                        AppPreferencesSanitizer.ESTIMATES_LIST_FIELD_KEYS));
    }

    @Test
    void sanitizePipeline_keepsDefaultViewWhenValid() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("defaultView", "combined");
        raw.put("legacyNoise", true);
        Map<String, Object> out = AppPreferencesSanitizer.sanitizePipeline(raw);
        assertEquals(Map.of("defaultView", "combined"), out);
    }

    @Test
    void sanitizePipeline_trimsDefaultView() {
        Map<String, Object> raw = Map.of("defaultView", "  jobs  ");
        Map<String, Object> out = AppPreferencesSanitizer.sanitizePipeline(raw);
        assertEquals(Map.of("defaultView", "jobs"), out);
    }

    @Test
    void sanitizePipeline_invalidDefaultViewReturnsEmptyMap() {
        assertTrue(AppPreferencesSanitizer.sanitizePipeline(Map.of("defaultView", "hack")).isEmpty());
    }

    @Test
    void sanitizePipeline_nonStringDefaultViewIgnored() {
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("defaultView", 99);
        assertTrue(AppPreferencesSanitizer.sanitizePipeline(raw).isEmpty());
    }

    @Test
    void shallowCopyStringKeyedMap_dropsNonStringKeys() {
        Map<Object, Object> messy = new LinkedHashMap<>();
        messy.put("mode", "combined");
        messy.put(1, "bad");
        Map<String, Object> copy = AppPreferencesSanitizer.shallowCopyStringKeyedMap(messy);
        assertEquals(Map.of("mode", "combined"), copy);
    }
}
