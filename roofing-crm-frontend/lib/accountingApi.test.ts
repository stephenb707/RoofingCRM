import type { AxiosInstance } from "axios";
import { uploadJobReceipt } from "./accountingApi";
import type { JobReceiptDto } from "./types";

describe("accountingApi", () => {
  const mockApi = {
    post: jest.fn(),
  } as unknown as AxiosInstance;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("uploadJobReceipt", () => {
    it("uses the provided authenticated api client with FormData", async () => {
      const file = new File(["receipt"], "receipt.jpg", { type: "image/jpeg" });
      const created: JobReceiptDto = {
        id: "receipt-1",
        fileName: "receipt.jpg",
        contentType: "image/jpeg",
        fileSize: 7,
        description: null,
        uploadedAt: "2024-01-01T00:00:00Z",
        linkedCostEntryId: null,
        linkedCostEntryDescription: null,
        linkedCostEntryAmount: null,
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await uploadJobReceipt(mockApi, "job-1", file);

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/jobs/job-1/receipts", expect.any(FormData));
      expect((mockApi.post as jest.Mock).mock.calls[0]).toHaveLength(2);
      const formData = (mockApi.post as jest.Mock).mock.calls[0][1] as FormData;
      expect(formData.get("file")).toBe(file);
      expect(formData.get("description")).toBeNull();
    });

    it("includes description when provided", async () => {
      const file = new File(["receipt"], "receipt.pdf", { type: "application/pdf" });
      const created: JobReceiptDto = {
        id: "receipt-2",
        fileName: "receipt.pdf",
        contentType: "application/pdf",
        fileSize: 12,
        description: "Supplier receipt",
        uploadedAt: "2024-01-01T00:00:00Z",
        linkedCostEntryId: null,
        linkedCostEntryDescription: null,
        linkedCostEntryAmount: null,
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await uploadJobReceipt(mockApi, "job-1", file, "Supplier receipt");

      const formData = (mockApi.post as jest.Mock).mock.calls[0][1] as FormData;
      expect(formData.get("description")).toBe("Supplier receipt");
    });
  });
});
