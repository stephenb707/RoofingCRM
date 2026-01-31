import type { AxiosInstance } from "axios";
import type {
  ActivityEventDto,
  ActivityEntityType,
  CreateNoteRequest,
  PageResponse,
} from "./types";

export interface ListActivityParams {
  entityType: ActivityEntityType;
  entityId: string;
  page?: number;
  size?: number;
}

/**
 * Fetch paginated activity events for an entity (lead or job).
 */
export async function listActivity(
  api: AxiosInstance,
  params: ListActivityParams
): Promise<PageResponse<ActivityEventDto>> {
  const queryParams: Record<string, string | number> = {
    entityType: params.entityType,
    entityId: params.entityId,
  };
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;

  const res = await api.get<PageResponse<ActivityEventDto>>("/api/v1/activity", {
    params: queryParams,
  });
  return res.data;
}

/**
 * Create a note (NOTE event) on a lead or job.
 */
export async function createNote(
  api: AxiosInstance,
  payload: CreateNoteRequest
): Promise<ActivityEventDto> {
  const res = await api.post<ActivityEventDto>("/api/v1/activity/notes", payload);
  return res.data;
}
