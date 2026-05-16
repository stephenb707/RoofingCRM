import {
  startOfMonth,
  startOfWeek,
  endOfWeek,
  eachDayOfInterval,
  endOfMonth,
  format,
} from "date-fns";
import { formatLocalDateInput } from "@/lib/format";
import type { JobDto } from "@/lib/types";

export const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"] as const;

export const MAX_JOBS_VISIBLE_PER_DAY = 3;

export type ScheduleRenderItem = {
  job: JobDto;
  isDraggable: boolean;
};

export type CalendarDay = {
  dateKey: string;
  inMonth: boolean;
};

/** Full month grid including leading/trailing days (weeks start Sunday). */
export function buildCalendarDays(viewMonthStart: Date): CalendarDay[] {
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

export function isTodayDateKey(dateKey: string): boolean {
  return dateKey === formatLocalDateInput(new Date());
}
