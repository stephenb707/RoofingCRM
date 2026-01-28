"use client";

import { useRef } from "react";
import { formatFileSize } from "@/lib/format";
import type { AttachmentDto } from "@/lib/types";

export interface AttachmentSectionProps {
  title: string;
  attachments: AttachmentDto[];
  onUpload: (file: File) => void | Promise<void>;
  onDownload: (attachmentId: string, fileName: string) => void | Promise<void>;
  isLoading?: boolean;
  isUploading?: boolean;
  errorMessage?: string | null;
}

export function AttachmentSection({
  title,
  attachments,
  onUpload,
  onDownload,
  isLoading = false,
  isUploading = false,
  errorMessage = null,
}: AttachmentSectionProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUpload(file);
      e.target.value = "";
    }
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">{title}</h2>

      {errorMessage && (
        <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {errorMessage}
        </div>
      )}

      <div className="flex flex-wrap items-center gap-2 mb-4">
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileChange}
          className="hidden"
          aria-label="Choose file to upload"
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}
          className="px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors disabled:opacity-60"
          aria-label="Upload"
        >
          {isUploading ? "Uploading…" : "Upload"}
        </button>
      </div>

      {isLoading ? (
        <p className="text-sm text-slate-500">Loading attachments…</p>
      ) : attachments.length === 0 ? (
        <p className="text-sm text-slate-500">No attachments yet.</p>
      ) : (
        <ul className="space-y-2">
          {attachments.map((a) => (
            <li
              key={a.id}
              className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0"
            >
              <div className="min-w-0 flex-1">
                <span className="text-sm font-medium text-slate-800 truncate block">
                  {a.fileName ?? "Unnamed"}
                </span>
                <span className="text-xs text-slate-500">
                  {formatFileSize(a.fileSize)}
                  {a.contentType ? ` · ${a.contentType}` : ""}
                </span>
              </div>
              <button
                type="button"
                onClick={() => onDownload(a.id, a.fileName ?? a.id)}
                className="ml-3 px-3 py-1.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg shrink-0"
              >
                Download
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
