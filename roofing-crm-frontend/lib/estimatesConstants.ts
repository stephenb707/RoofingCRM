import type { EstimateStatus } from "./types";

export const ESTIMATE_STATUSES: EstimateStatus[] = [
  "DRAFT",
  "SENT",
  "ACCEPTED",
  "REJECTED",
];

export const ESTIMATE_STATUS_LABELS: Record<EstimateStatus, string> = {
  DRAFT: "Draft",
  SENT: "Sent",
  ACCEPTED: "Accepted",
  REJECTED: "Rejected",
};

export const ESTIMATE_STATUS_COLORS: Record<EstimateStatus, string> = {
  DRAFT: "bg-slate-100 text-slate-700 border-slate-200",
  SENT: "bg-blue-100 text-blue-700 border-blue-200",
  ACCEPTED: "bg-green-100 text-green-700 border-green-200",
  REJECTED: "bg-red-100 text-red-700 border-red-200",
};
