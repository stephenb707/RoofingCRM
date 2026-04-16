import type { AxiosInstance } from "axios";
import type { AppPreferencesDto, UpdateAppPreferencesRequest } from "./types";

export async function getAppPreferences(
  api: AxiosInstance
): Promise<AppPreferencesDto> {
  const res = await api.get<AppPreferencesDto>(
    "/api/v1/settings/preferences"
  );
  return res.data;
}

export async function updateAppPreferences(
  api: AxiosInstance,
  request: UpdateAppPreferencesRequest
): Promise<AppPreferencesDto> {
  const res = await api.put<AppPreferencesDto>(
    "/api/v1/settings/preferences",
    request
  );
  return res.data;
}
