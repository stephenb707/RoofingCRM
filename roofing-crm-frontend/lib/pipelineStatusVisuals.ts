/**
 * Tailwind badge classes keyed by stable {@link systemKey} (built-in keys or custom prefix).
 * Labels come from the API; colors stay frontend-mapped for consistency.
 */

const LEAD_KEY_COLORS: Record<string, string> = {
  NEW: "bg-blue-100 text-blue-700 border-blue-200",
  CONTACTED: "bg-amber-100 text-amber-700 border-amber-200",
  INSPECTION_SCHEDULED: "bg-purple-100 text-purple-700 border-purple-200",
  QUOTE_SENT: "bg-sky-100 text-sky-700 border-sky-200",
  WON: "bg-green-100 text-green-700 border-green-200",
  LOST: "bg-red-100 text-red-700 border-red-200",
};

const JOB_KEY_COLORS: Record<string, string> = {
  UNSCHEDULED: "bg-slate-100 text-slate-600 border-slate-200",
  SCHEDULED: "bg-blue-100 text-blue-700 border-blue-200",
  IN_PROGRESS: "bg-amber-100 text-amber-700 border-amber-200",
  COMPLETED: "bg-green-100 text-green-700 border-green-200",
  INVOICED: "bg-slate-100 text-slate-700 border-slate-200",
};

const FALLBACK = "bg-slate-100 text-slate-700 border-slate-200";

export function leadStatusBadgeClass(systemKey: string): string {
  return LEAD_KEY_COLORS[systemKey] ?? FALLBACK;
}

export function jobStatusBadgeClass(systemKey: string): string {
  return JOB_KEY_COLORS[systemKey] ?? FALLBACK;
}
