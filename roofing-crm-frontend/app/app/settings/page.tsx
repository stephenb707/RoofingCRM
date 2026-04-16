"use client";

import Link from "next/link";
import { useAuthReady } from "@/lib/AuthContext";
import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";
import { getAppPreferences } from "@/lib/preferencesApi";
import { resolveDashboardWidgets, DASHBOARD_WIDGET_CONFIGS } from "@/lib/dashboardWidgetConfig";
import { LIST_FIELD_CONFIGS, resolveVisibleFields, type ListConfigKey, LIST_DISPLAY_NAMES } from "@/lib/listFieldConfig";

function ManageLink({ href }: { href: string }) {
  return (
    <Link
      href={href}
      className="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium text-sky-600 bg-sky-50 rounded-lg hover:bg-sky-100 transition-colors shrink-0"
    >
      Manage
      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
      </svg>
    </Link>
  );
}

export default function SettingsPage() {
  const { api, auth, ready } = useAuthReady();

  const currentTenant = auth.tenants.find(
    (t) => t.tenantId === auth.selectedTenantId
  );
  const canManageSettings =
    currentTenant?.role === "OWNER" || currentTenant?.role === "ADMIN";

  const { data: prefs, isLoading } = useQuery({
    queryKey: queryKeys.appPreferences(auth.selectedTenantId),
    queryFn: () => getAppPreferences(api),
    enabled: ready && Boolean(auth.selectedTenantId),
  });

  if (!ready || isLoading) {
    return (
      <div className="flex items-center justify-center py-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  const activeWidgets = resolveDashboardWidgets(prefs?.dashboard?.widgets);
  const widgetSummary = `${activeWidgets.length} of ${DASHBOARD_WIDGET_CONFIGS.length} widgets active`;

  const listKeys = Object.keys(LIST_FIELD_CONFIGS) as ListConfigKey[];
  const listSummaries = listKeys.map((k) => {
    const saved = prefs?.[k]?.visibleFields;
    const visible = resolveVisibleFields(k, saved);
    return `${LIST_DISPLAY_NAMES[k]}: ${visible.length}/${LIST_FIELD_CONFIGS[k].length}`;
  });

  const hasCustomPrefs = prefs?.updatedAt !== null;

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800">Settings</h1>
        <p className="text-sm text-slate-500 mt-1">
          Customize your dashboard, list pages, and pipeline stages.
        </p>
      </div>

      <div className="space-y-4">
        {/* Dashboard Settings */}
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-start gap-4">
            <div className="w-10 h-10 rounded-lg bg-sky-50 flex items-center justify-center shrink-0">
              <svg className="w-5 h-5 text-sky-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold text-slate-800">Dashboard</h2>
              <p className="text-sm text-slate-500 mt-1">
                Choose which widgets appear on your dashboard and their order.
              </p>
              <p className="text-xs text-slate-400 mt-2">{widgetSummary}</p>
            </div>
            <ManageLink href="/app/settings/dashboard" />
          </div>
        </section>

        {/* List View Settings */}
        <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
          <div className="flex items-start gap-4">
            <div className="w-10 h-10 rounded-lg bg-emerald-50 flex items-center justify-center shrink-0">
              <svg className="w-5 h-5 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-lg font-semibold text-slate-800">List Views</h2>
              <p className="text-sm text-slate-500 mt-1">
                Configure which columns are visible on each list page and their order.
              </p>
              <p className="text-xs text-slate-400 mt-2">{listSummaries.join(" · ")}</p>
            </div>
            <ManageLink href="/app/settings/list-views" />
          </div>
        </section>

        {/* Pipeline Settings */}
        {canManageSettings && (
          <section className="bg-white rounded-xl border border-slate-200 shadow-sm p-6">
            <div className="flex items-start gap-4">
              <div className="w-10 h-10 rounded-lg bg-violet-50 flex items-center justify-center shrink-0">
                <svg className="w-5 h-5 text-violet-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </div>
              <div className="flex-1 min-w-0">
                <h2 className="text-lg font-semibold text-slate-800">Pipeline Statuses</h2>
                <p className="text-sm text-slate-500 mt-1">
                  Manage your lead and job pipeline stages, labels, and ordering.
                </p>
              </div>
              <ManageLink href="/app/settings/pipeline-statuses" />
            </div>
          </section>
        )}
      </div>

      <div className="text-xs text-slate-400 text-center mt-8 pb-4">
        {hasCustomPrefs
          ? `Preferences last saved ${new Date(prefs!.updatedAt!).toLocaleDateString()}`
          : "Using default preferences"}
      </div>
    </div>
  );
}
