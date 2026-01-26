import type { AxiosInstance } from "axios";
import type {
  JobDto,
  JobStatus,
  JobType,
  CreateJobRequest,
  PageResponse,
} from "./types";

export interface ListJobsParams {
  status?: JobStatus | null;
  customerId?: string | null;
  page?: number;
  size?: number;
}

/**
 * Fetch paginated list of jobs with optional filters.
 */
export async function listJobs(
  api: AxiosInstance,
  params: ListJobsParams = {}
): Promise<PageResponse<JobDto>> {
  const queryParams: Record<string, string | number> = {};
  if (params.status != null) {
    queryParams.status = params.status;
  }
  if (params.customerId != null && params.customerId !== "") {
    queryParams.customerId = params.customerId;
  }
  if (params.page !== undefined) {
    queryParams.page = params.page;
  }
  if (params.size !== undefined) {
    queryParams.size = params.size;
  }

  const res = await api.get<PageResponse<JobDto>>("/api/v1/jobs", {
    params: queryParams,
  });
  return res.data;
}

/**
 * Fetch a single job by ID.
 */
export async function getJob(
  api: AxiosInstance,
  jobId: string
): Promise<JobDto> {
  const res = await api.get<JobDto>(`/api/v1/jobs/${jobId}`);
  return res.data;
}

/**
 * Create a new job.
 */
export async function createJob(
  api: AxiosInstance,
  payload: CreateJobRequest
): Promise<JobDto> {
  const res = await api.post<JobDto>("/api/v1/jobs", payload);
  return res.data;
}

/**
 * Update job status via POST /api/v1/jobs/{id}/status.
 */
export async function updateJobStatus(
  api: AxiosInstance,
  jobId: string,
  status: JobStatus
): Promise<JobDto> {
  const res = await api.post<JobDto>(`/api/v1/jobs/${jobId}/status`, {
    status,
  });
  return res.data;
}
