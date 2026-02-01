import type { JobStatus, JobType } from "./types";

export const JOB_STATUSES: JobStatus[] = [
  "UNSCHEDULED",
  "SCHEDULED",
  "IN_PROGRESS",
  "COMPLETED",
  "INVOICED",
];

export const JOB_STATUS_LABELS: Record<JobStatus, string> = {
  UNSCHEDULED: "Unscheduled",
  SCHEDULED: "Scheduled",
  IN_PROGRESS: "In Progress",
  COMPLETED: "Completed",
  INVOICED: "Invoiced",
};

export const JOB_STATUS_COLORS: Record<JobStatus, string> = {
  UNSCHEDULED: "bg-slate-100 text-slate-600 border-slate-200",
  SCHEDULED: "bg-blue-100 text-blue-700 border-blue-200",
  IN_PROGRESS: "bg-amber-100 text-amber-700 border-amber-200",
  COMPLETED: "bg-green-100 text-green-700 border-green-200",
  INVOICED: "bg-slate-100 text-slate-700 border-slate-200",
};

export const JOB_TYPES: JobType[] = ["REPLACEMENT", "REPAIR", "INSPECTION_ONLY"];

export const JOB_TYPE_LABELS: Record<JobType, string> = {
  REPLACEMENT: "Replacement",
  REPAIR: "Repair",
  INSPECTION_ONLY: "Inspection Only",
};
