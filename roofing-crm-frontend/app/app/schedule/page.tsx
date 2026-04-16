"use client";

import { useState, useMemo, useEffect } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  DndContext,
  DragOverlay,
  DragCancelEvent,
  DragEndEvent,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  useDraggable,
  useDroppable,
} from "@dnd-kit/core";
import { CSS } from "@dnd-kit/utilities";
import {
  addMonths,
  subMonths,
  startOfMonth,
  startOfWeek,
  endOfWeek,
  eachDayOfInterval,
  endOfMonth,
  isSameMonth,
  format,
} from "date-fns";
import { useAuthReady } from "@/lib/AuthContext";
import { listJobSchedule, updateJob } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import { queryKeys } from "@/lib/queryKeys";
import {
  formatDate,
  formatLocalDateInput,
  parseLocalDateOnly,
} from "@/lib/format";
import { DatePickerField } from "@/components/DatePickerField";
import { computeScheduleUpdate, applyOptimisticSchedulingTagChange } from "@/lib/scheduleDnd";
import type { JobDto } from "@/lib/types";

const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"] as const;
const MAX_JOBS_VISIBLE_PER_DAY = 3;

type ScheduleRenderItem = {
  job: JobDto;
  isDraggable: boolean;
};

type CalendarDay = {
  dateKey: string;
  inMonth: boolean;
};

/** Full month grid including leading/trailing days (weeks start Sunday). */
function buildCalendarDays(viewMonthStart: Date): CalendarDay[] {
  const monthStart = startOfMonth(viewMonthStart);
  const monthEnd = endOfMonth(monthStart);
  const gridStart = startOfWeek(monthStart, { weekStartsOn: 0 });
  const gridEnd = endOfWeek(monthEnd, { weekStartsOn: 0 });
  const monthIndex = monthStart.getMonth();
  return eachDayOfInterval({ start: gridStart, end: gridEnd }).map((d) => ({
    dateKey: format(d, "yyyy-MM-dd"),
    inMonth: d.getMonth() === monthIndex,
  }));
}

function isTodayDateKey(dateKey: string): boolean {
  return dateKey === formatLocalDateInput(new Date());
}

