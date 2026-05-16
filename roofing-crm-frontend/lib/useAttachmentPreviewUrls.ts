import type { AxiosInstance } from "axios";
import { useEffect, useRef, useState } from "react";
import { downloadAttachment } from "@/lib/attachmentsApi";

/** Minimal attachment identity needed for preview fetching */
export interface AttachmentPreviewRow {
  id: string;
}

export interface UseAttachmentPreviewUrlsOptions {
  /**
   * When true (default), aborted downloads remove placeholder ref entries so the next effect can retry.
   * Matches Lead/Job attachment previews.
   */
  revokeStalePlaceholderKeysOnCleanup?: boolean;
}

/**
 * Downloads preview blobs for image attachments, builds object URLs, tracks loading ids,
 * revokes blobs when an attachment leaves the list or on unmount, and avoids duplicate in-flight fetches.
 */
export function useAttachmentPreviewUrls(
  api: AxiosInstance,
  previewRows: AttachmentPreviewRow[],
  options?: UseAttachmentPreviewUrlsOptions
): {
  previewUrls: Record<string, string>;
  loadingAttachmentPreviewIds: string[];
} {
  const revokeStalePlaceholderKeysOnCleanup = options?.revokeStalePlaceholderKeysOnCleanup ?? true;

  const [previewUrls, setPreviewUrls] = useState<Record<string, string>>({});
  const [loadingAttachmentPreviewIds, setLoadingAttachmentPreviewIds] = useState<string[]>([]);
  const previewUrlRef = useRef<Record<string, string>>({});
  const inFlightRef = useRef(new Set<string>());

  useEffect(() => {
    return () => {
      if (typeof URL.revokeObjectURL !== "function") {
        return;
      }
      for (const url of Object.values(previewUrlRef.current)) {
        if (url && url !== "") {
          URL.revokeObjectURL(url);
        }
      }
      previewUrlRef.current = {};
      inFlightRef.current.clear();
    };
  }, []);

  useEffect(() => {
    if (typeof URL.createObjectURL !== "function") {
      return;
    }

    const validIds = new Set(previewRows.map((r) => r.id));

    const removedIds: string[] = [];
    for (const id of Object.keys(previewUrlRef.current)) {
      if (!validIds.has(id)) {
        removedIds.push(id);
      }
    }
    for (const id of removedIds) {
      const url = previewUrlRef.current[id];
      if (url && url !== "" && typeof URL.revokeObjectURL === "function") {
        URL.revokeObjectURL(url);
      }
      delete previewUrlRef.current[id];
      inFlightRef.current.delete(id);
    }

    if (removedIds.length > 0) {
      setPreviewUrls((prev) => {
        let changed = false;
        const next = { ...prev };
        for (const id of removedIds) {
          if (Object.prototype.hasOwnProperty.call(next, id)) {
            delete next[id];
            changed = true;
          }
        }
        return changed ? next : prev;
      });
      setLoadingAttachmentPreviewIds((prev) => {
        const next = prev.filter((id) => validIds.has(id));
        return next.length === prev.length && next.every((id, i) => id === prev[i]) ? prev : next;
      });
    }

    const missing = previewRows.filter((attachment) => {
      if (inFlightRef.current.has(attachment.id)) {
        return false;
      }
      const u = previewUrlRef.current[attachment.id];
      if (u !== undefined && u !== "") {
        return false;
      }
      return true;
    });

    if (missing.length === 0) {
      return;
    }

    let cancelled = false;
    const ids = missing.map((attachment) => attachment.id);
    for (const attachment of missing) {
      previewUrlRef.current[attachment.id] = "";
      inFlightRef.current.add(attachment.id);
    }
    setLoadingAttachmentPreviewIds((prev) => Array.from(new Set([...prev, ...ids])));

    void Promise.all(
      missing.map(async (attachment) => {
        try {
          const blob = await downloadAttachment(api, attachment.id);
          if (cancelled) {
            return;
          }
          if (!(blob instanceof Blob)) {
            previewUrlRef.current[attachment.id] = "";
            setPreviewUrls((prev) => ({ ...prev, [attachment.id]: "" }));
            return;
          }
          const url = URL.createObjectURL(blob);
          previewUrlRef.current[attachment.id] = url;
          setPreviewUrls((prev) => ({ ...prev, [attachment.id]: url }));
        } catch {
          if (!cancelled) {
            previewUrlRef.current[attachment.id] = "";
            setPreviewUrls((prev) => ({ ...prev, [attachment.id]: "" }));
          }
        } finally {
          inFlightRef.current.delete(attachment.id);
          if (!cancelled) {
            setLoadingAttachmentPreviewIds((prev) => prev.filter((id) => id !== attachment.id));
          }
        }
      })
    );

    return () => {
      cancelled = true;
      if (revokeStalePlaceholderKeysOnCleanup) {
        for (const attachment of missing) {
          if (previewUrlRef.current[attachment.id] === "") {
            delete previewUrlRef.current[attachment.id];
          }
          inFlightRef.current.delete(attachment.id);
        }
      } else {
        for (const attachment of missing) {
          inFlightRef.current.delete(attachment.id);
        }
      }
    };
  }, [api, previewRows, revokeStalePlaceholderKeysOnCleanup]);

  return { previewUrls, loadingAttachmentPreviewIds };
}
