"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { listJobSchedule, updateJob } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  JOB_STATUSES,
  JOB_STATUS_LABELS,
  JOB_TYPE_LABELS,
} from "@/lib/jobsConstants";
import { queryKeys } from "@/lib/queryKeys";
import {
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
  const [includeUnscheduled, setIncludeUnscheduled] = useState(true);

  const [editingJobId, setEditingJobId] = useState<string | null>(null);
  const [editStart, setEditStart] = useState("");
  const [editEnd, setEditEnd] = useState("");
  const [editCrew, setEditCrew] = useState("");

  const scheduleKey = queryKeys.jobSchedule(
    auth.selectedTenantId,
    startDate,
    endDate,
    statusFilter || null,
    crewFilter || null,
    includeUnscheduled
  );

  const { data: jobs = [], isLoading, isError, error } = useQuery({
    queryKey: scheduleKey,
    queryFn: () =>
      listJobSchedule(api, {
        from: startDate,
        to: endDate,
        status: statusFilter || undefined,
        crewName: crewFilter || undefined,
        includeUnscheduled,
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
      setEditingJobId(null);
      queryClient.invalidateQueries({ queryKey: scheduleKey });
      queryClient.invalidateQueries({
        queryKey: queryKeys.job(auth.selectedTenantId, variables.jobId),
      });
    },
  });

  const openEdit = (job: JobDto) => {
    setEditingJobId(job.id);
    setEditStart(job.scheduledStartDate ?? "");
    setEditEnd(job.scheduledEndDate ?? "");
    setEditCrew(job.crewName ?? "");
  };

  const handleSaveEdit = (e: React.FormEvent, jobId: string) => {
    e.preventDefault();
    const clearSchedule = !editStart && !editEnd;
    updateJobMutation.mutate({
      jobId,
      scheduledStartDate: editStart || null,
      scheduledEndDate: editEnd || null,
      clearSchedule,
      crewName: editCrew || null,
    });
  };

  const goPrevWeek = () => {
    const m = parseLocalDateOnly(startDate);
    m.setDate(m.getDate() - 7);
    const newStart = formatLocalDateInput(m);
    const newEnd = getSundayOfWeek(m);
    setStartDate(newStart);
    setEndDate(newEnd);
  };

  const goNextWeek = () => {
    const m = parseLocalDateOnly(startDate);
    m.setDate(m.getDate() + 7);
    const newStart = formatLocalDateInput(m);
    const newEnd = getSundayOfWeek(m);
    setStartDate(newStart);
    setEndDate(newEnd);
  };

  const jumpToWeek = (dateStr: string) => {
    if (!dateStr) return;
    const d = parseLocalDateOnly(dateStr);
    const newStart = getMondayOfWeek(d);
    const newEnd = getSundayOfWeek(d);
    setStartDate(newStart);
    setEndDate(newEnd);
  };

  const datesInRange = useMemo(
    () => getDatesInRange(startDate, endDate),
    [startDate, endDate]
  );

  const hasActiveFilters =
    statusFilter !== "" ||
    crewFilter !== "" ||
    !includeUnscheduled ||
    startDate !== defaultStart ||
    endDate !== defaultEnd;

  const handleClearFilters = () => {
    setStartDate(defaultStart);
    setEndDate(defaultEnd);
    setStatusFilter("");
    setCrewFilter("");
    setIncludeUnscheduled(true);
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
              htmlFor="schedule-jump-date"
              className="block text-sm font-medium text-slate-700 mb-1"
            >
              Jump to week
            </label>
            <input
              id="schedule-jump-date"
              type="date"
              value={startDate}
              onChange={(e) => jumpToWeek(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={goPrevWeek}
              className="px-3 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
            >
              ← Prev
            </button>
            <button
              type="button"
              onClick={goNextWeek}
              className="px-3 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
            >
              Next →
            </button>
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
                      isEditing={editingJobId === job.id}
                      editStart={editStart}
                      editEnd={editEnd}
                      editCrew={editCrew}
                      onEditStartChange={setEditStart}
                      onEditEndChange={setEditEnd}
                      onEditCrewChange={setEditCrew}
                      onEdit={() => openEdit(job)}
                      onSave={(e) => handleSaveEdit(e, job.id)}
                      onCancel={() => setEditingJobId(null)}
                      isSaving={updateJobMutation.isPending}
                      saveError={updateJobMutation.isError ? getApiErrorMessage(updateJobMutation.error, "Failed to update") : null}
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
                    isEditing={editingJobId === job.id}
                    editStart={editStart}
                    editEnd={editEnd}
                    editCrew={editCrew}
                    onEditStartChange={setEditStart}
                    onEditEndChange={setEditEnd}
                    onEditCrewChange={setEditCrew}
                    onEdit={() => openEdit(job)}
                    onSave={(e) => handleSaveEdit(e, job.id)}
                    onCancel={() => setEditingJobId(null)}
                    isSaving={updateJobMutation.isPending}
                    saveError={updateJobMutation.isError ? getApiErrorMessage(updateJobMutation.error, "Failed to update") : null}
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
    </div>
  );
}

function JobScheduleCard({
  job,
  isEditing,
  editStart,
  editEnd,
  editCrew,
  onEditStartChange,
  onEditEndChange,
  onEditCrewChange,
  onEdit,
  onSave,
  onCancel,
  isSaving,
  saveError,
}: {
  job: JobDto;
  isEditing: boolean;
  editStart: string;
  editEnd: string;
  editCrew: string;
  onEditStartChange: (v: string) => void;
  onEditEndChange: (v: string) => void;
  onEditCrewChange: (v: string) => void;
  onEdit: () => void;
  onSave: (e: React.FormEvent) => void;
  onCancel: () => void;
  isSaving: boolean;
  saveError: string | null;
}) {
  const addr = job.propertyAddress;
  const cityState = [addr?.city, addr?.state].filter(Boolean).join(", ");
  const customerName = [job.customerFirstName, job.customerLastName].filter(Boolean).join(" ").trim() || "—";

  return (
    <li className="flex flex-col p-3 bg-slate-50 rounded-lg border border-slate-100">
      <div className="flex items-center justify-between">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-slate-500">
              {JOB_TYPE_LABELS[job.type]}
            </span>
            <span className="text-xs text-slate-400">·</span>
            <span className="text-xs text-slate-500">{job.status}</span>
          </div>
          <p className="text-sm font-medium text-slate-800 truncate">
            {customerName}
            {addr ? ` — ${addr.line1 ?? ""}${cityState ? `, ${cityState}` : ""}` : ""}
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
        {!isEditing && (
          <div className="flex items-center gap-2 shrink-0">
            <Link
              href={`/app/jobs/${job.id}`}
              className="px-3 py-1.5 text-sm font-medium text-sky-600 hover:bg-sky-50 rounded-lg"
            >
              Open
            </Link>
            <button
              type="button"
              onClick={onEdit}
              className="px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg border border-slate-200"
            >
              Edit
            </button>
          </div>
        )}
      </div>

      {isEditing && (
        <form onSubmit={onSave} className="mt-3 pt-3 border-t border-slate-200 space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div>
              <label htmlFor="edit-start-date" className="block text-xs font-medium text-slate-600 mb-1">Start date</label>
              <input
                id="edit-start-date"
                type="date"
                value={editStart}
                onChange={(e) => onEditStartChange(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-2 py-1.5 text-sm"
              />
            </div>
            <div>
              <label htmlFor="edit-end-date" className="block text-xs font-medium text-slate-600 mb-1">End date</label>
              <input
                id="edit-end-date"
                type="date"
                value={editEnd}
                onChange={(e) => onEditEndChange(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-2 py-1.5 text-sm"
              />
            </div>
            <div>
              <label htmlFor="edit-crew" className="block text-xs font-medium text-slate-600 mb-1">Crew</label>
              <input
                id="edit-crew"
                type="text"
                value={editCrew}
                onChange={(e) => onEditCrewChange(e.target.value)}
                placeholder="Crew name"
                className="w-full border border-slate-300 rounded-lg px-2 py-1.5 text-sm"
              />
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="submit"
              disabled={isSaving}
              className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60"
            >
              {isSaving ? "Saving…" : "Save"}
            </button>
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            {saveError && (
              <span className="text-sm text-red-600">{saveError}</span>
            )}
          </div>
        </form>
      )}
    </li>
  );
}
