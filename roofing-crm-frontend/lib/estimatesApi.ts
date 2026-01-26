import type { AxiosInstance } from "axios";
import type {
  EstimateDto,
  EstimateStatus,
  CreateEstimateRequest,
  UpdateEstimateRequest,
} from "./types";

/**
 * List all estimates for a job. Backend returns List<EstimateDto>.
 */
export async function listEstimatesForJob(
  api: AxiosInstance,
  jobId: string
): Promise<EstimateDto[]> {
  const res = await api.get<EstimateDto[]>(
    `/api/v1/jobs/${jobId}/estimates`
  );
  return res.data;
}

/**
 * Fetch a single estimate by ID.
 */
export async function getEstimate(
  api: AxiosInstance,
  estimateId: string
): Promise<EstimateDto> {
  const res = await api.get<EstimateDto>(`/api/v1/estimates/${estimateId}`);
  return res.data;
}

/**
 * Create an estimate for a job. Request must include at least one item.
 */
export async function createEstimateForJob(
  api: AxiosInstance,
  jobId: string,
  payload: CreateEstimateRequest
): Promise<EstimateDto> {
  const res = await api.post<EstimateDto>(
    `/api/v1/jobs/${jobId}/estimates`,
    payload
  );
  return res.data;
}

/**
 * Update an estimate (metadata and/or replace items).
 * If items is provided, backend replaces all items.
 */
export async function updateEstimate(
  api: AxiosInstance,
  estimateId: string,
  payload: UpdateEstimateRequest
): Promise<EstimateDto> {
  const res = await api.put<EstimateDto>(
    `/api/v1/estimates/${estimateId}`,
    payload
  );
  return res.data;
}

/**
 * Update estimate status via POST /api/v1/estimates/{id}/status.
 */
export async function updateEstimateStatus(
  api: AxiosInstance,
  estimateId: string,
  status: EstimateStatus
): Promise<EstimateDto> {
  const res = await api.post<EstimateDto>(
    `/api/v1/estimates/${estimateId}/status`,
    { status }
  );
  return res.data;
}
