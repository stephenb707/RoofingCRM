"use client";

import { useCallback } from "react";
import { useAppPreferences } from "@/lib/useAppPreferences";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  DASHBOARD_WIDGET_CONFIGS,
  getDefaultDashboardWidgets,
  resolveDashboardWidgets,
} from "@/lib/dashboardWidgetConfig";
import SettingsBackLink from "../SettingsBackLink";

export default function DashboardSettingsPage() {
  const { prefs, isLoading, isError, error, saving, canManageSettings, mutate } =
    useAppPreferences();

  const saveDashboardWidgets = useCallback(
    (widgets: string[]) => mutate({ dashboard: { widgets } }),
    [mutate]
  );

  const restoreDashboardDefaults = useCallback(() => {
    mutate({ dashboard: { widgets: getDefaultDashboardWidgets() } });
  }, [mutate]);

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

  const currentWidgets = resolveDashboardWidgets(prefs?.dashboard?.widgets);
  const defaults = getDefaultDashboardWidgets();
  const isDefault =
    currentWidgets.length === defaults.length &&
    currentWidgets.every((w, i) => w === defaults[i]);

  const toggleWidget = (key: string) => {
    if (!canManageSettings) return;
    const next = currentWidgets.includes(key)
      ? currentWidgets.filter((w) => w !== key)
      : [...currentWidgets, key];
    if (next.length > 0) saveDashboardWidgets(next);
  };

  const moveWidget = (key: string, direction: -1 | 1) => {
    if (!canManageSettings) return;
    const idx = currentWidgets.indexOf(key);
    if (idx < 0) return;
    const target = idx + direction;
    if (target < 0 || target >= currentWidgets.length) return;
    const next = [...currentWidgets];
    [next[idx], next[target]] = [next[target], next[idx]];
    saveDashboardWidgets(next);
  };

  return (
    <div className="max-w-3xl mx-auto">
      <SettingsBackLink />

      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Dashboard Settings</h1>
        <p className="text-sm text-slate-500 mt-1">
          Choose which widgets appear on your dashboard and arrange their order.
        </p>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm">
        <div className="p-6">
          <div className="space-y-1">
            {DASHBOARD_WIDGET_CONFIGS.map((widget) => {
              const active = currentWidgets.includes(widget.key);
              const idx = currentWidgets.indexOf(widget.key);
              return (
                <div
                  key={widget.key}
                  className={`flex items-center justify-between py-2.5 px-3 rounded-md ${
                    active ? "bg-white" : "bg-slate-50 opacity-60"
                  }`}
                >
                  <label className="flex items-center gap-3 cursor-pointer flex-1 min-w-0">
                    <input
                      type="checkbox"
                      checked={active}
                      onChange={() => toggleWidget(widget.key)}
                      disabled={!canManageSettings || saving}
                      className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                    />
                    <div className="min-w-0">
                      <span className="text-sm text-slate-700 font-medium">{widget.label}</span>
                      <p className="text-xs text-slate-400 mt-0.5">{widget.description}</p>
                    </div>
                  </label>
                  {active && (
                    <div className="flex items-center gap-1 ml-2 shrink-0">
                      <button
                        type="button"
                        onClick={() => moveWidget(widget.key, -1)}
                        disabled={!canManageSettings || saving || idx === 0}
                        className="p-1 text-slate-400 hover:text-slate-600 disabled:opacity-30"
                        aria-label={`Move ${widget.label} up`}
                      >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        onClick={() => moveWidget(widget.key, 1)}
                        disabled={!canManageSettings || saving || idx === currentWidgets.length - 1}
                        className="p-1 text-slate-400 hover:text-slate-600 disabled:opacity-30"
                        aria-label={`Move ${widget.label} down`}
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

          {!isDefault && canManageSettings && (
            <div className="mt-4 pt-4 border-t border-slate-100">
              <button
                type="button"
                onClick={restoreDashboardDefaults}
                disabled={saving}
                className="text-xs text-slate-500 hover:text-slate-700 underline disabled:opacity-50"
              >
                Restore default dashboard
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
