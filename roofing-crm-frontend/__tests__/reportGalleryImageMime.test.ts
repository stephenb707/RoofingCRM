import { reportGalleryPrimaryMime, supportsReportGalleryImage } from "@/lib/reportGalleryImageMime";

describe("reportGalleryImageMime", () => {
  it("parses MIME primary type without parameters", () => {
    expect(reportGalleryPrimaryMime(" Image/JPEG ; charset=UTF-8 ")).toBe("image/jpeg");
  });

  it("accepts common raster types used in gallery reports", () => {
    expect(supportsReportGalleryImage("image/webp")).toBe(true);
    expect(supportsReportGalleryImage("image/gif")).toBe(true);
    expect(supportsReportGalleryImage("image/tiff")).toBe(true);
    expect(supportsReportGalleryImage("image/x-tiff")).toBe(true);
    expect(supportsReportGalleryImage("image/bmp")).toBe(true);
  });

  it("rejects types we do not embed or preview as report photos", () => {
    expect(supportsReportGalleryImage("image/svg+xml")).toBe(false);
    expect(supportsReportGalleryImage("image/heic")).toBe(false);
    expect(supportsReportGalleryImage("application/pdf")).toBe(false);
  });
});
