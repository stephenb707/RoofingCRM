"use client";

import { useState, useMemo, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  DndContext,
  DragCancelEvent,
  DragEndEvent,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";
import {
  addMonths,
  subMonths,
  startOfMonth,
  isSameMonth,
} from "date-fns";
import { useAuthReady } from "@/lib/AuthContext";
import { listJobSchedule, updateJob } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import { queryKeys } from "@/lib/queryKeys";
import { formatLocalDateInput } from "@/lib/format";
import { computeScheduleUpdate, applyOptimisticSchedulingTagChange } from "@/lib/scheduleDnd";
import type { JobDto } from "@/lib/types";
import { buildCalendarDays } from "@/components/schedule/scheduleTypes";
import { useScheduleJobsByDate } from "@/components/schedule/useScheduleJobsByDate";
import { ScheduleToolbar } from "@/components/schedule/ScheduleToolbar";
import { ScheduleFilters } from "@/components/schedule/ScheduleFilters";
import { ScheduleMonthGrid } from "@/components/schedule/ScheduleMonthGrid";
import { ScheduleUnscheduledSection } from "@/components/schedule/ScheduleUnscheduledSection";
import { ScheduleDragOverlay } from "@/components/schedule/ScheduleDragOverlay";

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

  const jobsByDate = useScheduleJobsByDate(jobs, calendarDayKeys);

  const saveError = updateJobMutation.isError
    ? getApiErrorMessage(updateJobMutation.error, "Failed to update")
    : null;

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

      <ScheduleToolbar
        viewMonth={viewMonth}
        onPrevMonth={goPrevMonth}
        onNextMonth={goNextMonth}
        onThisMonth={goThisMonth}
        rangeFrom={rangeFrom}
        rangeTo={rangeTo}
      />

      <ScheduleFilters
        jobPipelineDefs={jobPipelineDefs}
        statusFilter={statusFilter}
        onStatusFilterChange={setStatusFilter}
        crewFilter={crewFilter}
        onCrewFilterChange={setCrewFilter}
        includeUnscheduled={includeUnscheduled}
        onIncludeUnscheduledChange={setIncludeUnscheduled}
        hasActiveFilters={hasActiveFilters}
        onClearFilters={handleClearFilters}
      />

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
            <ScheduleMonthGrid
              calendarDays={calendarDays}
              jobsByDate={jobsByDate.byDate}
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
              saveError={saveError}
            />

            {includeUnscheduled && (
              <ScheduleUnscheduledSection
                jobs={jobsByDate.unscheduled}
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
                saveError={saveError}
              />
            )}

            {jobs.length === 0 && (
              <p className="text-sm text-slate-500 py-8">
                No jobs in this range.
                {!includeUnscheduled && " Try enabling “Include unscheduled”."}
              </p>
            )}
          </div>

          <ScheduleDragOverlay activeDragJob={activeDragJob} />
        </DndContext>
      )}
    </div>
  );
}
