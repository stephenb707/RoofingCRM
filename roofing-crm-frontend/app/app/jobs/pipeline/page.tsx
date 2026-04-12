"use client";

import { useMemo, useState, useEffect } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  useDraggable,
  useDroppable,
} from "@dnd-kit/core";
import { useAuthReady } from "@/lib/AuthContext";
import { listJobs, updateJobStatus } from "@/lib/jobsApi";
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import type { PipelineStatusDefinitionDto } from "@/lib/pipelineStatusesApi";
import { jobStatusBadgeClass } from "@/lib/pipelineStatusVisuals";
import { getApiErrorMessage } from "@/lib/apiError";
import { JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatAddress, formatDate, formatPhone } from "@/lib/format";
import type { JobDto } from "@/lib/types";

function customerName(job: JobDto): string {
  const parts = [job.customerFirstName, job.customerLastName].filter(Boolean);
  return parts.length > 0 ? parts.join(" ") : "—";
}

function sortJobsInColumn(jobs: JobDto[]): JobDto[] {
  return [...jobs].sort((a, b) => (b.updatedAt ?? "").localeCompare(a.updatedAt ?? ""));
}

function sortActiveJobColumns(defs: PipelineStatusDefinitionDto[]): PipelineStatusDefinitionDto[] {
  return [...defs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder);
}

function jobMatchesSearch(job: JobDto, q: string): boolean {
  if (!q.trim()) return true;
  const needle = q.trim().toLowerCase();
  const phoneDigits = job.customerPhone?.replace(/\D/g, "") ?? "";
  const parts = [
    customerName(job),
    formatAddress(job.propertyAddress),
    JOB_TYPE_LABELS[job.type],
    job.crewName ?? "",
    job.scheduledStartDate ?? "",
    job.scheduledEndDate ?? "",
    job.customerPhone ?? "",
    phoneDigits,
    job.statusLabel ?? "",
  ];
  return parts.some((p) => p.toLowerCase().includes(needle));
}

