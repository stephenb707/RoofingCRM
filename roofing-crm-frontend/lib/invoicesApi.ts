import type { AxiosInstance } from "axios";
import axios from "axios";
import type {
  InvoiceDto,
  InvoiceSummaryDto,
  InvoiceStatus,
  PageResponse,
  PublicInvoiceDto,
  SendInvoiceEmailRequest,
  SendInvoiceEmailResponse,
  ShareInvoiceResponse,
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

const publicApiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const publicApi = axios.create({
  baseURL: publicApiBaseUrl,
  headers: { "Content-Type": "application/json" },
});

/**
 * Create an invoice from an estimate.
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
): Promise<PageResponse<InvoiceSummaryDto>> {
  const queryParams: Record<string, string | number> = {};
  if (params.jobId != null && params.jobId !== "") queryParams.jobId = params.jobId;
  if (params.status != null) queryParams.status = params.status;
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;
  const res = await api.get<PageResponse<InvoiceSummaryDto>>("/api/v1/invoices", {
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
): Promise<InvoiceSummaryDto[]> {
  const res = await api.get<InvoiceSummaryDto[]>(`/api/v1/invoices/job/${jobId}`);
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

export async function shareInvoice(
  api: AxiosInstance,
  invoiceId: string,
  options?: { expiresInDays?: number }
): Promise<ShareInvoiceResponse> {
  const payload =
    options?.expiresInDays != null
      ? { expiresInDays: options.expiresInDays }
      : {};
  const res = await api.post<ShareInvoiceResponse>(
    `/api/v1/invoices/${invoiceId}/share`,
    Object.keys(payload).length > 0 ? payload : {}
  );
  return res.data;
}

export async function sendInvoiceEmail(
  api: AxiosInstance,
  invoiceId: string,
  payload: SendInvoiceEmailRequest
): Promise<SendInvoiceEmailResponse> {
  const res = await api.post<SendInvoiceEmailResponse>(
    `/api/v1/invoices/${invoiceId}/send-email`,
    payload
  );
  return res.data;
}

export function buildPublicInvoiceUrl(token: string): string {
  if (typeof window === "undefined") return `/invoice/${token}`;
  return `${window.location.origin}/invoice/${token}`;
}

export async function getPublicInvoice(token: string): Promise<PublicInvoiceDto> {
  const res = await publicApi.get<PublicInvoiceDto>(
    `/api/public/invoices/${encodeURIComponent(token)}`
  );
  return res.data;
}