export default function SchedulePage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const focusJobId = searchParams.get("focusJob");

  const [viewMonth, setViewMonth] = useState(() => startOfMonth(new Date()));
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [crewFilter, setCrewFilter] = useState("");
  const [includeUnscheduled, setIncludeUnscheduled] = useState(true);

  const [editingJobId, setEditingJobId] = useState<string | null>(null);
  const [editStart, setEditStart] = useState("");
  const [editEnd, setEditEnd] = useState("");
  const [editCrew, setEditCrew] = useState("");
  const [activeDragJob, setActiveDragJob] = useState<JobDto | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    })
  );

  const calendarDays = useMemo(() => buildCalendarDays(viewMonth), [viewMonth]);
  const rangeFrom = calendarDays[0]?.dateKey ?? formatLocalDateInput(viewMonth);
  const rangeTo = calendarDays[calendarDays.length - 1]?.dateKey ?? rangeFrom;

  const scheduleKey = queryKeys.jobSchedule(
    auth.selectedTenantId,
    rangeFrom,
    rangeTo,
    statusFilter || null,
    crewFilter || null,
    includeUnscheduled
  );

  const pipelineStatusesKey = queryKeys.pipelineStatuses(auth.selectedTenantId, "JOB");
  const { data: jobPipelineDefs = [] } = useQuery({
    queryKey: pipelineStatusesKey,
    queryFn: () => listPipelineStatuses(api, "JOB"),
    enabled: ready && !!auth.selectedTenantId,
  });

  const { data: jobs = [], isLoading, isError, error } = useQuery({
    queryKey: scheduleKey,
    queryFn: () =>
      listJobSchedule(api, {
        from: rangeFrom,
        to: rangeTo,
        statusDefinitionId: statusFilter || null,
        crewName: crewFilter || undefined,
        includeUnscheduled,
      }),
    enabled: ready && !!auth.selectedTenantId,
  });

  useEffect(() => {
    if (focusJobId) {
      setIncludeUnscheduled(true);
    }
  }, [focusJobId]);

  const calendarDayKeys = useMemo(() => calendarDays.map((d) => d.dateKey), [calendarDays]);
  const jobsSignature = useMemo(() => jobs.map((j) => j.id).sort().join(","), [jobs]);

  useEffect(() => {
    if (!focusJobId || isLoading || jobs.length === 0) return;
    const el = document.querySelector(`[data-testid^="schedule-card-${focusJobId}-"]`);
    el?.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [focusJobId, isLoading, jobs.length, jobsSignature]);

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
    onMutate: async (variables) => {
      await queryClient.cancelQueries({ queryKey: scheduleKey });
      const previous = queryClient.getQueryData<JobDto[]>(scheduleKey);
      const scheduleUpdate = {
        clearSchedule: variables.clearSchedule ?? false,
        scheduledStartDate: variables.scheduledStartDate ?? undefined,
        scheduledEndDate: variables.scheduledEndDate ?? undefined,
      };
      queryClient.setQueryData<JobDto[]>(scheduleKey, (old) => {
        if (!old) return old;
        return old.map((j) => {
          if (j.id !== variables.jobId) return j;
          const updated = applyOptimisticSchedulingTagChange(j, scheduleUpdate, jobPipelineDefs);
          if (variables.crewName !== undefined) {
            updated.crewName = variables.crewName ?? null;
          }
          return updated;
        });
      });
      return { previous };
    },
    onError: (_err, _variables, context) => {
      if (context?.previous != null) {
        queryClient.setQueryData(scheduleKey, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: scheduleKey });
    },
    onSuccess: (_, variables) => {
      setEditingJobId(null);
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

  const handleDragStart = (event: DragStartEvent) => {
    const activeId = String(event.active.id);
    if (!activeId.startsWith("job:")) return;
    const jobId =
      (event.active.data.current?.jobId as string | undefined) ??
      activeId.slice(4).split("@")[0];
    const job = jobs.find((j) => j.id === jobId);
    if (job) setActiveDragJob(job);
  };

  const handleDragCancel = (_event: DragCancelEvent) => {
    setActiveDragJob(null);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveDragJob(null);
    const { active, over } = event;
    if (!over) return;
    const activeId = String(active.id);
    const overId = String(over.id);
    if (!activeId.startsWith("job:") || (!overId.startsWith("date:") && overId !== "unscheduled")) {
      return;
    }
    const activeJobId =
      (active.data.current?.jobId as string | undefined) ??
      activeId.slice(4).split("@")[0];
    const jobId = activeJobId;
    const job = jobs.find((j) => j.id === jobId);
    if (!job) return;

    const target =
      overId === "unscheduled"
        ? { type: "unscheduled" as const }
        : { type: "date" as const, dateKey: overId.slice(5) };
    const update = computeScheduleUpdate(job, target);

    updateJobMutation.mutate({
      jobId,
      scheduledStartDate: update.scheduledStartDate ?? null,
      scheduledEndDate: update.scheduledEndDate ?? null,
      clearSchedule: update.clearSchedule ?? false,
      crewName: job.crewName ?? null,
    });
  };

  const goPrevMonth = () => setViewMonth((prev) => startOfMonth(subMonths(prev, 1)));
  const goNextMonth = () => setViewMonth((prev) => startOfMonth(addMonths(prev, 1)));
  const goThisMonth = () => setViewMonth(startOfMonth(new Date()));

  const hasActiveFilters =
    statusFilter !== "" ||
    crewFilter !== "" ||
    !includeUnscheduled ||
    !isSameMonth(viewMonth, new Date());

  const handleClearFilters = () => {
    setViewMonth(startOfMonth(new Date()));
    setStatusFilter("");
    setCrewFilter("");
    setIncludeUnscheduled(true);
  };

  const jobsByDate = useMemo(() => {
    const map = new Map<string, ScheduleRenderItem[]>();
    for (const d of calendarDayKeys) {
      map.set(d, []);
    }
    const unscheduled: ScheduleRenderItem[] = [];
    for (const job of jobs) {
      const sd = job.scheduledStartDate;
      if (!sd) {
        unscheduled.push({ job, isDraggable: true });
      } else {
        const end = job.scheduledEndDate && job.scheduledEndDate >= sd ? job.scheduledEndDate : sd;
        for (const d of calendarDayKeys) {
          if (d >= sd && d <= end) {
            const list = map.get(d) ?? [];
            list.push({ job, isDraggable: d === sd });
            map.set(d, list);
          }
        }
      }
    }
    for (const [d, list] of map.entries()) {
      const unique = new Map<string, ScheduleRenderItem>();
      for (const item of list) {
        if (!unique.has(item.job.id)) {
          unique.set(item.job.id, item);
        }
      }
      map.set(d, Array.from(unique.values()));
    }
    return { byDate: map, unscheduled };
  }, [jobs, calendarDayKeys]);

  if (!ready) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-4">Schedule</h1>

      {/* Month navigation */}
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={goPrevMonth}
            className="px-3 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
          >
            ← Prev
          </button>
          <h2 className="text-lg font-semibold text-slate-800 min-w-[10rem] text-center">
            {format(viewMonth, "MMMM yyyy")}
          </h2>
          <button
            type="button"
            onClick={goNextMonth}
            className="px-3 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
          >
            Next →
          </button>
          <button
            type="button"
            onClick={goThisMonth}
            className="px-3 py-2 text-sm font-medium text-sky-700 border border-sky-200 bg-sky-50 rounded-lg hover:bg-sky-100"
          >
            This month
          </button>
        </div>
        <div className="flex items-center gap-2 text-sm text-slate-600">
          <span className="hidden sm:inline">Viewing</span>
          <span className="font-medium text-slate-800">
            {format(parseLocalDateOnly(rangeFrom), "MMM d")} –{" "}
            {format(parseLocalDateOnly(rangeTo), "MMM d, yyyy")}
          </span>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-slate-200 p-4 mb-6">
        <div className="flex flex-wrap items-end gap-4">
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
              onChange={(e) => setStatusFilter(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {jobPipelineDefs
                .filter((d) => d.active)
                .map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.label}
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
              type="button"
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
        <DndContext
          sensors={sensors}
          onDragStart={handleDragStart}
          onDragCancel={handleDragCancel}
          onDragEnd={handleDragEnd}
        >
          <div className="space-y-6">
            <div className="bg-white rounded-xl border border-slate-200 p-3 sm:p-4">
              <div className="grid grid-cols-7 gap-1 text-center text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                {WEEKDAY_LABELS.map((d) => (
                  <div key={d} className="py-2">
                    {d}
                  </div>
                ))}
              </div>
              <div className="grid grid-cols-7 gap-1">
                {calendarDays.map(({ dateKey, inMonth }) => {
                  const dayJobs = jobsByDate.byDate.get(dateKey) ?? [];
                  const dayNum = parseLocalDateOnly(dateKey).getDate();
                  const todayCell = isTodayDateKey(dateKey);
                  return (
                    <DayGridCell
                      key={dateKey}
                      dateKey={dateKey}
                      dayNum={dayNum}
                      inMonth={inMonth}
                      isToday={todayCell}
                      jobs={dayJobs}
                      editingJobId={editingJobId}
                      editStart={editStart}
                      editEnd={editEnd}
                      editCrew={editCrew}
                      onEditStartChange={setEditStart}
                      onEditEndChange={setEditEnd}
                      onEditCrewChange={setEditCrew}
                      openEdit={openEdit}
                      handleSaveEdit={handleSaveEdit}
                      setEditingJobId={setEditingJobId}
                      isSaving={updateJobMutation.isPending}
                      saveError={
                        updateJobMutation.isError
                          ? getApiErrorMessage(updateJobMutation.error, "Failed to update")
                          : null
                      }
                    />
                  );
                })}
              </div>
            </div>

            {includeUnscheduled && (
              <ScheduleColumn
                columnId="unscheduled"
                title="Unscheduled"
                dateStr={null}
                jobs={jobsByDate.unscheduled}
                compact={false}
                editingJobId={editingJobId}
                editStart={editStart}
                editEnd={editEnd}
                editCrew={editCrew}
                onEditStartChange={setEditStart}
                onEditEndChange={setEditEnd}
                onEditCrewChange={setEditCrew}
                openEdit={openEdit}
                handleSaveEdit={handleSaveEdit}
                setEditingJobId={setEditingJobId}
                isSaving={updateJobMutation.isPending}
                saveError={
                  updateJobMutation.isError
                    ? getApiErrorMessage(updateJobMutation.error, "Failed to update")
                    : null
                }
              />
            )}

            {jobs.length === 0 && (
              <p className="text-sm text-slate-500 py-8">
                No jobs in this range.
                {!includeUnscheduled && " Try enabling “Include unscheduled”."}
              </p>
            )}
          </div>

          <DragOverlay dropAnimation={{ duration: 180, easing: "ease" }}>
            {activeDragJob ? (
              <div
                className="min-w-[10rem] max-w-[14rem] rounded-lg border-2 border-sky-400 bg-white shadow-2xl opacity-[0.98] cursor-grabbing pointer-events-none"
                data-testid="schedule-drag-overlay-preview"
              >
                <div className="p-2">
                  <ScheduleCardCompactContent job={activeDragJob} />
                </div>
              </div>
            ) : null}
          </DragOverlay>
        </DndContext>
      )}
    </div>
  );
}

