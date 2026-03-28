import type { AxiosInstance } from "axios";
import type {
  CreateJobCostEntryRequest,
  JobAccountingSummaryDto,
  JobCostEntryDto,
  UpdateJobCostEntryRequest,
} from "./types";

export async function getJobAccountingSummary(
  api: AxiosInstance,
  jobId: string
): Promise<JobAccountingSummaryDto> {
  const res = await api.get<JobAccountingSummaryDto>(`/api/v1/jobs/${jobId}/accounting/summary`);
  return res.data;
}

export async function listJobCostEntries(
  api: AxiosInstance,
  jobId: string
): Promise<JobCostEntryDto[]> {
  const res = await api.get<JobCostEntryDto[]>(`/api/v1/jobs/${jobId}/costs`);
  return res.data;
}

export async function createJobCostEntry(
  api: AxiosInstance,
  jobId: string,
  payload: CreateJobCostEntryRequest
): Promise<JobCostEntryDto> {
  const res = await api.post<JobCostEntryDto>(`/api/v1/jobs/${jobId}/costs`, payload);
  return res.data;
}

export async function updateJobCostEntry(
  api: AxiosInstance,
  jobId: string,
  costEntryId: string,
  payload: UpdateJobCostEntryRequest
): Promise<JobCostEntryDto> {
  const res = await api.put<JobCostEntryDto>(`/api/v1/jobs/${jobId}/costs/${costEntryId}`, payload);
  return res.data;
}

export async function deleteJobCostEntry(
  api: AxiosInstance,
  jobId: string,
  costEntryId: string
): Promise<void> {
  await api.delete(`/api/v1/jobs/${jobId}/costs/${costEntryId}`);
}
