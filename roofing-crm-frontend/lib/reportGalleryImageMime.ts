/** MIME allowlist aligned with backend `ReportGalleryImageMime` — raster images for reports & thumbnails. */

const ALLOWED_PRIMARY = new Set([
  "image/png",
  "image/jpeg",
  "image/jpg",
  "image/pjpeg",
  "image/gif",
  "image/webp",
  "image/bmp",
  "image/x-ms-bmp",
  "image/tiff",
  "image/tif",
  "image/x-tiff",
]);

export function reportGalleryPrimaryMime(contentType: string | null | undefined): string {
  if (contentType == null || typeof contentType !== "string") {
    return "";
  }
  const trimmed = contentType.trim();
  if (!trimmed) {
    return "";
  }
  const semi = trimmed.indexOf(";");
  const primary = semi >= 0 ? trimmed.slice(0, semi) : trimmed;
  return primary.trim().toLowerCase();
}

export function supportsReportGalleryImage(contentType: string | null | undefined): boolean {
  return ALLOWED_PRIMARY.has(reportGalleryPrimaryMime(contentType));
}
