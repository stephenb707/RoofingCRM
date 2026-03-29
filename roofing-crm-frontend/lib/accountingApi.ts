import type { AxiosInstance } from "axios";
import type {
  CreateCostFromReceiptRequest,
  CreateJobCostEntryRequest,
  JobAccountingSummaryDto,
  JobCostEntryDto,
  JobReceiptDto,
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

export async function listJobReceipts(
  api: AxiosInstance,
  jobId: string
): Promise<JobReceiptDto[]> {
  const res = await api.get<JobReceiptDto[]>(`/api/v1/jobs/${jobId}/receipts`);
  return res.data;
}

export async function uploadJobReceipt(
  api: AxiosInstance,
  jobId: string,
  file: File,
  description?: string | null
): Promise<JobReceiptDto> {
  const form = new FormData();
  form.append("file", file);
  if (description != null && description !== "") {
    form.append("description", description);
  }
  const res = await api.post<JobReceiptDto>(`/api/v1/jobs/${jobId}/receipts`, form);
  return res.data;
}

export async function createCostFromReceipt(
  api: AxiosInstance,
  jobId: string,
  receiptId: string,
  payload: CreateCostFromReceiptRequest
): Promise<JobCostEntryDto> {
  const res = await api.post<JobCostEntryDto>(
    `/api/v1/jobs/${jobId}/receipts/${receiptId}/create-cost`,
    payload
  );
  return res.data;
}

export async function linkReceiptToCost(
  api: AxiosInstance,
  jobId: string,
  receiptId: string,
  costEntryId: string
): Promise<JobReceiptDto> {
  const res = await api.put<JobReceiptDto>(
    `/api/v1/jobs/${jobId}/receipts/${receiptId}/link-cost/${costEntryId}`
  );
  return res.data;
}

export async function unlinkReceiptFromCost(
  api: AxiosInstance,
  jobId: string,
  receiptId: string
): Promise<JobReceiptDto> {
  const res = await api.delete<JobReceiptDto>(`/api/v1/jobs/${jobId}/receipts/${receiptId}/link-cost`);
  return res.data;
}

export async function deleteJobReceipt(
  api: AxiosInstance,
  jobId: string,
  receiptId: string
): Promise<void> {
  await api.delete(`/api/v1/jobs/${jobId}/receipts/${receiptId}`);
}
