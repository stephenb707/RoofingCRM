"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { listScheduleJobs } from "@/lib/scheduleApi";
import { updateJob } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  JOB_STATUSES,
  JOB_STATUS_LABELS,
  JOB_TYPE_LABELS,
} from "@/lib/jobsConstants";
import { queryKeys } from "@/lib/queryKeys";
import {
  formatAddress,
  formatDate,
  formatLocalDateInput,
  parseLocalDateOnly,
} from "@/lib/format";
import { DateRangePicker } from "@/components/DateRangePicker";
import type { JobDto, JobStatus } from "@/lib/types";

function getMondayOfWeek(d: Date): string {
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1);
  const monday = new Date(d);
  monday.setDate(diff);
  return formatLocalDateInput(monday);
}

function getSundayOfWeek(d: Date): string {
  const monday = parseLocalDateOnly(getMondayOfWeek(d));
  monday.setDate(monday.getDate() + 6);
  return formatLocalDateInput(monday);
}

function getDatesInRange(start: string, end: string): string[] {
  const out: string[] = [];
  const s = parseLocalDateOnly(start);
  const e = parseLocalDateOnly(end);
  for (let d = new Date(s); d <= e; d.setDate(d.getDate() + 1)) {
    out.push(formatLocalDateInput(d));
  }
  return out;
}

