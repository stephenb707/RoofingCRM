import type { AxiosInstance } from "axios";
import axios from "axios";
import type {
  EstimateDto,
  EstimateStatus,
  CreateEstimateRequest,
  UpdateEstimateRequest,
  ShareEstimateResponse,
  PublicEstimateDto,
  PublicEstimateDecisionRequest,
} from "./types";

const publicApiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const publicApi = axios.create({
  baseURL: publicApiBaseUrl,
  headers: { "Content-Type": "application/json" },
});

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
 * Share estimate (generate/refresh public link). RBAC: OWNER, ADMIN, SALES.
 */
export async function shareEstimate(
  api: AxiosInstance,
  estimateId: string,
  options?: { expiresInDays?: number }
): Promise<ShareEstimateResponse> {
  const payload = options?.expiresInDays != null ? { expiresInDays: options.expiresInDays } : {};
  const res = await api.post<ShareEstimateResponse>(
    `/api/v1/estimates/${estimateId}/share`,
    Object.keys(payload).length > 0 ? payload : {}
  );
  return res.data;
}

/**
 * Fetch public estimate by token (no auth required).
 */
export async function getPublicEstimate(token: string): Promise<PublicEstimateDto> {
  const res = await publicApi.get<PublicEstimateDto>(
    `/api/public/estimates/${encodeURIComponent(token)}`
  );
  return res.data;
}

/**
 * Submit accept/reject decision on public estimate (no auth required).
 */
export async function decidePublicEstimate(
  token: string,
  payload: PublicEstimateDecisionRequest
): Promise<PublicEstimateDto> {
  const res = await publicApi.post<PublicEstimateDto>(
    `/api/public/estimates/${encodeURIComponent(token)}/decision`,
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
