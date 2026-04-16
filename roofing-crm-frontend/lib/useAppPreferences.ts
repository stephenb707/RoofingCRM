import { useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "./AuthContext";
import { queryKeys } from "./queryKeys";
import { getAppPreferences, updateAppPreferences } from "./preferencesApi";

/**
 * Shared hook for loading and mutating tenant app preferences.
 * Used by the dashboard settings page, list view settings page,
 * and anywhere else that reads/writes preferences.
 */
export function useAppPreferences() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const currentTenant = auth.tenants.find(
    (t) => t.tenantId === auth.selectedTenantId
  );
  const canManageSettings =
    currentTenant?.role === "OWNER" || currentTenant?.role === "ADMIN";

  const {
    data: prefs,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: queryKeys.appPreferences(auth.selectedTenantId),
    queryFn: () => getAppPreferences(api),
    enabled: ready && Boolean(auth.selectedTenantId),
  });

  const mutation = useMutation({
    mutationFn: (payload: Parameters<typeof updateAppPreferences>[1]) =>
      updateAppPreferences(api, payload),
    onSuccess: (updated) => {
      queryClient.setQueryData(
        queryKeys.appPreferences(auth.selectedTenantId),
        updated
      );
    },
  });

  const mutate = useCallback(
    (payload: Parameters<typeof updateAppPreferences>[1]) => {
      mutation.mutate(payload);
    },
    [mutation]
  );

  return {
    prefs: prefs ?? null,
    isLoading: !ready || isLoading,
    isError,
    error,
    saving: mutation.isPending,
    canManageSettings,
    mutate,
  };
}
