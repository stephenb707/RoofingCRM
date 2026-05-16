import type { FormEvent } from "react";
import Link from "next/link";
import { useDraggable } from "@dnd-kit/core";
import { CSS } from "@dnd-kit/utilities";
import { JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { formatDate } from "@/lib/format";
import { DatePickerField } from "@/components/DatePickerField";
import type { JobDto } from "@/lib/types";

export function ScheduleCardCompactContent({ job }: { job: JobDto }) {
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
  onSave: (e: FormEvent) => void;
  onCancel: () => void;
  isSaving: boolean;
  saveError: string | null;
}) {
  const addr = job.propertyAddress;
  const cityState = [addr?.city, addr?.state].filter(Boolean).join(", ");
  const customerName =
    [job.customerFirstName, job.customerLastName].filter(Boolean).join(" ").trim() || "—";

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
          <div
            className={`flex items-center shrink-0 ${compact ? "flex-col gap-0.5" : "gap-2"}`}
          >
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
  onSave: (e: FormEvent) => void;
  onCancel: () => void;
  isSaving: boolean;
  saveError: string | null;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `job:${job.id}`,
    data: { jobId: job.id },
    disabled: isEditing,
  });

  const style =
    !isDragging && transform ? { transform: CSS.Translate.toString(transform) } : undefined;

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
        !isEditing ? "cursor-grab active:cursor-grabbing touch-manipulation" : ""
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
  onSave: (e: FormEvent) => void;
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

export function ScheduleJobCard({
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
  onSave: (e: FormEvent) => void;
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
