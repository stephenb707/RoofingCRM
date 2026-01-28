import type { AxiosInstance } from "axios";
import type {
  CommunicationLogDto,
  CreateCommunicationLogRequest,
} from "./types";

/**
 * List communication logs for a lead.
 */
export async function listLeadCommunicationLogs(
  api: AxiosInstance,
  leadId: string
): Promise<CommunicationLogDto[]> {
  const { data } = await api.get<CommunicationLogDto[]>(
    `/api/v1/leads/${leadId}/communications`
  );
  return data;
}

/**
 * Add a communication log for a lead.
 */
export async function addLeadCommunicationLog(
  api: AxiosInstance,
  leadId: string,
  payload: CreateCommunicationLogRequest
): Promise<CommunicationLogDto> {
  const { data } = await api.post<CommunicationLogDto>(
    `/api/v1/leads/${leadId}/communications`,
    payload
  );
  return data;
}

/**
 * List communication logs for a job.
 */
export async function listJobCommunicationLogs(
  api: AxiosInstance,
  jobId: string
): Promise<CommunicationLogDto[]> {
  const { data } = await api.get<CommunicationLogDto[]>(
    `/api/v1/jobs/${jobId}/communications`
  );
  return data;
}

/**
 * Add a communication log for a job.
 */
export async function addJobCommunicationLog(
  api: AxiosInstance,
  jobId: string,
  payload: CreateCommunicationLogRequest
): Promise<CommunicationLogDto> {
  const { data } = await api.post<CommunicationLogDto>(
    `/api/v1/jobs/${jobId}/communications`,
    payload
  );
  return data;
}
