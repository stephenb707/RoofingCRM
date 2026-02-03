import { parseISO, format, addDays, differenceInCalendarDays } from "date-fns";
import type { JobDto, JobStatus } from "./types";
import type { UpdateJobRequest } from "./types";

export type ScheduleDropTarget =
  | { type: "unscheduled" }
  | { type: "date"; dateKey: string };

/**
 * Compute the UpdateJobRequest for a job dropped onto a schedule target.
 */
export function computeScheduleUpdate(
  job: JobDto,
  dropTarget: ScheduleDropTarget
): UpdateJobRequest {
  if (dropTarget.type === "unscheduled") {
    return { clearSchedule: true };
  }

  const dateKey = dropTarget.dateKey;
  const newStartDate = parseISO(dateKey);

  let durationDays = 0;
  const start = job.scheduledStartDate ? parseISO(job.scheduledStartDate) : null;
  const end = job.scheduledEndDate ? parseISO(job.scheduledEndDate) : null;
  if (start && end) {
    durationDays = differenceInCalendarDays(end, start);
  }

  const newEndDate = addDays(newStartDate, durationDays);
  return {
    scheduledStartDate: format(newStartDate, "yyyy-MM-dd"),
    scheduledEndDate: format(newEndDate, "yyyy-MM-dd"),
    clearSchedule: false,
  };
}

/**
 * Apply optimistic scheduling tag/status change for a job.
 * Mirrors backend normalization: scheduled → SCHEDULED, unscheduled → UNSCHEDULED.
 */
export function applyOptimisticSchedulingTagChange(
  job: JobDto,
  scheduleUpdate: UpdateJobRequest
): JobDto {
  const clearSchedule = scheduleUpdate.clearSchedule === true;
  const newStatus: JobStatus = clearSchedule ? "UNSCHEDULED" : "SCHEDULED";
  return {
    ...job,
    status: newStatus,
    scheduledStartDate: clearSchedule ? null : scheduleUpdate.scheduledStartDate ?? null,
    scheduledEndDate: clearSchedule ? null : scheduleUpdate.scheduledEndDate ?? null,
  };
}
