import type { AxiosInstance } from "axios";
import type {
  InvoiceDto,
  InvoiceStatus,
  PageResponse,
} from "./types";

export interface CreateInvoiceFromEstimateParams {
  estimateId: string;
  dueAt?: string | null;
  notes?: string | null;
}

export interface ListInvoicesParams {
  jobId?: string | null;
  status?: InvoiceStatus | null;
  page?: number;
  size?: number;
}

/**
 * Create an invoice from an ACCEPTED estimate.
 */
export async function createInvoiceFromEstimate(
  api: AxiosInstance,
  params: CreateInvoiceFromEstimateParams
): Promise<InvoiceDto> {
  const payload: Record<string, unknown> = { estimateId: params.estimateId };
  if (params.dueAt != null && params.dueAt !== "") payload.dueAt = params.dueAt;
  if (params.notes != null && params.notes !== "") payload.notes = params.notes;
  const res = await api.post<InvoiceDto>("/api/v1/invoices", payload);
  return res.data;
}

/**
 * List invoices with optional filters (paginated).
 */
export async function listInvoices(
  api: AxiosInstance,
  params: ListInvoicesParams = {}
): Promise<PageResponse<InvoiceDto>> {
  const queryParams: Record<string, string | number> = {};
  if (params.jobId != null && params.jobId !== "") queryParams.jobId = params.jobId;
  if (params.status != null) queryParams.status = params.status;
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;
  const res = await api.get<PageResponse<InvoiceDto>>("/api/v1/invoices", {
    params: queryParams,
  });
  return res.data;
}

/**
 * List invoices for a specific job (non-paginated).
 */
export async function listInvoicesForJob(
  api: AxiosInstance,
  jobId: string
): Promise<InvoiceDto[]> {
  const res = await api.get<InvoiceDto[]>(`/api/v1/invoices/job/${jobId}`);
  return res.data;
}

/**
 * Get a single invoice by ID.
 */
export async function getInvoice(
  api: AxiosInstance,
  invoiceId: string
): Promise<InvoiceDto> {
  const res = await api.get<InvoiceDto>(`/api/v1/invoices/${invoiceId}`);
  return res.data;
}

/**
 * Update invoice status.
 */
export async function updateInvoiceStatus(
  api: AxiosInstance,
  invoiceId: string,
  status: InvoiceStatus
): Promise<InvoiceDto> {
  const res = await api.put<InvoiceDto>(`/api/v1/invoices/${invoiceId}/status`, {
    status,
  });
  return res.data;
}