function DayGridCell({
  dateKey,
  dayNum,
  inMonth,
  isToday,
  jobs,
  editingJobId,
  editStart,
  editEnd,
  editCrew,
  onEditStartChange,
  onEditEndChange,
  onEditCrewChange,
  openEdit,
  handleSaveEdit,
  setEditingJobId,
  isSaving,
  saveError,
}: {
  dateKey: string;
  dayNum: number;
  inMonth: boolean;
  isToday: boolean;
  jobs: ScheduleRenderItem[];
  editingJobId: string | null;
  editStart: string;
  editEnd: string;
  editCrew: string;
  onEditStartChange: (v: string) => void;
  onEditEndChange: (v: string) => void;
  onEditCrewChange: (v: string) => void;
  openEdit: (job: JobDto) => void;
  handleSaveEdit: (e: React.FormEvent, jobId: string) => void;
  setEditingJobId: (id: string | null) => void;
  isSaving: boolean;
  saveError: string | null;
}) {
  const columnId = `date:${dateKey}`;
  const { setNodeRef, isOver } = useDroppable({ id: columnId });
  const dataTestId = `schedule-col-${dateKey}`;
  const uniqueCount = new Set(jobs.map((item) => item.job.id)).size;
  const visible = jobs.slice(0, MAX_JOBS_VISIBLE_PER_DAY);
  const moreCount = jobs.length - visible.length;

  return (
    <div
      ref={setNodeRef}
      data-testid={dataTestId}
      className={`min-h-[7rem] rounded-lg border p-1.5 flex flex-col transition-colors ${
        isOver ? "border-sky-400 bg-sky-50/60" : "border-slate-200"
      } ${!inMonth ? "bg-slate-50/80 text-slate-400" : "bg-white"} ${
        isToday ? "ring-2 ring-sky-500 ring-offset-1" : ""
      }`}
    >
      <div className="flex items-center justify-between gap-1 mb-1">
        <span
          className={`text-sm font-semibold tabular-nums ${inMonth ? "text-slate-800" : "text-slate-400"}`}
        >
          {dayNum}
        </span>
        <span className="sr-only" data-testid={`${dataTestId}-capacity`}>
          Jobs: {uniqueCount}
        </span>
      </div>
      <ul className="space-y-1 flex-1 min-h-0 overflow-y-auto max-h-[200px]">
        {visible.map((item) => (
          <ScheduleJobCard
            key={`${item.job.id}-${columnId}`}
            job={item.job}
            columnId={columnId}
            isDraggable={item.isDraggable}
            compact
            isEditing={editingJobId === item.job.id && dateKey === item.job.scheduledStartDate}
            editStart={editStart}
            editEnd={editEnd}
            editCrew={editCrew}
            onEditStartChange={onEditStartChange}
            onEditEndChange={onEditEndChange}
            onEditCrewChange={onEditCrewChange}
            onEdit={() => openEdit(item.job)}
            onSave={(e) => handleSaveEdit(e, item.job.id)}
            onCancel={() => setEditingJobId(null)}
            isSaving={isSaving}
            saveError={saveError}
          />
        ))}
      </ul>
      {moreCount > 0 && (
        <p className="text-[10px] text-slate-500 mt-1 text-center font-medium">
          +{moreCount} more
        </p>
      )}
    </div>
  );
}

