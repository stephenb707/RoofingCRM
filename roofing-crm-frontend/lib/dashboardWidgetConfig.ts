/**
 * Centralized configuration for dashboard widgets.
 *
 * Each widget has a key that matches what is stored in
 * tenant preferences (dashboard.widgets = ["metrics", ...]).
 */

export interface DashboardWidgetDef {
  key: string;
  label: string;
  description: string;
  defaultVisible: boolean;
}

export const DASHBOARD_WIDGET_CONFIGS: DashboardWidgetDef[] = [
  { key: "metrics", label: "Summary Metrics", description: "Customer, lead, job, estimate, invoice, and task counts", defaultVisible: true },
  { key: "quickActions", label: "Quick Actions", description: "Shortcuts to create customers, leads, tasks, and more", defaultVisible: true },
  { key: "leadPipeline", label: "Lead Pipeline", description: "Pipeline status breakdown for leads with stats", defaultVisible: true },
  { key: "jobPipeline", label: "Job Pipeline", description: "Pipeline status breakdown for jobs", defaultVisible: true },
  { key: "nextBestActions", label: "Next Best Actions", description: "Actionable follow-up suggestions", defaultVisible: true },
  { key: "recentLeads", label: "Recent Leads", description: "Latest updated leads at a glance", defaultVisible: true },
  { key: "upcomingJobs", label: "Upcoming Jobs", description: "Jobs scheduled in the next two weeks", defaultVisible: true },
  { key: "openTasks", label: "Open Tasks", description: "Tasks that still need attention", defaultVisible: true },
];

const AVAILABLE_KEYS = new Set(DASHBOARD_WIDGET_CONFIGS.map((w) => w.key));

export function getDefaultDashboardWidgets(): string[] {
  return DASHBOARD_WIDGET_CONFIGS
    .filter((w) => w.defaultVisible)
    .map((w) => w.key);
}

/**
 * Resolve the ordered list of active widget keys from saved preferences.
 * Falls back to defaults if preferences are missing/empty.
 * Filters out stale keys no longer in config.
 */
export function resolveDashboardWidgets(saved: unknown): string[] {
  if (Array.isArray(saved) && saved.length > 0) {
    const valid = saved.filter(
      (k): k is string => typeof k === "string" && AVAILABLE_KEYS.has(k)
    );
    if (valid.length > 0) return valid;
  }
  return getDefaultDashboardWidgets();
}
