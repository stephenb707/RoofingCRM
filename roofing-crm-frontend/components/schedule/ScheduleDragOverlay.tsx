import { DragOverlay } from "@dnd-kit/core";
import type { JobDto } from "@/lib/types";
import { ScheduleCardCompactContent } from "./ScheduleJobCard";

type ScheduleDragOverlayProps = {
  activeDragJob: JobDto | null;
};

export function ScheduleDragOverlay({ activeDragJob }: ScheduleDragOverlayProps) {
  return (
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
  );
}
