import type { JobCostCategory } from "./types";

export const JOB_COST_CATEGORY_LABELS: Record<JobCostCategory, string> = {
  MATERIAL: "Materials",
  TRANSPORTATION: "Transportation",
  LABOR: "Labor",
  OTHER: "Other",
};

export const JOB_COST_CATEGORIES: JobCostCategory[] = [
  "MATERIAL",
  "TRANSPORTATION",
  "LABOR",
  "OTHER",
];
