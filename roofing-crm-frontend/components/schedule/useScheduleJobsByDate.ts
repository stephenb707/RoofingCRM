import { useMemo } from "react";
import type { JobDto } from "@/lib/types";
import type { ScheduleRenderItem } from "./scheduleTypes";

export function useScheduleJobsByDate(jobs: JobDto[], calendarDayKeys: string[]) {
  return useMemo(() => {
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
}
