"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getAppPreferences } from "@/lib/preferencesApi";
import { queryKeys } from "@/lib/queryKeys";
import { resolvePipelineHubTarget } from "@/lib/pipelineNav";

/**
 * Generic pipeline entry: redirects to the tenant default view from preferences
 * (leads-only, jobs-only, or combined). Direct links to /app/leads/pipeline etc. are unchanged.
 */
export default function PipelineHubPage() {
  const { api, auth, ready } = useAuthReady();
  const router = useRouter();
  const { data, isSuccess, isError } = useQuery({
    queryKey: queryKeys.appPreferences(auth.selectedTenantId),
    queryFn: () => getAppPreferences(api),
    enabled: ready,
  });

  useEffect(() => {
    if (!ready || !isSuccess) return;
    router.replace(resolvePipelineHubTarget(data));
  }, [ready, isSuccess, data, router]);

  useEffect(() => {
    if (!ready || !isError) return;
    router.replace(resolvePipelineHubTarget(null));
  }, [ready, isError, router]);

  return (
    <div className="max-w-7xl mx-auto flex flex-col items-center justify-center py-16" data-testid="pipeline-hub-loading">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mb-4" />
      <p className="text-sm text-slate-500">Opening pipeline…</p>
    </div>
  );
}
