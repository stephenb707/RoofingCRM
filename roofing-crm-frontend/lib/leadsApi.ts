import { AxiosInstance } from "axios";
import {
  LeadDto,
  CreateLeadRequest,
  UpdateLeadRequest,
  UpdateLeadStatusRequest,
  ConvertLeadToJobRequest,
  JobDto,
  LeadStatus,
  PageResponse,
} from "./types";

export interface ListLeadsParams {
  status?: LeadStatus | null;
  customerId?: string | null;
  page?: number;
  size?: number;
}

/**
 * Fetch paginated list of leads with optional status and customerId filters.
 */
export async function listLeads(
  api: AxiosInstance,
  params: ListLeadsParams = {}
): Promise<PageResponse<LeadDto>> {
  const queryParams: Record<string, string | number> = {};

  if (params.status) {
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

  const response = await api.get<PageResponse<LeadDto>>("/api/v1/leads", {
    params: queryParams,
  });
  return response.data;
}

export interface PickerItem {
  id: string;
  label: string;
  subLabel: string;
}

/**
 * Search leads for picker (lightweight { id, label, subLabel }).
 */
export async function searchLeadsPicker(
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
  const res = await api.get<PickerItem[]>("/api/v1/leads/picker", {
    params: queryParams,
  });
  return res.data;
}

/**
 * Fetch a single lead by ID.
 */
export async function getLead(
  api: AxiosInstance,
  leadId: string
): Promise<LeadDto> {
  const response = await api.get<LeadDto>(`/api/v1/leads/${leadId}`);
  return response.data;
}

/**
 * Create a new lead.
 */
export async function createLead(
  api: AxiosInstance,
  payload: CreateLeadRequest
): Promise<LeadDto> {
  const response = await api.post<LeadDto>("/api/v1/leads", payload);
  return response.data;
}

/**
 * Update an existing lead (source, leadNotes, propertyAddress).
 */
export async function updateLead(
  api: AxiosInstance,
  leadId: string,
  payload: UpdateLeadRequest
): Promise<LeadDto> {
  const response = await api.put<LeadDto>(`/api/v1/leads/${leadId}`, payload);
  return response.data;
}

/**
 * Update the status of an existing lead.
 */
export async function updateLeadStatus(
  api: AxiosInstance,
  leadId: string,
  status: LeadStatus
): Promise<LeadDto> {
  const payload: UpdateLeadStatusRequest = { status };
  const response = await api.post<LeadDto>(
    `/api/v1/leads/${leadId}/status`,
    payload
  );
  return response.data;
}

/**
 * Convert a lead to a job. Alias: convertLead.
 */
export async function convertLeadToJob(
  api: AxiosInstance,
  leadId: string,
  payload: ConvertLeadToJobRequest
): Promise<JobDto> {
  const response = await api.post<JobDto>(
    `/api/v1/leads/${leadId}/convert`,
    payload
  );
  return response.data;
}
