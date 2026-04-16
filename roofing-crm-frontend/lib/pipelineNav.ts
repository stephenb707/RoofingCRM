import type { AppPreferencesDto } from "./types";

export type PipelineViewId = "leads" | "jobs" | "combined";

export const PIPELINE_PATH_LEADS = "/app/leads/pipeline";
export const PIPELINE_PATH_JOBS = "/app/jobs/pipeline";
export const PIPELINE_PATH_COMBINED = "/app/pipeline/combined";
/** Hub: redirects to the tenant default pipeline view from preferences. */
export const PIPELINE_PATH_HUB = "/app/pipeline";

const VALID: readonly PipelineViewId[] = ["leads", "jobs", "combined"];

export function parsePipelineDefaultView(raw: unknown): PipelineViewId | null {
  if (raw === "leads" || raw === "jobs" || raw === "combined") {
    return raw;
  }
  return null;
}

/** Path to open for the generic pipeline hub when preferences are loaded. */
export function resolvePipelineHubTarget(
  prefs: AppPreferencesDto | null | undefined
): string {
  const v = parsePipelineDefaultView(prefs?.pipeline?.defaultView);
  if (v === "jobs") return PIPELINE_PATH_JOBS;
  if (v === "combined") return PIPELINE_PATH_COMBINED;
  if (v === "leads") return PIPELINE_PATH_LEADS;
  return PIPELINE_PATH_LEADS;
}

export function pipelineViewFromPathname(pathname: string): PipelineViewId | null {
  if (pathname.startsWith("/app/pipeline/combined")) return "combined";
  if (pathname.startsWith("/app/leads/pipeline")) return "leads";
  if (pathname.startsWith("/app/jobs/pipeline")) return "jobs";
  return null;
}

export function isValidPipelineViewId(v: string): v is PipelineViewId {
  return (VALID as readonly string[]).includes(v);
}
