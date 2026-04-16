import type { AxiosInstance } from "axios";
import type { LeadSource } from "./types";

const LIMIT_OPTIONS = [500, 2000, 5000] as const;

export { LIMIT_OPTIONS };

export function parseFilenameFromContentDisposition(header?: string): string | null {
  if (!header) return null;
  const match = header.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
  if (!match) return null;
  let filename = match[1].replace(/['"]/g, "");
  if (filename.startsWith("UTF-8''")) {
    filename = decodeURIComponent(filename.slice(7));
  }
  return filename || null;
}

export function triggerBrowserDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export async function downloadLeadsCsv(
  api: AxiosInstance,
  params: { statusDefinitionId?: string; source?: LeadSource; limit?: number }
): Promise<{ blob: Blob; filename: string }> {
  const searchParams = new URLSearchParams();
  if (params.statusDefinitionId)
    searchParams.set("statusDefinitionId", params.statusDefinitionId);
  if (params.source) searchParams.set("source", params.source);
  if (params.limit != null) searchParams.set("limit", String(params.limit));
  const query = searchParams.toString();
  const url = `/api/v1/reports/leads.csv${query ? `?${query}` : ""}`;

  const response = await api.get(url, { responseType: "blob" });
  const blob = response.data as Blob;
  const contentDisposition = response.headers["content-disposition"];
  const filename =
    parseFilenameFromContentDisposition(contentDisposition) ??
    `leads-${new Date().toISOString().slice(0, 10)}.csv`;
  return { blob, filename };
}

export async function downloadJobsCsv(
  api: AxiosInstance,
  params: { statusDefinitionId?: string; limit?: number }
): Promise<{ blob: Blob; filename: string }> {
  const searchParams = new URLSearchParams();
  if (params.statusDefinitionId)
    searchParams.set("statusDefinitionId", params.statusDefinitionId);
  if (params.limit != null) searchParams.set("limit", String(params.limit));
  const query = searchParams.toString();
  const url = `/api/v1/reports/jobs.csv${query ? `?${query}` : ""}`;

  const response = await api.get(url, { responseType: "blob" });
  const blob = response.data as Blob;
  const contentDisposition = response.headers["content-disposition"];
  const filename =
    parseFilenameFromContentDisposition(contentDisposition) ??
    `jobs-${new Date().toISOString().slice(0, 10)}.csv`;
  return { blob, filename };
}

export async function getPaidInvoiceYears(
  api: AxiosInstance
): Promise<number[]> {
  const response = await api.get<number[]>("/api/v1/reports/invoices/paid/years");
  return response.data ?? [];
}

export async function downloadPaidInvoicesPdf(
  api: AxiosInstance,
  year: number
): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get(`/api/v1/reports/invoices/paid?year=${year}`, {
    responseType: "blob",
  });
  const blob = response.data as Blob;
  const contentDisposition = response.headers["content-disposition"];
  const filename =
    parseFilenameFromContentDisposition(contentDisposition) ??
    `paid-invoices-${year}.pdf`;
  return { blob, filename };
}

export async function downloadAccountingJobsExcel(
  api: AxiosInstance
): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get("/api/v1/reports/accounting/jobs.xlsx", {
    responseType: "blob",
  });
  const blob = response.data as Blob;
  const contentDisposition = response.headers["content-disposition"];
  const filename =
    parseFilenameFromContentDisposition(contentDisposition) ??
    `accounting-report-${new Date().toISOString().slice(0, 10)}.xlsx`;
  return { blob, filename };
}
