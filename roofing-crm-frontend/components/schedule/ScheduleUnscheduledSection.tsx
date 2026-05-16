import type { FormEvent } from "react";
import { useDroppable } from "@dnd-kit/core";
import type { JobDto } from "@/lib/types";
import type { ScheduleRenderItem } from "./scheduleTypes";
import { ScheduleJobCard } from "./ScheduleJobCard";

export function ScheduleUnscheduledSection({
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
  const columnId = "unscheduled";
  const { setNodeRef, isOver } = useDroppable({ id: columnId });
  const dataTestId = "schedule-col-unscheduled";

  const uniqueCount = new Set(jobs.map((item) => item.job.id)).size;

  return (
    <section
      ref={setNodeRef}
      data-testid={dataTestId}
      className={`bg-white rounded-xl border p-4 min-h-[120px] transition-colors ${
        isOver ? "border-sky-400 bg-sky-50/50" : "border-slate-200"
      }`}
    >
      <h2 className="text-lg font-semibold text-slate-800 mb-3">Unscheduled</h2>
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
            compact={false}
            isEditing={editingJobId === item.job.id}
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
