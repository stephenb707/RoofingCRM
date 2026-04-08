import type { AxiosInstance } from "axios";
import {
  confirmReceiptCost,
  extractReceiptDetails,
  getReceiptExtraction,
  uploadJobReceipt,
} from "./accountingApi";
import type { JobReceiptDto } from "./types";

describe("accountingApi", () => {
  const mockApi = {
    get: jest.fn(),
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

  it("triggers receipt extraction with the authenticated api client", async () => {
    (mockApi.post as jest.Mock).mockResolvedValue({
      data: {
        receiptId: "receipt-1",
        status: "COMPLETED",
        extractedAt: "2024-01-01T00:00:00Z",
        error: null,
        confidence: 88,
        result: {
          vendorName: "ABC Supply",
          amount: 98.76,
          suggestedCategory: "MATERIAL",
        },
      },
    });

    await extractReceiptDetails(mockApi, "job-1", "receipt-1");

    expect(mockApi.post).toHaveBeenCalledWith("/api/v1/jobs/job-1/receipts/receipt-1/extract");
    expect((mockApi.post as jest.Mock).mock.calls[0]).toHaveLength(1);
  });

  it("loads the latest stored extraction result", async () => {
    (mockApi.get as jest.Mock).mockResolvedValue({
      data: {
        receiptId: "receipt-1",
        status: "COMPLETED",
        result: { vendorName: "ABC Supply" },
      },
    });

    await getReceiptExtraction(mockApi, "job-1", "receipt-1");

    expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs/job-1/receipts/receipt-1/extraction");
  });

  it("confirms a reviewed receipt into a cost entry", async () => {
    (mockApi.post as jest.Mock).mockResolvedValue({
      data: {
        id: "cost-1",
        jobId: "job-1",
        category: "MATERIAL",
        description: "Receipt from ABC Supply",
        amount: 98.76,
        incurredAt: "2024-01-01T12:00:00Z",
      },
    });

    await confirmReceiptCost(mockApi, "job-1", "receipt-1", {
      category: "MATERIAL",
      vendorName: "ABC Supply",
      description: "Receipt from ABC Supply",
      amount: 98.76,
      incurredAt: "2024-01-01T12:00:00Z",
      notes: "Total visible on receipt",
    });

    expect(mockApi.post).toHaveBeenCalledWith(
      "/api/v1/jobs/job-1/receipts/receipt-1/confirm-cost",
      expect.objectContaining({
        description: "Receipt from ABC Supply",
        amount: 98.76,
      })
    );
  });
});