export default function SchedulePage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const today = useMemo(() => new Date(), []);
  const defaultStart = useMemo(() => getMondayOfWeek(today), [today]);
  const defaultEnd = useMemo(() => getSundayOfWeek(today), [today]);
  const [startDate, setStartDate] = useState(defaultStart);
  const [endDate, setEndDate] = useState(defaultEnd);
  const [statusFilter, setStatusFilter] = useState<JobStatus | "">("");
  const [crewFilter, setCrewFilter] = useState("");
  const [includeUnscheduled, setIncludeUnscheduled] = useState(false);

  const [rescheduleJob, setRescheduleJob] = useState<JobDto | null>(null);
  const [rescheduleStart, setRescheduleStart] = useState("");
  const [rescheduleEnd, setRescheduleEnd] = useState("");
  const [rescheduleCrew, setRescheduleCrew] = useState("");

  const scheduleKey = queryKeys.scheduleJobs(
    auth.selectedTenantId,
    startDate,
    endDate,
    statusFilter || null,
    crewFilter || null,
    includeUnscheduled
  );

  const { data, isLoading, isError, error } = useQuery({
    queryKey: scheduleKey,
    queryFn: () =>
      listScheduleJobs(api, {
        startDate,
        endDate,
        status: statusFilter || undefined,
        crewName: crewFilter || undefined,
        includeUnscheduled,
        page: 0,
        size: 200,
      }),
    enabled: ready && !!auth.selectedTenantId,
  });

  const updateJobMutation = useMutation({
    mutationFn: (args: {
      jobId: string;
      scheduledStartDate?: string | null;
      scheduledEndDate?: string | null;
      clearSchedule?: boolean;
      crewName?: string | null;
    }) =>
      updateJob(api, args.jobId, {
        scheduledStartDate: args.clearSchedule ? undefined : (args.scheduledStartDate || undefined),
        scheduledEndDate: args.clearSchedule ? undefined : (args.scheduledEndDate || undefined),
        clearSchedule: args.clearSchedule,
        crewName: args.crewName ?? undefined,
      }),
    onSuccess: (_, variables) => {
      setRescheduleJob(null);
      queryClient.invalidateQueries({ queryKey: scheduleKey });
      queryClient.invalidateQueries({
        queryKey: queryKeys.job(auth.selectedTenantId, variables.jobId),
      });
    },
  });

  const openReschedule = (job: JobDto) => {
    setRescheduleJob(job);
    setRescheduleStart(job.scheduledStartDate ?? "");
    setRescheduleEnd(job.scheduledEndDate ?? "");
    setRescheduleCrew(job.crewName ?? "");
  };

  const handleRescheduleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!rescheduleJob) return;
    const clearSchedule = !rescheduleStart && !rescheduleEnd;
    updateJobMutation.mutate({
      jobId: rescheduleJob.id,
      scheduledStartDate: rescheduleStart || null,
      scheduledEndDate: rescheduleEnd || null,
      clearSchedule,
      crewName: rescheduleCrew || null,
    });
  };

  const jobs = data?.content ?? [];
  const datesInRange = useMemo(
    () => getDatesInRange(startDate, endDate),
    [startDate, endDate]
  );

  const hasActiveFilters =
    statusFilter !== "" ||
    crewFilter !== "" ||
    includeUnscheduled ||
    startDate !== defaultStart ||
    endDate !== defaultEnd;

  const handleClearFilters = () => {
    setStartDate(defaultStart);
    setEndDate(defaultEnd);
    setStatusFilter("");
    setCrewFilter("");
    setIncludeUnscheduled(false);
  };

  const jobsByDate = useMemo(() => {
    const map = new Map<string, JobDto[]>();
    for (const d of datesInRange) {
      map.set(d, []);
    }
    const unscheduled: JobDto[] = [];
    for (const job of jobs) {
      const sd = job.scheduledStartDate;
      if (!sd) {
        unscheduled.push(job);
      } else {
        const list = map.get(sd) ?? [];
        list.push(job);
        map.set(sd, list);
      }
    }
    return { byDate: map, unscheduled };
  }, [jobs, datesInRange]);

  if (!ready) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Schedule</h1>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 mb-6">
        <div className="flex flex-wrap items-end gap-4">
          <div className="min-w-[200px]">
            <label
              htmlFor="schedule-daterange"
              className="block text-sm font-medium text-slate-700 mb-1"
            >
              Date range
            </label>
            <DateRangePicker
              id="schedule-daterange"
              startDate={startDate}
              endDate={endDate}
              onChange={(start, end) => {
                setStartDate(start || defaultStart);
                setEndDate(end || defaultEnd);
              }}
              placeholder="Select date range…"
            />
          </div>
          <div>
            <label
              htmlFor="schedule-status"
              className="block text-sm font-medium text-slate-700 mb-1"
            >
              Status
            </label>
            <select
              id="schedule-status"
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as JobStatus | "")
              }
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {JOB_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {JOB_STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label
              htmlFor="schedule-crew"
              className="block text-sm font-medium text-slate-700 mb-1"
            >
              Crew
            </label>
            <input
              id="schedule-crew"
              type="text"
              placeholder="Filter by crew"
              value={crewFilter}
              onChange={(e) => setCrewFilter(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              id="schedule-unscheduled"
              type="checkbox"
              checked={includeUnscheduled}
              onChange={(e) => setIncludeUnscheduled(e.target.checked)}
              className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
            />
            <label
              htmlFor="schedule-unscheduled"
              className="text-sm font-medium text-slate-700"
            >
              Include unscheduled
            </label>
          </div>
          {hasActiveFilters && (
            <button
              onClick={handleClearFilters}
              className="text-sm text-slate-500 hover:text-slate-700 underline"
            >
              Clear filters
            </button>
          )}
        </div>
      </div>

      {isLoading && (
        <p className="text-sm text-slate-500 py-4">Loading schedule…</p>
      )}
      {isError && (
        <p className="text-sm text-red-600 py-4">
          {getApiErrorMessage(error, "Failed to load schedule")}
        </p>
      )}

      {!isLoading && !isError && (
        <div className="space-y-6">
          {datesInRange.map((dateStr) => {
            const dayJobs = jobsByDate.byDate.get(dateStr) ?? [];
            if (dayJobs.length === 0) return null;
            return (
              <section
                key={dateStr}
                className="bg-white rounded-xl border border-slate-200 p-4"
              >
                <h2 className="text-lg font-semibold text-slate-800 mb-3">
                  {formatDate(dateStr)}
                </h2>
                <ul className="space-y-2">
                  {dayJobs.map((job) => (
                    <JobScheduleCard
                      key={job.id}
                      job={job}
                      onReschedule={() => openReschedule(job)}
                    />
                  ))}
                </ul>
              </section>
            );
          })}

          {includeUnscheduled && jobsByDate.unscheduled.length > 0 && (
            <section className="bg-white rounded-xl border border-slate-200 p-4">
              <h2 className="text-lg font-semibold text-slate-800 mb-3">
                Unscheduled
              </h2>
              <ul className="space-y-2">
                {jobsByDate.unscheduled.map((job) => (
                  <JobScheduleCard
                    key={job.id}
                    job={job}
                    onReschedule={() => openReschedule(job)}
                  />
                ))}
              </ul>
            </section>
          )}

          {jobs.length === 0 && (
            <p className="text-sm text-slate-500 py-8">
              No jobs in this range.
              {!includeUnscheduled && " Try enabling “Include unscheduled”."}
            </p>
          )}
        </div>
      )}

      {/* Reschedule Modal */}
      {rescheduleJob && (
        <div
          className="fixed inset-0 bg-black/40 flex items-center justify-center z-50"
          onClick={() => setRescheduleJob(null)}
        >
          <div
            className="bg-white rounded-xl p-6 w-full max-w-md shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-semibold text-slate-800 mb-4">
              Reschedule
            </h3>
            <form onSubmit={handleRescheduleSave} className="space-y-4">
              <div>
                <label
                  htmlFor="reschedule-dates"
                  className="block text-sm font-medium text-slate-700 mb-1"
                >
                  Scheduled dates
                </label>
                <DateRangePicker
                  id="reschedule-dates"
                  startDate={rescheduleStart}
                  endDate={rescheduleEnd}
                  onChange={(start, end) => {
                    setRescheduleStart(start);
                    setRescheduleEnd(end);
                  }}
                  placeholder="Select date range…"
                />
              </div>
              <div>
                <label
                  htmlFor="reschedule-crew"
                  className="block text-sm font-medium text-slate-700 mb-1"
                >
                  Crew
                </label>
                <input
                  id="reschedule-crew"
                  type="text"
                  value={rescheduleCrew}
                  onChange={(e) => setRescheduleCrew(e.target.value)}
                  placeholder="Crew name"
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                />
              </div>
              <div className="flex gap-2 pt-2">
              <button
                  type="submit"
                  disabled={updateJobMutation.isPending}
                  className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60"
                >
                  {updateJobMutation.isPending ? "Saving…" : "Save"}
                </button>
                <button
                  type="button"
                  onClick={() => setRescheduleJob(null)}
                  className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
                >
                  Cancel
                </button>
              </div>
            </form>
            {updateJobMutation.isError && (
              <p className="mt-2 text-sm text-red-600">
                {getApiErrorMessage(updateJobMutation.error, "Failed to update")}
              </p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function JobScheduleCard({
  job,
  onReschedule,
}: {
  job: JobDto;
  onReschedule: () => void;
}) {
  const addr = job.propertyAddress;
  const cityState = [addr?.city, addr?.state].filter(Boolean).join(", ");

  return (
    <li className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-100">
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-slate-500">
            {JOB_TYPE_LABELS[job.type]}
          </span>
          <span className="text-xs text-slate-400">·</span>
          <span className="text-xs text-slate-500">{job.status}</span>
        </div>
        <p className="text-sm font-medium text-slate-800 truncate">
          {addr?.line1 ?? "—"}
          {cityState ? `, ${cityState}` : ""}
        </p>
        <div className="flex items-center gap-3 text-xs text-slate-500 mt-1">
          {job.scheduledStartDate ? (
            <span>
              {formatDate(job.scheduledStartDate)}
              {job.scheduledEndDate &&
                job.scheduledEndDate !== job.scheduledStartDate &&
                ` – ${formatDate(job.scheduledEndDate)}`}
            </span>
          ) : (
            <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded bg-slate-100 text-slate-600 border border-slate-200">
              Unscheduled
            </span>
          )}
          {job.crewName && (
            <span className="font-medium">{job.crewName}</span>
          )}
        </div>
      </div>
      <div className="flex items-center gap-2 shrink-0">
        <Link
          href={`/app/jobs/${job.id}`}
          className="px-3 py-1.5 text-sm font-medium text-sky-600 hover:bg-sky-50 rounded-lg"
        >
          Open
        </Link>
        <button
          type="button"
          onClick={onReschedule}
          className="px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg border border-slate-200"
        >
          Reschedule
        </button>
      </div>
    </li>
  );
}
