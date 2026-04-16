"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { useAuthReady } from "@/lib/AuthContext";
import { queryKeys } from "@/lib/queryKeys";
import { getDashboardSummary } from "@/lib/dashboardApi";
import { getAppPreferences } from "@/lib/preferencesApi";
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { formatDate, formatDateShortWeekday, formatDateTime } from "@/lib/format";
import { resolveDashboardWidgets } from "@/lib/dashboardWidgetConfig";
import { jobStatusBadgeClass } from "@/lib/pipelineStatusVisuals";
import type {
  DashboardSummaryDto,
  DashboardJobSnippetDto,
  DashboardLeadSnippetDto,
  DashboardTaskSnippetDto,
} from "@/lib/types";
import type { PipelineStatusDefinitionDto } from "@/lib/pipelineStatusesApi";

function taskDetailHref(t: DashboardTaskSnippetDto): string {
  return `/app/tasks/${t.taskId}`;
}

function MetricCard({
  label,
  value,
  href,
}: {
  label: string;
  value: number;
  href: string;
}) {
  return (
    <Link
      href={href}
      className="block bg-white rounded-xl border border-slate-200 p-4 shadow-sm hover:border-sky-200 hover:shadow transition-all"
    >
      <div className="text-2xl font-bold text-slate-800 tabular-nums">{value}</div>
      <div className="text-xs font-medium text-slate-500 mt-1">{label}</div>
    </Link>
  );
}

// ---------------------------------------------------------------------------
// Individual widget components
// ---------------------------------------------------------------------------

function MetricsWidget({ data }: { data: DashboardSummaryDto }) {
  return (
    <section className="mb-8">
      <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-3">Summary</h2>
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
        <MetricCard label="Customers" value={data.customerCount} href="/app/customers" />
        <MetricCard label="Leads" value={data.leadCount} href="/app/leads" />
        <MetricCard label="Jobs" value={data.jobCount} href="/app/jobs" />
        <MetricCard label="Estimates" value={data.estimateCount} href="/app/jobs" />
        <MetricCard label="Invoices" value={data.invoiceCount} href="/app/jobs" />
        <MetricCard label="Open tasks" value={data.openTaskCount} href="/app/tasks" />
      </div>
    </section>
  );
}

function QuickActionsWidget() {
  return (
    <section className="mb-8">
      <h2 className="text-xs font-semibold uppercase tracking-wide text-slate-400 mb-3">Quick actions</h2>
      <div className="flex flex-wrap gap-2">
        <Link href="/app/customers/new" className="inline-flex items-center px-4 py-2.5 rounded-lg bg-sky-600 text-white text-sm font-medium hover:bg-sky-700 shadow-sm transition-colors">New customer</Link>
        <Link href="/app/leads/new" className="inline-flex items-center px-4 py-2.5 rounded-lg bg-white border border-slate-200 text-slate-700 text-sm font-medium hover:bg-slate-50 transition-colors">New lead</Link>
        <Link href="/app/tasks/new" className="inline-flex items-center px-4 py-2.5 rounded-lg bg-white border border-slate-200 text-slate-700 text-sm font-medium hover:bg-slate-50 transition-colors">Create task</Link>
        <Link href="/app/schedule" className="inline-flex items-center px-4 py-2.5 rounded-lg bg-white border border-slate-200 text-slate-700 text-sm font-medium hover:bg-slate-50 transition-colors">Schedule</Link>
        <Link href="/app/jobs" className="inline-flex items-center px-4 py-2.5 rounded-lg bg-white border border-slate-200 text-slate-700 text-sm font-medium hover:bg-slate-50 transition-colors">Jobs &amp; estimates</Link>
        <Link href="/app/reports" className="inline-flex items-center px-4 py-2.5 rounded-lg bg-white border border-slate-200 text-slate-700 text-sm font-medium hover:bg-slate-50 transition-colors">Reports</Link>
      </div>
    </section>
  );
}

