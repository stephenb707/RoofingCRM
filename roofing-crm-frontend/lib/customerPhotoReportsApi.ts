import type { AxiosInstance } from "axios";
import type { AttachmentDto } from "./types";
import type {
  CustomerPhotoReportDto,
  SendCustomerPhotoReportEmailRequest,
  SendCustomerPhotoReportEmailResponse,
  CustomerPhotoReportSummaryDto,
  UpsertCustomerPhotoReportRequest,
} from "./types";
import {
  parseFilenameFromContentDisposition,
  simplePdfFilenameFromTitle,
} from "./reportsApi";

const BASE = "/api/v1/customer-photo-reports";

export async function listCustomerPhotoReports(
  api: AxiosInstance
): Promise<CustomerPhotoReportSummaryDto[]> {
  const { data } = await api.get<CustomerPhotoReportSummaryDto[]>(BASE);
  return data;
}

export async function getCustomerPhotoReport(
  api: AxiosInstance,
  reportId: string
): Promise<CustomerPhotoReportDto> {
  const { data } = await api.get<CustomerPhotoReportDto>(`${BASE}/${reportId}`);
  return data;
}

export async function createCustomerPhotoReport(
  api: AxiosInstance,
  payload: UpsertCustomerPhotoReportRequest
): Promise<CustomerPhotoReportDto> {
  const { data } = await api.post<CustomerPhotoReportDto>(BASE, payload);
  return data;
}

export async function updateCustomerPhotoReport(
  api: AxiosInstance,
  reportId: string,
  payload: UpsertCustomerPhotoReportRequest
): Promise<CustomerPhotoReportDto> {
  const { data } = await api.put<CustomerPhotoReportDto>(`${BASE}/${reportId}`, payload);
  return data;
}

export async function deleteCustomerPhotoReport(api: AxiosInstance, reportId: string): Promise<void> {
  await api.delete(`${BASE}/${reportId}`);
}

export async function downloadCustomerPhotoReportPdf(
  api: AxiosInstance,
  reportId: string,
  reportTitleFallback?: string | null
): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get(`${BASE}/${String(reportId).trim()}/pdf`, { responseType: "blob" });
  const blob = response.data as Blob;
  const cd = response.headers["content-disposition"];
  const filename =
    parseFilenameFromContentDisposition(
      typeof cd === "string" ? cd : Array.isArray(cd) ? cd[0] : undefined
    ) ?? simplePdfFilenameFromTitle(reportTitleFallback);
  return { blob, filename };
}

export async function sendCustomerPhotoReportEmail(
  api: AxiosInstance,
  reportId: string,
  payload: SendCustomerPhotoReportEmailRequest
): Promise<SendCustomerPhotoReportEmailResponse> {
  const { data } = await api.post<SendCustomerPhotoReportEmailResponse>(`${BASE}/${reportId}/send-email`, payload);
  return data;
}

export async function listReportAttachmentCandidates(
  api: AxiosInstance,
  customerId: string,
  jobId?: string | null
): Promise<AttachmentDto[]> {
  const params: Record<string, string> = { customerId };
  if (jobId) params.jobId = jobId;
  const { data } = await api.get<AttachmentDto[]>(`${BASE}/attachment-candidates`, { params });
  return data;
}
