import { sanitizeAttachmentIds } from "@/lib/customerPhotoReportPayload";

describe("sanitizeAttachmentIds", () => {
  it("drops null-like and blank strings so JSON never sends invalid UUIDs", () => {
    const bad = [
      "  550e8400-e29b-41d4-a716-446655440000  ",
      "",
      "   ",
      undefined,
      null,
    ] as unknown as string[];
    expect(sanitizeAttachmentIds(bad)).toEqual(["550e8400-e29b-41d4-a716-446655440000"]);
  });

  it("returns empty array when nothing valid", () => {
    expect(sanitizeAttachmentIds(["", " "])).toEqual([]);
  });
});