function LeadPipelineWidget({
  data,
  defs,
}: {
  data: DashboardSummaryDto;
  defs: PipelineStatusDefinitionDto[];
}) {
  const sorted = useMemo(
    () => [...defs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder),
    [defs]
  );
  return (
    <section className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-sm font-semibold text-slate-800">Lead pipeline</h2>
        <Link href="/app/leads/pipeline" className="text-xs text-sky-600 hover:text-sky-700">Open pipeline →</Link>
      </div>
      <div className="space-y-2">
        {sorted.map((def) => {
          const n = data.leadCountByStatus[def.systemKey] ?? 0;
          return (
            <div key={def.id} className="flex items-center gap-3 text-sm">
              <span className="text-slate-600 w-36 shrink-0">{def.label}</span>
              <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                  className="h-full bg-sky-500 rounded-full transition-all"
                  style={{ width: `${data.leadCount > 0 ? Math.min(100, (n / data.leadCount) * 100) : 0}%` }}
                />
              </div>
              <span className="text-slate-800 font-medium tabular-nums w-8 text-right">{n}</span>
            </div>
          );
        })}
      </div>
      <div className="mt-4 pt-4 border-t border-slate-100 grid grid-cols-2 gap-3 text-xs text-slate-600">
        <div>
          <span className="text-slate-400">This week on schedule</span>
          <div className="text-base font-semibold text-slate-800">{data.jobsScheduledThisWeek}</div>
        </div>
        <div>
          <span className="text-slate-400">Unscheduled jobs</span>
          <div className="text-base font-semibold text-slate-800">{data.unscheduledJobsCount}</div>
        </div>
        <div>
          <span className="text-slate-400">Estimates awaiting response</span>
          <div className="text-base font-semibold text-slate-800">{data.estimatesSentCount}</div>
        </div>
        <div>
          <span className="text-slate-400">Unpaid invoices</span>
          <div className="text-base font-semibold text-slate-800">{data.unpaidInvoiceCount}</div>
        </div>
      </div>
    </section>
  );
}