function ScheduleColumn({
  columnId,
  title,
  dateStr,
  jobs,
  compact,
  editingJobId,
  editStart,
  editEnd,
  editCrew,
  onEditStartChange,
  onEditEndChange,
  onEditCrewChange,
  openEdit,
  handleSaveEdit,
  setEditingJobId,
  isSaving,
  saveError,
}: {
  columnId: string;
  title: string;
  dateStr: string | null;
  jobs: ScheduleRenderItem[];
  compact: boolean;
  editingJobId: string | null;
  editStart: string;
  editEnd: string;
  editCrew: string;
  onEditStartChange: (v: string) => void;
  onEditEndChange: (v: string) => void;
  onEditCrewChange: (v: string) => void;
  openEdit: (job: JobDto) => void;
  handleSaveEdit: (e: React.FormEvent, jobId: string) => void;
  setEditingJobId: (id: string | null) => void;
  isSaving: boolean;
  saveError: string | null;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: columnId });
  const dataTestId =
    columnId === "unscheduled"
      ? "schedule-col-unscheduled"
      : `schedule-col-${columnId.slice(5)}`;

  const uniqueCount = new Set(jobs.map((item) => item.job.id)).size;

  return (
    <section
      ref={setNodeRef}
      data-testid={dataTestId}
      className={`bg-white rounded-xl border p-4 min-h-[120px] transition-colors ${
        isOver ? "border-sky-400 bg-sky-50/50" : "border-slate-200"
      }`}
    >
      <h2 className="text-lg font-semibold text-slate-800 mb-3">{title}</h2>
      <p className="mb-3 text-xs text-slate-500" data-testid={`${dataTestId}-capacity`}>
        Jobs: {uniqueCount}
      </p>
      <ul className="space-y-2">
        {jobs.map((item) => (
          <ScheduleJobCard
            key={`${item.job.id}-${columnId}`}
            job={item.job}
            columnId={columnId}
            isDraggable={item.isDraggable}
            compact={compact}
            isEditing={
              editingJobId === item.job.id && (dateStr == null || dateStr === item.job.scheduledStartDate)
            }
            editStart={editStart}
            editEnd={editEnd}
            editCrew={editCrew}
            onEditStartChange={onEditStartChange}
            onEditEndChange={onEditEndChange}
            onEditCrewChange={onEditCrewChange}
            onEdit={() => openEdit(item.job)}
            onSave={(e) => handleSaveEdit(e, item.job.id)}
            onCancel={() => setEditingJobId(null)}
            isSaving={isSaving}
            saveError={saveError}
          />
        ))}
      </ul>
    </section>
  );
}