export default function JobsPipelinePage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const searchParams = useSearchParams();
  const customerIdFromQuery = searchParams.get("customerId");
  const [activeJob, setActiveJob] = useState<JobDto | null>(null);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  const canEditPipeline =
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "OWNER" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "ADMIN" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "SALES";

  const pipelineKey = queryKeys.jobsPipeline(auth.selectedTenantId, customerIdFromQuery);
  const pipelineDefsKey = queryKeys.pipelineStatuses(auth.selectedTenantId, "JOB");

  const { data: jobDefs = [], isLoading: defsLoading } = useQuery({
    queryKey: pipelineDefsKey,
    queryFn: () => listPipelineStatuses(api, "JOB"),
    enabled: ready,
  });

  const columnDefs = useMemo(() => sortActiveJobColumns(jobDefs), [jobDefs]);

  const defById = useMemo(() => {
    const m = new Map<string, PipelineStatusDefinitionDto>();
    for (const d of jobDefs) m.set(d.id, d);
    return m;
  }, [jobDefs]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: pipelineKey,
    queryFn: () =>
      listJobs(api, {
        page: 0,
        size: 200,
        customerId: customerIdFromQuery || null,
      }),
    enabled: ready,
  });

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    })
  );

  const updateStatusMutation = useMutation({
    mutationFn: ({
      jobId,
      statusDefinitionId,
    }: {
      jobId: string;
      statusDefinitionId: string;
    }) => updateJobStatus(api, jobId, statusDefinitionId),
    onMutate: async ({ jobId, statusDefinitionId }) => {
      await queryClient.cancelQueries({ queryKey: pipelineKey });
      const previous = queryClient.getQueryData<typeof data>(pipelineKey);
      const targetDef = defById.get(statusDefinitionId);
      queryClient.setQueryData(pipelineKey, (old: typeof data) => {
        if (!old || !targetDef) return old;
        const newContent = old.content.map((j) =>
          j.id === jobId
            ? {
                ...j,
                statusDefinitionId,
                statusKey: targetDef.systemKey,
                statusLabel: targetDef.label,
              }
            : j
        );
        return { ...old, content: newContent };
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(pipelineKey, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: pipelineKey });
      queryClient.invalidateQueries({
        queryKey: ["jobs", auth.selectedTenantId],
        exact: false,
      });
    },
  });

  const filteredContent = useMemo(() => {
    const jobs = data?.content ?? [];
    return jobs.filter((j) => jobMatchesSearch(j, debouncedSearch));
  }, [data?.content, debouncedSearch]);

  const jobsByDefinitionId = useMemo(() => {
    const byId = new Map<string, JobDto[]>();
    for (const d of columnDefs) {
      byId.set(d.id, []);
    }
    for (const job of filteredContent) {
      const list = byId.get(job.statusDefinitionId) ?? [];
      list.push(job);
      byId.set(job.statusDefinitionId, list);
    }
    for (const d of columnDefs) {
      byId.set(d.id, sortJobsInColumn(byId.get(d.id) ?? []));
    }
    return byId;
  }, [filteredContent, columnDefs]);

  const handleStatusChange = (jobId: string, statusDefinitionId: string) => {
    updateStatusMutation.mutate({ jobId, statusDefinitionId });
  };

  const handleDragStart = (event: DragStartEvent) => {
    const jobId = String(event.active.id).replace("job-", "");
    const job = (data?.content ?? []).find((j) => j.id === jobId);
    if (job) setActiveJob(job);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveJob(null);
    const { active, over } = event;
    if (!over || !canEditPipeline) return;
    const jobId = String(active.id).replace("job-", "");
    const job = (data?.content ?? []).find((j) => j.id === jobId);
    if (!job) return;

    const overId = String(over.id);
    let newDefId: string;

    const colIds = new Set(columnDefs.map((c) => c.id));
    if (colIds.has(overId)) {
      newDefId = overId;
    } else if (overId.startsWith("job-")) {
      const overJobId = overId.replace("job-", "");
      const overJob = (data?.content ?? []).find((j) => j.id === overJobId);
      if (!overJob) return;
      newDefId = overJob.statusDefinitionId;
    } else {
      return;
    }

    if (job.statusDefinitionId === newDefId) return;

    handleStatusChange(jobId, newDefId);
  };

  if (defsLoading || isLoading) {
    return (
      <div className="max-w-7xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading pipeline…</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-7xl mx-auto">
        <Link
          href="/app/jobs"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Jobs
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load pipeline</h3>
          <p className="text-sm text-red-600 mt-1">
            {getApiErrorMessage(error, "An error occurred. Please try again.")}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between mb-6">
        <div>
          <Link
            href={customerIdFromQuery ? `/app/jobs?customerId=${encodeURIComponent(customerIdFromQuery)}` : "/app/jobs"}
            className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Jobs
          </Link>
          <h1 className="text-2xl font-bold text-slate-800">Pipeline</h1>
          <p className="text-sm text-slate-500 mt-1">
            Jobs by workflow stage—from unscheduled through invoiced
            {!canEditPipeline && (
              <span className="ml-2 text-amber-600 font-medium">(Read-only)</span>
            )}
          </p>
          <p className="text-xs text-slate-500 mt-2">
            Tip: Drag cards to change status. For dates and details,{" "}
            <Link href="/app/jobs" className="text-sky-600 hover:text-sky-700 underline">
              open a job
            </Link>
            .
          </p>
          {customerIdFromQuery && (
            <p className="text-xs text-sky-700 mt-2">
              Filtered to one customer.{" "}
              <Link href="/app/jobs/pipeline" className="underline font-medium">
                Show all jobs
              </Link>
            </p>
          )}
        </div>
        <div className="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-3">
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search customer, address, type…"
            className="border border-slate-300 rounded-lg px-3 py-2 text-sm w-full sm:w-64 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            aria-label="Search jobs in pipeline"
          />
          <Link
            href={customerIdFromQuery ? `/app/jobs?customerId=${encodeURIComponent(customerIdFromQuery)}` : "/app/jobs"}
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors text-center"
          >
            Back to Jobs
          </Link>
          <Link
            href="/app/jobs/new"
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors text-center"
          >
            + New Job
          </Link>
        </div>
      </div>

      <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div className="flex overflow-x-auto pb-4 gap-3">
          {columnDefs.map((def) => (
            <JobPipelineColumn
              key={def.id}
              definition={def}
              jobs={jobsByDefinitionId.get(def.id) ?? []}
              canEdit={canEditPipeline}
            />
          ))}
        </div>

        <DragOverlay>
          {activeJob ? (
            <div className="bg-white rounded-lg border-2 border-sky-400 shadow-xl opacity-95 p-3 w-52">
              <div className="font-medium text-slate-800 text-sm truncate">{customerName(activeJob)}</div>
              <div className="text-xs text-slate-600 truncate">
                {JOB_TYPE_LABELS[activeJob.type]}
                {activeJob.propertyAddress?.line1 ? ` · ${activeJob.propertyAddress.line1}` : ""}
              </div>
              {activeJob.customerPhone?.trim() ? (
                <div className="text-xs text-slate-500 mt-0.5 truncate">
                  {formatPhone(activeJob.customerPhone)}
                </div>
              ) : null}
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
}

function JobPipelineColumn({
  definition,
  jobs,
  canEdit,
}: {
  definition: PipelineStatusDefinitionDto;
  jobs: JobDto[];
  canEdit: boolean;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: definition.id });

  return (
    <div
      ref={setNodeRef}
      className={`flex-shrink-0 w-56 bg-slate-50 rounded-xl border-2 overflow-hidden transition-colors ${
        isOver ? "border-sky-400 bg-sky-50/50" : "border-slate-200"
      }`}
      data-testid={`job-pipeline-col-${definition.id}`}
    >
      <div className={`border-b border-slate-200 ${jobStatusBadgeClass(definition.systemKey)} rounded-t-xl p-3`}>
        <h2 className="font-semibold text-slate-800 text-sm">{definition.label}</h2>
        <p className="text-xs text-slate-600 mt-0.5">{jobs.length} jobs</p>
      </div>
      <div className="max-h-[calc(100vh-280px)] overflow-y-auto p-2 space-y-2">
        {jobs.length === 0 && (
          <p className="text-xs text-slate-400 text-center py-6 px-2">No jobs in this stage</p>
        )}
        {jobs.map((job) => (
          <JobPipelineCard key={job.id} job={job} canEdit={canEdit} />
        ))}
      </div>
    </div>
  );
}

function JobPipelineCard({ job, canEdit }: { job: JobDto; canEdit: boolean }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `job-${job.id}`,
    data: { job },
    disabled: !canEdit,
  });

  const scheduleLabel =
    job.scheduledStartDate != null
      ? job.scheduledEndDate != null && job.scheduledEndDate !== job.scheduledStartDate
        ? `${formatDate(job.scheduledStartDate)} – ${formatDate(job.scheduledEndDate)}`
        : formatDate(job.scheduledStartDate)
      : null;

  return (
    <div
      ref={setNodeRef}
      {...(canEdit ? { ...listeners, ...attributes } : {})}
      className={`bg-white rounded-lg border border-slate-200 shadow-sm p-3 ${
        canEdit ? "cursor-grab active:cursor-grabbing touch-manipulation" : ""
      } ${isDragging ? "opacity-50" : ""}`}
      data-testid={`job-pipeline-card-${job.id}`}
    >
      <div className="font-medium text-slate-800 truncate text-sm">{customerName(job)}</div>
      <div className="text-xs text-slate-600 mt-1 truncate" title={formatAddress(job.propertyAddress)}>
        {formatAddress(job.propertyAddress)}
      </div>
      {job.customerPhone?.trim() ? (
        <div className="text-xs text-slate-500 mt-0.5 truncate" title={formatPhone(job.customerPhone)}>
          {formatPhone(job.customerPhone)}
        </div>
      ) : null}
      <div className="text-xs text-slate-500 mt-0.5">{JOB_TYPE_LABELS[job.type]}</div>
      {scheduleLabel && (
        <div className="text-xs text-slate-500 mt-0.5">Scheduled: {scheduleLabel}</div>
      )}
      {job.crewName && (
        <div className="text-xs text-slate-500 mt-0.5 truncate" title={job.crewName}>
          Crew: {job.crewName}
        </div>
      )}
      <div className="flex items-center justify-between gap-2 mt-2">
        <span
          className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${jobStatusBadgeClass(job.statusKey)}`}
          data-testid={`job-pipeline-card-status-${job.id}`}
        >
          {job.statusLabel}
        </span>
        <Link
          href={`/app/jobs/${job.id}`}
          onPointerDown={(e) => e.stopPropagation()}
          className="text-xs font-medium text-sky-600 hover:text-sky-700 px-2 py-1 shrink-0 relative z-10"
        >
          Open
        </Link>
      </div>
    </div>
  );
}
