import type { FormEvent } from "react";
import { WEEKDAY_LABELS, isTodayDateKey, type CalendarDay, type ScheduleRenderItem } from "./scheduleTypes";
import type { JobDto } from "@/lib/types";
import { ScheduleDayCell } from "./ScheduleDayCell";

type ScheduleMonthGridProps = {
  calendarDays: CalendarDay[];
  jobsByDate: Map<string, ScheduleRenderItem[]>;
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
};

export function ScheduleMonthGrid({
  calendarDays,
  jobsByDate,
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
}: ScheduleMonthGridProps) {
  return (
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
          const dayJobs = jobsByDate.get(dateKey) ?? [];
          const todayCell = isTodayDateKey(dateKey);
          return (
            <ScheduleDayCell
              key={dateKey}
              dateKey={dateKey}
              inMonth={inMonth}
              isToday={todayCell}
              jobs={dayJobs}
              editingJobId={editingJobId}
              editStart={editStart}
              editEnd={editEnd}
              editCrew={editCrew}
              onEditStartChange={onEditStartChange}
              onEditEndChange={onEditEndChange}
              onEditCrewChange={onEditCrewChange}
              openEdit={openEdit}
              handleSaveEdit={handleSaveEdit}
              setEditingJobId={setEditingJobId}
              isSaving={isSaving}
              saveError={saveError}
            />
          );
        })}
      </div>
    </div>
  );
}