function JobPipelineWidget({
  data,
  defs,
}: {
  data: DashboardSummaryDto;
  defs: PipelineStatusDefinitionDto[];
}) {
  const sorted = useMemo(
    () => [...defs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder),
    [defs]
  );
  return (
    <section className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-sm font-semibold text-slate-800">Job pipeline</h2>
        <Link href="/app/jobs/pipeline" className="text-xs text-sky-600 hover:text-sky-700">Open pipeline →</Link>
      </div>
      <div className="space-y-2">
        {sorted.map((def) => {
          const n = (data.jobCountByStatus ?? {})[def.systemKey] ?? 0;
          return (
            <div key={def.id} className="flex items-center gap-3 text-sm">
              <span className="text-slate-600 w-36 shrink-0">{def.label}</span>
              <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all ${jobStatusBadgeClass(def.systemKey).includes("emerald") ? "bg-emerald-500" : "bg-violet-500"}`}
                  style={{ width: `${data.jobCount > 0 ? Math.min(100, (n / data.jobCount) * 100) : 0}%` }}
                />
              </div>
              <span className="text-slate-800 font-medium tabular-nums w-8 text-right">{n}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function NextBestActionsWidget({ data }: { data: DashboardSummaryDto }) {
  return (
    <section className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm">
      <h2 className="text-sm font-semibold text-slate-800 mb-4">Next best actions</h2>
      <ul className="space-y-3 text-sm text-slate-600">
        {data.unscheduledJobsCount > 0 && (
          <li className="flex gap-2">
            <span className="text-amber-500 shrink-0">●</span>
            <span>
              <Link href="/app/jobs" className="text-sky-600 hover:text-sky-700 font-medium">
                {data.unscheduledJobsCount} job{data.unscheduledJobsCount === 1 ? "" : "s"}
              </Link>{" "}
              still need a start date—open Jobs to schedule.
            </span>
          </li>
        )}
        {data.activePipelineLeadCount > 0 && (
          <li className="flex gap-2">
            <span className="text-sky-500 shrink-0">●</span>
            <span>
              <Link href="/app/leads" className="text-sky-600 hover:text-sky-700 font-medium">
                {data.activePipelineLeadCount} active lead{data.activePipelineLeadCount === 1 ? "" : "s"}
              </Link>{" "}
              in the pipeline (not won or lost).
            </span>
          </li>
        )}
        {data.estimatesSentCount > 0 && (
          <li className="flex gap-2">
            <span className="text-violet-500 shrink-0">●</span>
            <span>
              <Link href="/app/jobs" className="text-sky-600 hover:text-sky-700 font-medium">
                {data.estimatesSentCount} estimate{data.estimatesSentCount === 1 ? "" : "s"}
              </Link>{" "}
              sent—follow up for a decision.
            </span>
          </li>
        )}
        {data.unpaidInvoiceCount > 0 && (
          <li className="flex gap-2">
            <span className="text-emerald-600 shrink-0">●</span>
            <span>
              <span className="font-medium text-slate-800">{data.unpaidInvoiceCount}</span> draft or
              sent invoice{data.unpaidInvoiceCount === 1 ? "" : "s"} still open—collect payment.
            </span>
          </li>
        )}
        {data.unscheduledJobsCount === 0 &&
          data.activePipelineLeadCount === 0 &&
          data.estimatesSentCount === 0 &&
          data.unpaidInvoiceCount === 0 && (
            <li className="text-slate-500">You&apos;re caught up on the usual follow-ups. Nice work.</li>
          )}
      </ul>
    </section>
  );
}

function RecentLeadsWidget({ data }: { data: DashboardSummaryDto }) {
  return (
    <section className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="px-5 py-3 border-b border-slate-100 flex justify-between items-center">
        <h2 className="text-sm font-semibold text-slate-800">Recent leads</h2>
        <Link href="/app/leads" className="text-xs text-sky-600 hover:text-sky-700">All leads</Link>
      </div>
      <ul className="divide-y divide-slate-100">
        {data.recentLeads.length === 0 && (
          <li className="px-5 py-8 text-sm text-slate-500 text-center">No leads yet.</li>
        )}
        {data.recentLeads.map((l: DashboardLeadSnippetDto) => (
          <li key={l.id}>
            <Link href={`/app/leads/${l.id}`} className="block px-5 py-3 hover:bg-slate-50 transition-colors">
              <div className="font-medium text-slate-800 text-sm">{l.customerLabel}</div>
              <div className="text-xs text-slate-500 mt-0.5">{l.statusLabel}{l.propertyLine1 ? ` · ${l.propertyLine1}` : ""}</div>
              <div className="text-xs text-slate-400 mt-1">Updated {formatDate(l.updatedAt)}</div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

function UpcomingJobsWidget({ data }: { data: DashboardSummaryDto }) {
  return (
    <section className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="px-5 py-3 border-b border-slate-100 flex justify-between items-center">
        <h2 className="text-sm font-semibold text-slate-800">Upcoming jobs</h2>
        <Link href="/app/schedule" className="text-xs text-sky-600 hover:text-sky-700">Calendar</Link>
      </div>
      <ul className="divide-y divide-slate-100">
        {data.upcomingJobs.length === 0 && (
          <li className="px-5 py-8 text-sm text-slate-500 text-center">No jobs in the next two weeks.</li>
        )}
        {data.upcomingJobs.map((j: DashboardJobSnippetDto) => (
          <li key={j.id}>
            <Link href={`/app/jobs/${j.id}`} className="block px-5 py-3 hover:bg-slate-50 transition-colors">
              <div className="font-medium text-slate-800 text-sm">{j.scheduledStartDate ? formatDateShortWeekday(j.scheduledStartDate) : "TBD"}</div>
              <div className="text-xs text-slate-500 mt-0.5">{j.propertyLine1 || j.customerLabel}</div>
              <div className="text-xs text-slate-400 mt-1">{j.customerLabel}</div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

function OpenTasksWidget({ data }: { data: DashboardSummaryDto }) {
  return (
    <section className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="px-5 py-3 border-b border-slate-100 flex justify-between items-center">
        <h2 className="text-sm font-semibold text-slate-800">Open tasks</h2>
        <Link href="/app/tasks" className="text-xs text-sky-600 hover:text-sky-700">All tasks</Link>
      </div>
      <ul className="divide-y divide-slate-100">
        {data.openTasks.length === 0 && (
          <li className="px-5 py-8 text-sm text-slate-500 text-center">No open tasks.</li>
        )}
        {data.openTasks.map((t: DashboardTaskSnippetDto) => (
          <li key={t.taskId}>
            <Link href={taskDetailHref(t)} className="block px-5 py-3 hover:bg-slate-50 transition-colors">
              <div className="font-medium text-slate-800 text-sm">{t.title}</div>
              <div className="text-xs text-slate-500 mt-0.5">Due {t.dueAt ? formatDateTime(t.dueAt) : "—"}</div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

// ---------------------------------------------------------------------------
// Layout helpers — group widgets into semantic layout zones
// ---------------------------------------------------------------------------

type WidgetKey = string;

const PAIRED_KEYS = new Set(["leadPipeline", "jobPipeline", "nextBestActions"]);
const CARD_KEYS = new Set(["recentLeads", "upcomingJobs", "openTasks"]);

export default function DashboardPage() {
  const { api, auth, ready } = useAuthReady();

  const { data, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.dashboardSummary(auth.selectedTenantId),
    queryFn: () => getDashboardSummary(api),
    enabled: ready && Boolean(auth.selectedTenantId),
  });

  const { data: prefs } = useQuery({
    queryKey: queryKeys.appPreferences(auth.selectedTenantId),
    queryFn: () => getAppPreferences(api),
    enabled: ready && Boolean(auth.selectedTenantId),
    staleTime: 60_000,
  });

  const { data: leadPipelineDefs = [] } = useQuery({
    queryKey: queryKeys.pipelineStatuses(auth.selectedTenantId, "LEAD"),
    queryFn: () => listPipelineStatuses(api, "LEAD"),
    enabled: ready && Boolean(auth.selectedTenantId),
  });

  const { data: jobPipelineDefs = [] } = useQuery({
    queryKey: queryKeys.pipelineStatuses(auth.selectedTenantId, "JOB"),
    queryFn: () => listPipelineStatuses(api, "JOB"),
    enabled: ready && Boolean(auth.selectedTenantId),
  });

  const activeWidgets = useMemo(
    () => resolveDashboardWidgets(prefs?.dashboard?.widgets),
    [prefs]
  );

  const renderWidget = (key: WidgetKey) => {
    if (!data) return null;
    switch (key) {
      case "metrics": return <MetricsWidget key={key} data={data} />;
      case "quickActions": return <QuickActionsWidget key={key} />;
      case "leadPipeline": return <LeadPipelineWidget key={key} data={data} defs={leadPipelineDefs} />;
      case "jobPipeline": return <JobPipelineWidget key={key} data={data} defs={jobPipelineDefs} />;
      case "nextBestActions": return <NextBestActionsWidget key={key} data={data} />;
      case "recentLeads": return <RecentLeadsWidget key={key} data={data} />;
      case "upcomingJobs": return <UpcomingJobsWidget key={key} data={data} />;
      case "openTasks": return <OpenTasksWidget key={key} data={data} />;
      default: return null;
    }
  };

  const topWidgets = activeWidgets.filter((k) => !PAIRED_KEYS.has(k) && !CARD_KEYS.has(k));
  const pairedWidgets = activeWidgets.filter((k) => PAIRED_KEYS.has(k));
  const cardWidgets = activeWidgets.filter((k) => CARD_KEYS.has(k));

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800">Dashboard</h1>
        <p className="text-sm text-slate-500 mt-1 max-w-2xl">
          Your roofing business at a glance—pipeline health, what&apos;s on the calendar, and the
          next things worth doing.
        </p>
      </div>

      {isLoading && (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading dashboard…</p>
        </div>
      )}

      {isError && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-sm text-red-800">
          {getApiErrorMessage(error, "Could not load dashboard.")}
        </div>
      )}

      {data && !isLoading && (
        <>
          {topWidgets.map((key) => renderWidget(key))}

          {pairedWidgets.length > 0 && (
            <div className={`grid ${pairedWidgets.length === 1 ? "grid-cols-1" : "lg:grid-cols-2"} gap-6 mb-8`}>
              {pairedWidgets.map((key) => renderWidget(key))}
            </div>
          )}

          {cardWidgets.length > 0 && (
            <div className={`grid ${cardWidgets.length === 1 ? "grid-cols-1" : cardWidgets.length === 2 ? "md:grid-cols-2" : "md:grid-cols-2 xl:grid-cols-3"} gap-6`}>
              {cardWidgets.map((key) => renderWidget(key))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
