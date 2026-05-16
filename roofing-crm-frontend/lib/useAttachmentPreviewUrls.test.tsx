import { renderHook, waitFor } from "@testing-library/react";
import type { AxiosInstance } from "axios";
import { useAttachmentPreviewUrls } from "./useAttachmentPreviewUrls";

jest.mock("@/lib/attachmentsApi", () => ({
  downloadAttachment: jest.fn(),
}));

const { downloadAttachment } = jest.requireMock("@/lib/attachmentsApi") as {
  downloadAttachment: jest.Mock;
};

const mockApi = {} as AxiosInstance;

describe("useAttachmentPreviewUrls", () => {
  let urlSerial: number;
  const createObjectUrls: string[] = [];

  beforeEach(() => {
    jest.clearAllMocks();
    createObjectUrls.length = 0;
    urlSerial = 0;
    global.URL.createObjectURL = jest.fn((blob: Blob) => {
      const url = `blob:mock-${++urlSerial}`;
      createObjectUrls.push(url);
      return url;
    });
    global.URL.revokeObjectURL = jest.fn();
  });

  it("creates an object URL for an image attachment after download", async () => {
    const blob = new Blob(["x"], { type: "image/png" });
    downloadAttachment.mockResolvedValueOnce(blob);

    const rows = [{ id: "a1" }];
    const { result } = renderHook(() => useAttachmentPreviewUrls(mockApi, rows));

    await waitFor(() => expect(result.current.previewUrls["a1"]).toMatch(/^blob:mock-/));
    expect(downloadAttachment).toHaveBeenCalledWith(mockApi, "a1");
    expect(URL.createObjectURL).toHaveBeenCalledWith(blob);
  });

  it("revokes object URLs on unmount", async () => {
    const blob = new Blob(["x"], { type: "image/png" });
    downloadAttachment.mockResolvedValueOnce(blob);

    const rows = [{ id: "a1" }];
    const { result, unmount } = renderHook(() => useAttachmentPreviewUrls(mockApi, rows));

    await waitFor(() => expect(result.current.previewUrls["a1"]).toMatch(/^blob:mock-/));
    const created = createObjectUrls[0];
    unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith(created);
  });

  it("revokes object URL when attachment is removed from the list", async () => {
    downloadAttachment
      .mockResolvedValueOnce(new Blob(["k"], { type: "image/png" }))
      .mockResolvedValueOnce(new Blob(["d"], { type: "image/png" }));

    const { result, rerender } = renderHook(
      ({ rows }) => useAttachmentPreviewUrls(mockApi, rows),
      { initialProps: { rows: [{ id: "keep" }, { id: "drop" }] } }
    );

    await waitFor(() => expect(result.current.previewUrls["keep"]).toMatch(/^blob:mock-/));
    await waitFor(() => expect(result.current.previewUrls["drop"]).toMatch(/^blob:mock-/));

    const droppedUrl = result.current.previewUrls["drop"];
    expect(droppedUrl).toMatch(/^blob:mock-/);

    rerender({ rows: [{ id: "keep" }] });

    await waitFor(() => expect(URL.revokeObjectURL).toHaveBeenCalledWith(droppedUrl));
    expect(result.current.previewUrls["drop"]).toBeUndefined();
    expect(result.current.previewUrls["keep"]).toBeTruthy();
  });

  it("does not re-download when the same attachment id stays in the list", async () => {
    const blob = new Blob(["x"], { type: "image/png" });
    downloadAttachment.mockResolvedValue(blob);

    const { result, rerender } = renderHook(
      ({ rows }) => useAttachmentPreviewUrls(mockApi, rows),
      { initialProps: { rows: [{ id: "a1" }] } }
    );

    await waitFor(() => expect(result.current.previewUrls["a1"]).toMatch(/^blob:mock-/));
    const firstUrl = result.current.previewUrls["a1"];
    expect(downloadAttachment).toHaveBeenCalledTimes(1);

    rerender({ rows: [{ id: "a1" }] });

    expect(result.current.previewUrls["a1"]).toBe(firstUrl);
    expect(downloadAttachment).toHaveBeenCalledTimes(1);
    expect(URL.revokeObjectURL).not.toHaveBeenCalled();
  });

  it("does not create a preview URL when download returns a non-Blob (e.g. bad response)", async () => {
    downloadAttachment.mockResolvedValueOnce(null as unknown as Blob);

    const rows = [{ id: "bad" }];
    const { result } = renderHook(() => useAttachmentPreviewUrls(mockApi, rows));

    await waitFor(() => expect(downloadAttachment).toHaveBeenCalled());
    expect(URL.createObjectURL).not.toHaveBeenCalled();
    expect(result.current.previewUrls["bad"]).toBe("");
  });

  it("retries after a failed download when preview rows update", async () => {
    downloadAttachment.mockRejectedValueOnce(new Error("network"));
    downloadAttachment.mockResolvedValueOnce(new Blob(["z"], { type: "image/png" }));

    const { result, rerender } = renderHook(
      ({ rows }) => useAttachmentPreviewUrls(mockApi, rows),
      { initialProps: { rows: [{ id: "r1" }] } }
    );

    await waitFor(() => expect(downloadAttachment).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(result.current.previewUrls["r1"]).toBe(""));

    rerender({ rows: [{ id: "r1" }] });

    await waitFor(() => expect(downloadAttachment).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(result.current.previewUrls["r1"]).toMatch(/^blob:mock-/));
  });
});
