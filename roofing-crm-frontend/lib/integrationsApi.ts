import type { AxiosInstance } from "axios";
import type {
  IntegrationSettingsDto,
  UpdateIntegrationSettingsRequest,
} from "./types";

export async function listIntegrationSettings(
  api: AxiosInstance
): Promise<IntegrationSettingsDto[]> {
  const res = await api.get<IntegrationSettingsDto[]>(
    "/api/v1/settings/integrations"
  );
  return res.data;
}

export async function updateIntegrationSettings(
  api: AxiosInstance,
  provider: string,
  body: UpdateIntegrationSettingsRequest
): Promise<IntegrationSettingsDto> {
  const res = await api.put<IntegrationSettingsDto>(
    `/api/v1/settings/integrations/${provider}`,
    body
  );
  return res.data;
}

export async function disableIntegration(
  api: AxiosInstance,
  provider: string
): Promise<IntegrationSettingsDto> {
  const res = await api.post<IntegrationSettingsDto>(
    `/api/v1/settings/integrations/${provider}/disable`
  );
  return res.data;
}
