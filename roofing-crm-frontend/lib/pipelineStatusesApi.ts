import type { AxiosInstance } from "axios";

/** Matches backend `PipelineType` enum. */
export type PipelineTypeApi = "LEAD" | "JOB";

/** Matches backend `PipelineStatusDefinitionDto`. */
export interface PipelineStatusDefinitionDto {
  id: string;
  pipelineType: PipelineTypeApi;
  systemKey: string;
  label: string;
  sortOrder: number;
  builtIn: boolean;
  active: boolean;
}

/**
 * Active pipeline status definitions for building columns and filters (GET /api/v1/pipeline-statuses?type=LEAD|JOB).
 */
export async function listPipelineStatuses(
  api: AxiosInstance,
  type: PipelineTypeApi
): Promise<PipelineStatusDefinitionDto[]> {
  const res = await api.get<PipelineStatusDefinitionDto[]>("/api/v1/pipeline-statuses", {
    params: { type },
  });
  return res.data;
}

export interface CreatePipelineStatusRequest {
  pipelineType: PipelineTypeApi;
  label: string;
}

export interface UpdatePipelineStatusRequest {
  label: string;
}

export interface ReorderPipelineStatusesRequest {
  pipelineType: PipelineTypeApi;
  orderedDefinitionIds: string[];
}

/**
 * Admin: all non-archived definitions for a pipeline type (includes inactive).
 */
export async function listSettingsPipelineStatuses(
  api: AxiosInstance,
  type: PipelineTypeApi
): Promise<PipelineStatusDefinitionDto[]> {
  const res = await api.get<PipelineStatusDefinitionDto[]>(
    "/api/v1/settings/pipeline-statuses",
    { params: { type } }
  );
  return res.data;
}

export async function createSettingsPipelineStatus(
  api: AxiosInstance,
  payload: CreatePipelineStatusRequest
): Promise<PipelineStatusDefinitionDto> {
  const res = await api.post<PipelineStatusDefinitionDto>(
    "/api/v1/settings/pipeline-statuses",
    payload
  );
  return res.data;
}

export async function updateSettingsPipelineStatus(
  api: AxiosInstance,
  id: string,
  payload: UpdatePipelineStatusRequest
): Promise<PipelineStatusDefinitionDto> {
  const res = await api.put<PipelineStatusDefinitionDto>(
    `/api/v1/settings/pipeline-statuses/${id}`,
    payload
  );
  return res.data;
}

export async function reorderSettingsPipelineStatuses(
  api: AxiosInstance,
  payload: ReorderPipelineStatusesRequest
): Promise<void> {
  await api.put("/api/v1/settings/pipeline-statuses/reorder", payload);
}

export async function restoreDefaultPipelineStatuses(
  api: AxiosInstance,
  type: PipelineTypeApi,
  deactivateUnusedCustom: boolean
): Promise<void> {
  await api.post("/api/v1/settings/pipeline-statuses/restore-defaults", null, {
    params: { type, deactivateUnusedCustom },
  });
}

export async function deactivateSettingsPipelineStatus(
  api: AxiosInstance,
  id: string
): Promise<void> {
  await api.delete(`/api/v1/settings/pipeline-statuses/${id}`);
}
