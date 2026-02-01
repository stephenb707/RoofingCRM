import type { AxiosInstance } from "axios";
import type {
  JobDto,
  JobStatus,
  CreateJobRequest,
  UpdateJobRequest,
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

export interface PickerItem {
  id: string;
  label: string;
  subLabel: string;
}

/**
 * Search jobs for picker (lightweight { id, label, subLabel }).
 */
export async function searchJobsPicker(
  api: AxiosInstance,
  params: { q?: string | null; limit?: number } = {}
): Promise<PickerItem[]> {
  const queryParams: Record<string, string | number> = {};
  if (params.q != null && params.q !== "") {
    queryParams.q = params.q;
  }
  if (params.limit != null) {
    queryParams.limit = params.limit;
  }
  const res = await api.get<PickerItem[]>("/api/v1/jobs/picker", {
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
 * Update an existing job (type, propertyAddress, scheduled dates, internalNotes, crewName).
 */
export async function updateJob(
  api: AxiosInstance,
  jobId: string,
  payload: UpdateJobRequest
): Promise<JobDto> {
  const res = await api.put<JobDto>(`/api/v1/jobs/${jobId}`, payload);
  return res.data;
}

/**
 * Fetch jobs for schedule view (GET /api/v1/jobs/schedule).
 * Returns non-paged list sorted by scheduledStartDate asc nulls last, createdAt desc.
 */
export async function listJobSchedule(
  api: AxiosInstance,
  params: {
    from: string;
    to: string;
    status?: JobStatus | null;
    crewName?: string | null;
    includeUnscheduled?: boolean;
  }
): Promise<JobDto[]> {
  const queryParams: Record<string, string | number | boolean> = {
    from: params.from,
    to: params.to,
  };
  if (params.status != null && String(params.status) !== "") {
    queryParams.status = params.status;
  }
  if (params.crewName != null && params.crewName !== "") {
    queryParams.crewName = params.crewName;
  }
  if (params.includeUnscheduled !== undefined) {
    queryParams.includeUnscheduled = params.includeUnscheduled;
  }

  const res = await api.get<JobDto[]>("/api/v1/jobs/schedule", {
    params: queryParams,
  });
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
