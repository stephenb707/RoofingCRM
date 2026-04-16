import { useQuery } from "@tanstack/react-query";
import { useAuthReady } from "./AuthContext";
import { queryKeys } from "./queryKeys";
import { getAppPreferences } from "./preferencesApi";
import { resolveVisibleFields, type ListConfigKey } from "./listFieldConfig";

/**
 * Hook that returns the visible field keys for a given list page,
 * resolved from tenant preferences with fallback to defaults.
 */
export function useVisibleFields(listKey: ListConfigKey): {
  visibleFields: string[];
  isLoading: boolean;
} {
  const { api, auth, ready } = useAuthReady();

  const { data: prefs, isLoading } = useQuery({
    queryKey: queryKeys.appPreferences(auth.selectedTenantId),
    queryFn: () => getAppPreferences(api),
    enabled: ready && Boolean(auth.selectedTenantId),
    staleTime: 60_000,
  });

  const saved = prefs?.[listKey]?.visibleFields;
  const visibleFields = resolveVisibleFields(listKey, saved);

  return { visibleFields, isLoading };
}
