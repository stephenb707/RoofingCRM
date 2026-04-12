import type { QueryClient } from "@tanstack/react-query";

/**
 * After pipeline status definitions change (settings), refresh read models that depend on them.
 */
export function invalidatePipelineRelatedQueries(
  queryClient: QueryClient,
  tenantId: string | null
): void {
  if (!tenantId) return;
  queryClient.invalidateQueries({ queryKey: ["pipelineStatuses", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["settingsPipelineStatuses", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["leadsPipeline", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["jobsPipeline", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["dashboardSummary", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["jobSchedule", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["scheduleJobs", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["leads", tenantId] });
  queryClient.invalidateQueries({ queryKey: ["jobs", tenantId] });
}
