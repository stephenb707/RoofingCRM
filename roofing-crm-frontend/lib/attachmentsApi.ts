import type { AxiosInstance } from "axios";
import type { AttachmentDto } from "./types";

/**
 * List attachments for a lead.
 */
export async function listLeadAttachments(
  api: AxiosInstance,
  leadId: string
): Promise<AttachmentDto[]> {
  const { data } = await api.get<AttachmentDto[]>(`/api/v1/leads/${leadId}/attachments`);
  return data;
}

/**
 * Upload a file as an attachment for a lead.
 * Uses FormData; do not set Content-Type manually (axios sets multipart boundary).
 */
export async function uploadLeadAttachment(
  api: AxiosInstance,
  leadId: string,
  file: File
): Promise<AttachmentDto> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await api.post<AttachmentDto>(`/api/v1/leads/${leadId}/attachments`, form);
  return data;
}

/**
 * List attachments for a job.
 */
export async function listJobAttachments(
  api: AxiosInstance,
  jobId: string
): Promise<AttachmentDto[]> {
  const { data } = await api.get<AttachmentDto[]>(`/api/v1/jobs/${jobId}/attachments`);
  return data;
}

/**
 * Upload a file as an attachment for a job.
 */
export async function uploadJobAttachment(
  api: AxiosInstance,
  jobId: string,
  file: File
): Promise<AttachmentDto> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await api.post<AttachmentDto>(`/api/v1/jobs/${jobId}/attachments`, form);
  return data;
}

/**
 * Download attachment content as a Blob.
 */
export async function downloadAttachment(
  api: AxiosInstance,
  attachmentId: string
): Promise<Blob> {
  const { data } = await api.get<Blob>(`/api/v1/attachments/${attachmentId}/download`, {
    responseType: "blob",
  });
  return data;
}