function ScheduleJobCard({
  isDraggable,
  compact,
  ...props
}: {
  isDraggable: boolean;
  compact: boolean;
  job: JobDto;
  columnId: string;
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
  return isDraggable ? (
    <DraggableJobCard {...props} compact={compact} />
  ) : (
    <StaticJobCard {...props} compact={compact} />
  );
}

function DraggableJobCard({
  job,
  columnId,
  compact,
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
  columnId: string;
  compact: boolean;
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
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    isDragging,
  } = useDraggable({
    id: `job:${job.id}`,
    data: { jobId: job.id },
    disabled: isEditing,
  });

  const style =
    !isDragging && transform
      ? { transform: CSS.Translate.toString(transform) }
      : undefined;

  return (
    <li
      ref={setNodeRef}
      style={style}
      {...(!isEditing ? { ...listeners, ...attributes } : {})}
      data-testid={`schedule-card-${job.id}-${columnId.replace(":", "-")}`}
      className={`flex flex-col rounded-lg border transition-shadow ${
        isDragging
          ? "opacity-45 border-sky-200/90 bg-slate-100/90 ring-1 ring-sky-200 scale-[0.98]"
          : "bg-slate-50 border-slate-100"
      } ${
        !isEditing
          ? "cursor-grab active:cursor-grabbing touch-manipulation"
          : ""
      }`}
    >
      <JobScheduleCard
        job={job}
        compact={compact}
        isEditing={isEditing}
        editStart={editStart}
        editEnd={editEnd}
        editCrew={editCrew}
        onEditStartChange={onEditStartChange}
        onEditEndChange={onEditEndChange}
        onEditCrewChange={onEditCrewChange}
        onEdit={onEdit}
        onSave={onSave}
        onCancel={onCancel}
        isSaving={isSaving}
        saveError={saveError}
      />
    </li>
  );
}

function StaticJobCard({
  job,
  columnId,
  compact,
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
  columnId: string;
  compact: boolean;
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
  return (
    <li
      data-testid={`schedule-card-${job.id}-${columnId.replace(":", "-")}`}
      className="flex flex-col rounded-lg border bg-slate-50 border-slate-100"
    >
      <JobScheduleCard
        job={job}
        compact={compact}
        isEditing={isEditing}
        editStart={editStart}
        editEnd={editEnd}
        editCrew={editCrew}
        onEditStartChange={onEditStartChange}
        onEditEndChange={onEditEndChange}
        onEditCrewChange={onEditCrewChange}
        onEdit={onEdit}
        onSave={onSave}
        onCancel={onCancel}
        isSaving={isSaving}
        saveError={saveError}
      />
    </li>
  );
}

/** Compact card body used in day cells and in the drag overlay preview. */
function ScheduleCardCompactContent({ job }: { job: JobDto }) {
  const addr = job.propertyAddress;
  const customerName =
    [job.customerFirstName, job.customerLastName].filter(Boolean).join(" ").trim() || "—";

  return (
    <div className="min-w-0 flex-1">
      <div className="flex items-center gap-1 flex-wrap text-[10px]">
        <span className="font-medium text-slate-500">{JOB_TYPE_LABELS[job.type]}</span>
        <span className="text-slate-400">·</span>
        <span className="text-slate-500">{job.statusLabel}</span>
      </div>
      <p className="font-medium text-slate-800 truncate text-xs">
        {customerName}
        {addr?.line1 ? ` · ${addr.line1}` : ""}
      </p>
      {job.crewName ? (
        <p className="text-[10px] text-slate-500 truncate mt-0.5">{job.crewName}</p>
      ) : null}
    </div>
  );
}

function JobScheduleCard({
  job,
  compact,
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
  compact: boolean;
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

  const padCls = compact ? "p-2" : "p-3";
  const titleCls = compact ? "text-xs" : "text-sm";

  return (
    <div className={`flex flex-col ${padCls}`}>
      <div className={`flex items-start justify-between gap-1 ${compact ? "" : "gap-2"}`}>
        {compact ? (
          <ScheduleCardCompactContent job={job} />
        ) : (
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1 flex-wrap text-xs">
              <span className="font-medium text-slate-500">{JOB_TYPE_LABELS[job.type]}</span>
              <span className="text-slate-400">·</span>
              <span className="text-slate-500">{job.statusLabel}</span>
            </div>
            <p className={`font-medium text-slate-800 truncate ${titleCls}`}>
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
              {job.crewName && <span className="font-medium">{job.crewName}</span>}
            </div>
          </div>
        )}
        {!isEditing && (
          <div className={`flex items-center shrink-0 ${compact ? "flex-col gap-0.5" : "gap-2"}`}>
            <Link
              href={`/app/jobs/${job.id}`}
              onPointerDown={(e) => e.stopPropagation()}
              className={`font-medium text-sky-600 hover:bg-sky-50 rounded ${
                compact ? "px-1.5 py-0.5 text-[10px]" : "px-3 py-1.5 text-sm"
              }`}
            >
              Open
            </Link>
            <button
              type="button"
              onClick={onEdit}
              onPointerDown={(e) => e.stopPropagation()}
              className={`font-medium text-slate-700 hover:bg-slate-100 rounded border border-slate-200 ${
                compact ? "px-1.5 py-0.5 text-[10px]" : "px-3 py-1.5 text-sm"
              }`}
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
              <DatePickerField
                id="edit-start-date"
                label="Start date"
                value={editStart}
                onChange={onEditStartChange}
              />
            </div>
            <div>
              <DatePickerField
                id="edit-end-date"
                label="End date"
                value={editEnd}
                onChange={onEditEndChange}
              />
            </div>
            <div>
              <label htmlFor="edit-crew" className="block text-xs font-medium text-slate-600 mb-1">
                Crew
              </label>
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
          <div className="flex items-center gap-2 flex-wrap">
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
            {saveError && <span className="text-sm text-red-600">{saveError}</span>}
          </div>
        </form>
      )}
    </div>
  );
}
