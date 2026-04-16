"use client";

import { useState, useCallback } from "react";
import { useAppPreferences } from "@/lib/useAppPreferences";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  LIST_FIELD_CONFIGS,
  LIST_DISPLAY_NAMES,
  getDefaultVisibleFields,
  resolveVisibleFields,
  type ListConfigKey,
} from "@/lib/listFieldConfig";
import type { AppPreferencesDto } from "@/lib/types";
import SettingsBackLink from "../SettingsBackLink";

export default function ListViewSettingsPage() {
  const { prefs, isLoading, isError, error, saving, canManageSettings, mutate } =
    useAppPreferences();

  const saveListFields = useCallback(
    (listKey: ListConfigKey, fields: string[]) => {
      mutate({ [listKey]: { visibleFields: fields } });
    },
    [mutate]
  );

  const restoreListDefaults = useCallback(
    (listKey: ListConfigKey) => {
      const defaults = getDefaultVisibleFields(listKey);
      mutate({ [listKey]: { visibleFields: defaults } });
    },
    [mutate]
  );

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-3xl mx-auto">
        <SettingsBackLink />
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700 text-sm">
          {getApiErrorMessage(error, "Failed to load preferences.")}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto">
      <SettingsBackLink />

      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">List View Settings</h1>
        <p className="text-sm text-slate-500 mt-1">
          Choose which columns are visible on each list page and their order.
        </p>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="p-6 space-y-4">
          {(Object.keys(LIST_FIELD_CONFIGS) as ListConfigKey[]).map((listKey) => (
            <ListFieldEditor
              key={listKey}
              listKey={listKey}
              prefs={prefs}
              onSave={saveListFields}
              onRestore={restoreListDefaults}
              saving={saving}
              disabled={!canManageSettings}
            />
          ))}
        </div>
      </div>
    </div>
  );
}

// ---------------------------------------------------------------------------
// List field editor component (extracted from old settings page)
// ---------------------------------------------------------------------------

function ListFieldEditor({
  listKey,
  prefs,
  onSave,
  onRestore,
  saving,
  disabled,
}: {
  listKey: ListConfigKey;
  prefs: AppPreferencesDto | null;
  onSave: (listKey: ListConfigKey, fields: string[]) => void;
  onRestore: (listKey: ListConfigKey) => void;
  saving: boolean;
  disabled: boolean;
}) {
  const allFields = LIST_FIELD_CONFIGS[listKey];
  const savedFields = prefs?.[listKey]?.visibleFields;
  const currentVisible = resolveVisibleFields(listKey, savedFields);
  const defaults = getDefaultVisibleFields(listKey);
  const isDefault =
    currentVisible.length === defaults.length &&
    currentVisible.every((f, i) => f === defaults[i]);

  const [expanded, setExpanded] = useState(false);

  const toggleField = (key: string) => {
    if (disabled) return;
    const next = currentVisible.includes(key)
      ? currentVisible.filter((f) => f !== key)
      : [...currentVisible, key];
    if (next.length > 0) onSave(listKey, next);
  };

  const moveField = (key: string, direction: -1 | 1) => {
    if (disabled) return;
    const idx = currentVisible.indexOf(key);
    if (idx < 0) return;
    const target = idx + direction;
    if (target < 0 || target >= currentVisible.length) return;
    const next = [...currentVisible];
    [next[idx], next[target]] = [next[target], next[idx]];
    onSave(listKey, next);
  };

  return (
    <div className="border border-slate-200 rounded-lg">
      <button
        type="button"
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-slate-50 transition-colors rounded-lg"
      >
        <div className="flex items-center gap-3">
          <span className="text-sm font-semibold text-slate-700">
            {LIST_DISPLAY_NAMES[listKey]}
          </span>
          <span className="text-xs text-slate-400">
            {currentVisible.length} of {allFields.length} fields
          </span>
          {!isDefault && (
            <span className="text-xs font-medium text-sky-600 bg-sky-50 px-1.5 py-0.5 rounded">
              Customized
            </span>
          )}
        </div>
        <svg
          className={`w-4 h-4 text-slate-400 transition-transform ${expanded ? "rotate-180" : ""}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {expanded && (
        <div className="px-4 pb-4 border-t border-slate-100">
          <div className="mt-3 space-y-1">
            {allFields.map((field) => {
              const visible = currentVisible.includes(field.key);
              const idx = currentVisible.indexOf(field.key);
              return (
                <div
                  key={field.key}
                  className={`flex items-center justify-between py-2 px-3 rounded-md ${
                    visible ? "bg-white" : "bg-slate-50 opacity-60"
                  }`}
                >
                  <label className="flex items-center gap-3 cursor-pointer flex-1 min-w-0">
                    <input
                      type="checkbox"
                      checked={visible}
                      onChange={() => toggleField(field.key)}
                      disabled={disabled || saving}
                      className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                    />
                    <span className="text-sm text-slate-700">{field.label}</span>
                  </label>
                  {visible && (
                    <div className="flex items-center gap-1 ml-2">
                      <button
                        type="button"
                        onClick={() => moveField(field.key, -1)}
                        disabled={disabled || saving || idx === 0}
                        className="p-1 text-slate-400 hover:text-slate-600 disabled:opacity-30"
                        aria-label={`Move ${field.label} up`}
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        onClick={() => moveField(field.key, 1)}
                        disabled={disabled || saving || idx === currentVisible.length - 1}
                        className="p-1 text-slate-400 hover:text-slate-600 disabled:opacity-30"
                        aria-label={`Move ${field.label} down`}
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {!isDefault && !disabled && (
            <div className="mt-3 pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => onRestore(listKey)}
                disabled={saving}
                className="text-xs text-slate-500 hover:text-slate-700 underline disabled:opacity-50"
              >
                Restore defaults
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
