import type { FormEvent } from "react";
import { useDroppable } from "@dnd-kit/core";
import { parseLocalDateOnly } from "@/lib/format";
import type { JobDto } from "@/lib/types";
import { MAX_JOBS_VISIBLE_PER_DAY, type ScheduleRenderItem } from "./scheduleTypes";
import { ScheduleJobCard } from "./ScheduleJobCard";

export function ScheduleDayCell({
  dateKey,
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
  handleSaveEdit: (e: FormEvent, jobId: string) => void;
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
  const dayNum = parseLocalDateOnly(dateKey).getDate();

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
