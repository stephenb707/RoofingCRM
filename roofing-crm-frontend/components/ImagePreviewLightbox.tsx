"use client";

import { useEffect } from "react";

export interface ImagePreviewLightboxProps {
  url: string;
  alt: string;
  onClose: () => void;
  backdropTestId?: string;
  closeButtonTestId?: string;
}

export function ImagePreviewLightbox({
  url,
  alt,
  onClose,
  backdropTestId = "image-preview-lightbox",
  closeButtonTestId = "image-preview-close",
}: ImagePreviewLightboxProps) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose]);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Image preview"
      data-testid={backdropTestId}
      className="fixed inset-0 z-[60] flex items-center justify-center bg-black/70 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <button
        type="button"
        aria-label="Close image preview"
        data-testid={closeButtonTestId}
        className="absolute right-5 top-5 flex h-10 w-10 items-center justify-center rounded-full border border-white/30 bg-white/10 text-xl font-medium text-white shadow-lg backdrop-blur-md hover:bg-white/20"
        onClick={(e) => {
          e.stopPropagation();
          onClose();
        }}
      >
        ×
      </button>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={url}
        alt={alt}
        className="max-h-[min(90vh,100%)] max-w-[min(95vw,100%)] object-contain shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      />
    </div>
  );
}
