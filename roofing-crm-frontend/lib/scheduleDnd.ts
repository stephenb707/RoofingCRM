import { parseISO, format, addDays, differenceInCalendarDays } from "date-fns";
import type { JobDto } from "./types";
import type { UpdateJobRequest } from "./types";
import type { PipelineStatusDefinitionDto } from "./pipelineStatusesApi";

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

function findDef(
  defs: PipelineStatusDefinitionDto[],
  systemKey: string
): PipelineStatusDefinitionDto | undefined {
  return defs.find((d) => d.systemKey === systemKey);
}

/**
 * Apply optimistic scheduling tag/status change for a job.
 * Mirrors backend normalization: scheduled → SCHEDULED, unscheduled → UNSCHEDULED
 * when the job was only in one of those scheduling buckets.
 */
export function applyOptimisticSchedulingTagChange(
  job: JobDto,
  scheduleUpdate: UpdateJobRequest,
  jobPipelineDefs: PipelineStatusDefinitionDto[]
): JobDto {
  const clearSchedule = scheduleUpdate.clearSchedule === true;
  const scheduledDef = findDef(jobPipelineDefs, "SCHEDULED");
  const unscheduledDef = findDef(jobPipelineDefs, "UNSCHEDULED");

  const wasSchedulingOnly =
    job.statusKey === "SCHEDULED" || job.statusKey === "UNSCHEDULED";

  let nextDefId = job.statusDefinitionId;
  let nextKey = job.statusKey;
  let nextLabel = job.statusLabel;

  if (wasSchedulingOnly && scheduledDef && unscheduledDef) {
    if (clearSchedule) {
      nextDefId = unscheduledDef.id;
      nextKey = unscheduledDef.systemKey;
      nextLabel = unscheduledDef.label;
    } else {
      nextDefId = scheduledDef.id;
      nextKey = scheduledDef.systemKey;
      nextLabel = scheduledDef.label;
    }
  }

  return {
    ...job,
    statusDefinitionId: nextDefId,
    statusKey: nextKey,
    statusLabel: nextLabel,
    scheduledStartDate: clearSchedule ? null : scheduleUpdate.scheduledStartDate ?? null,
    scheduledEndDate: clearSchedule ? null : scheduleUpdate.scheduledEndDate ?? null,
  };
}
