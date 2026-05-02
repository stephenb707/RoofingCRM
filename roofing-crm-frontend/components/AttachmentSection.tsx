"use client";

import { useCallback, useRef, useState } from "react";
import { ImagePreviewLightbox } from "@/components/ImagePreviewLightbox";
import { formatFileSize } from "@/lib/format";
import { supportsReportGalleryImage } from "@/lib/reportGalleryImageMime";
import { ATTACHMENT_TAGS, TAG_LABELS } from "@/lib/attachmentConstants";
import type { AttachmentDto, AttachmentTag } from "@/lib/types";

export interface UploadOptions {
  tag?: AttachmentTag;
  description?: string;
}

export interface AttachmentSectionProps {
  title: string;
  attachments: AttachmentDto[];
  onUpload: (file: File, options?: UploadOptions) => void | Promise<void>;
  onDownload: (attachmentId: string, fileName: string) => void | Promise<void>;
  onDelete?: (attachmentId: string, fileName: string) => void | Promise<void>;
  isLoading?: boolean;
  isUploading?: boolean;
  deletingAttachmentId?: string | null;
  errorMessage?: string | null;
  previewUrls?: Record<string, string>;
  loadingPreviewIds?: string[];
  scrollableList?: boolean;
}

export function AttachmentSection({
  title,
  attachments,
  onUpload,
  onDownload,
  onDelete,
  isLoading = false,
  isUploading = false,
  deletingAttachmentId = null,
  errorMessage = null,
  previewUrls = {},
  loadingPreviewIds = [],
  scrollableList = false,
}: AttachmentSectionProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [selectedTag, setSelectedTag] = useState<AttachmentTag>("OTHER");
  const [description, setDescription] = useState("");
  const [imagePreview, setImagePreview] = useState<{ url: string; alt: string } | null>(null);
  const closeImagePreview = useCallback(() => setImagePreview(null), []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUpload(file, {
        tag: selectedTag,
        description: description.trim() || undefined,
      });
      setDescription("");
      e.target.value = "";
    }
  };

  const thumbFrame =
    "flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-md border border-slate-200 bg-slate-100";

  const renderPreview = (attachment: AttachmentDto) => {
    const isGalleryRaster = supportsReportGalleryImage(attachment.contentType);
    if (!isGalleryRaster) {
      return null;
    }
    const previewUrl = previewUrls[attachment.id];
    const isLoadingPreview = loadingPreviewIds.includes(attachment.id);
    const altLabel = attachment.fileName ?? "Attachment preview";
    if (previewUrl) {
      return (
        <button
          type="button"
          data-testid={`attachment-thumbnail-${attachment.id}`}
          aria-label={`View full size: ${altLabel}`}
          className={`${thumbFrame} cursor-zoom-in p-0`}
          onClick={() => setImagePreview({ url: previewUrl, alt: altLabel })}
        >
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={previewUrl}
            alt={altLabel}
            className="pointer-events-none h-full w-full object-cover"
          />
        </button>
      );
    }
    return (
      <div data-testid={`attachment-thumbnail-${attachment.id}`} className={thumbFrame}>
        {isLoadingPreview ? (
          <span className="text-[10px] text-slate-400">Loading…</span>
        ) : (
          <span className="text-[10px] text-slate-400">No preview</span>
        )}
      </div>
    );
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">{title}</h2>

      {errorMessage && (
        <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {errorMessage}
        </div>
      )}

      <div className="flex flex-wrap items-end gap-2 mb-4">
        <input
          ref={fileInputRef}
          type="file"
          onChange={handleFileChange}
          className="hidden"
          aria-label="Choose file to upload"
        />
        <select
          value={selectedTag}
          onChange={(e) => setSelectedTag(e.target.value as AttachmentTag)}
          className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          aria-label="Tag"
        >
          {ATTACHMENT_TAGS.map((t) => (
            <option key={t} value={t}>
              {TAG_LABELS[t]}
            </option>
          ))}
        </select>
        <input
          type="text"
          placeholder="Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          className="border border-slate-300 rounded-lg px-3 py-2 text-sm w-40 focus:outline-none focus:ring-2 focus:ring-sky-500"
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
        <div
          data-testid="attachments-scroll-region"
          className={scrollableList ? "max-h-80 overflow-y-auto pr-2 overscroll-contain" : undefined}
        >
          <ul className="space-y-2">
            {attachments.map((a) => (
              <li
                key={a.id}
                className="flex items-center justify-between gap-3 py-2 border-b border-slate-100 last:border-0"
              >
                {renderPreview(a)}
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-sm font-medium text-slate-800 truncate">
                      {a.fileName ?? "Unnamed"}
                    </span>
                    {a.tag && (
                      <span className="text-xs px-2 py-0.5 rounded bg-slate-100 text-slate-600 shrink-0">
                        {TAG_LABELS[a.tag] ?? a.tag}
                      </span>
                    )}
                  </div>
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
                {onDelete && (
                  <button
                    type="button"
                    onClick={() => onDelete(a.id, a.fileName ?? a.id)}
                    disabled={deletingAttachmentId === a.id}
                    className="ml-2 px-3 py-1.5 text-sm font-medium text-red-700 bg-white border border-red-200 hover:bg-red-50 rounded-lg shrink-0 disabled:opacity-60"
                  >
                    {deletingAttachmentId === a.id ? "Deleting…" : "Delete"}
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
      {imagePreview ? (
        <ImagePreviewLightbox
          url={imagePreview.url}
          alt={imagePreview.alt}
          onClose={closeImagePreview}
        />
      ) : null}
    </div>
  );
}
