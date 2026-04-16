import type { JobType } from "./types";

export const JOB_TYPES: JobType[] = ["REPLACEMENT", "REPAIR", "INSPECTION_ONLY"];

export const JOB_TYPE_LABELS: Record<JobType, string> = {
  REPLACEMENT: "Replacement",
  REPAIR: "Repair",
  INSPECTION_ONLY: "Inspection Only",
};
