import type { AxiosInstance } from "axios";
import type { LeadSource } from "./types";

const LIMIT_OPTIONS = [500, 2000, 5000] as const;

export { LIMIT_OPTIONS };

export function parseFilenameFromContentDisposition(header?: string): string | null {
  if (!header) return null;

  const star = header.match(/filename\*\s*=\s*UTF-8''([^;\s]+)/i);
  if (star) {
    try {
      const decoded = decodeURIComponent(star[1].replace(/^"+|"+$/g, ""));
      if (decoded) return decoded;
    } catch {
      /* fall through */
    }
  }

  const rfc2047 = header.match(/filename\s*=\s*"\s*=\?UTF-8\?Q\?([^?]+)\?\="/i);
  if (rfc2047?.[1]) {
    return rfc2047[1].replace(/_/g, " ");
  }

  const match = header.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
  if (!match) return null;
  let filename = match[1].replace(/['"]/g, "");
  if (filename.startsWith("UTF-8''")) {
    filename = decodeURIComponent(filename.slice(7));
  }
  return filename || null;
}

/** Client-side fallback for PDF download names; mirrors backend sanitizer loosely. */
export function simplePdfFilenameFromTitle(title?: string | null): string {
  const defaultName = "Customer Report.pdf";
  if (!title) return defaultName;
  let t = title.replace(/\u00a0/g, " ").trim();
  if (!t) return defaultName;
  const lower = t.toLowerCase();
  if (lower.endsWith(".pdf")) t = t.slice(0, -4).trim();
  if (!t) return defaultName;
  t = t
    .replace(/\s+/g, " ")
    .replace(/[<>:"/\\|?*\u0000-\u001f]/g, "-")
    .replace(/"/g, "");
  t = t.replace(/-+/g, "-").replace(/[. ]+$/g, "");
  if (!t) return defaultName;
  const stem = t.length > 180 ? t.slice(0, 180).trim().replace(/[. ]+$/g, "") : t;
  return `${stem || "Customer Report"}.pdf`;
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
