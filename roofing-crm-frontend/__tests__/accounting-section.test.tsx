import React from "react";
import userEvent from "@testing-library/user-event";
import { render, screen, waitFor, within } from "./test-utils";
import { AccountingSection } from "@/components/AccountingSection";
import * as accountingApi from "@/lib/accountingApi";
import * as attachmentsApi from "@/lib/attachmentsApi";

jest.mock("@/lib/accountingApi");
jest.mock("@/lib/attachmentsApi");

const mockedAccountingApi = accountingApi as jest.Mocked<typeof accountingApi>;
const mockedAttachmentsApi = attachmentsApi as jest.Mocked<typeof attachmentsApi>;

const summaryResponse = {
  agreedAmount: 12000,
  invoicedAmount: 9500,
  paidAmount: 8000,
  totalCosts: 5500,
  grossProfit: 2500,
  marginPercent: 31.25,
  projectedProfit: 6500,
  actualProfit: 2500,
  projectedMarginPercent: 54.17,
  actualMarginPercent: 31.25,
  categoryTotals: {
    MATERIAL: 3500,
    TRANSPORTATION: 500,
    LABOR: 1200,
    OTHER: 300,
  },
  hasAcceptedEstimate: true,
} as const;

describe("AccountingSection", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedAccountingApi.getJobAccountingSummary.mockResolvedValue(summaryResponse);
    mockedAccountingApi.listJobCostEntries.mockResolvedValue([
      {
        id: "cost-1",
        jobId: "job-1",
        category: "MATERIAL",
        vendorName: "ABC Supply",
        description: "Shingles",
        amount: 3500,
        incurredAt: "2026-03-01T12:00:00Z",
        notes: "Delivered to site",
        createdAt: "2026-03-01T12:00:00Z",
        updatedAt: "2026-03-01T12:00:00Z",
      },
      {
        id: "cost-2",
        jobId: "job-1",
        category: "LABOR",
        vendorName: "Crew 7",
        description: "Install crew",
        amount: 1200,
        incurredAt: "2026-03-02T12:00:00Z",
        notes: null,
        createdAt: "2026-03-02T12:00:00Z",
        updatedAt: "2026-03-02T12:00:00Z",
      },
    ]);
    mockedAccountingApi.listJobReceipts.mockResolvedValue([
      {
        id: "receipt-1",
        fileName: "receipt-1.pdf",
        contentType: "application/pdf",
        fileSize: 2048,
        description: "ABC Supply receipt",
        uploadedAt: "2026-03-03T12:00:00Z",
        linkedCostEntryId: null,
        linkedCostEntryDescription: null,
        linkedCostEntryAmount: null,
        extractionStatus: "NOT_STARTED",
        extractedAt: null,
        extractionError: null,
        extractionConfidence: null,
        extractionResult: null,
      },
    ]);
    mockedAccountingApi.createJobCostEntry.mockResolvedValue({
      id: "cost-new",
      jobId: "job-1",
      category: "MATERIAL",
      vendorName: "ABC Supply",
      description: "Starter shingles",
      amount: 450.25,
      incurredAt: "2026-03-10T12:00:00Z",
      notes: "Rush order",
      createdAt: "2026-03-10T12:00:00Z",
      updatedAt: "2026-03-10T12:00:00Z",
    });
    mockedAccountingApi.uploadJobReceipt.mockResolvedValue({
      id: "receipt-2",
      fileName: "receipt-2.pdf",
      contentType: "application/pdf",
      fileSize: 1000,
      description: "Uploaded receipt",
      uploadedAt: "2026-03-11T12:00:00Z",
      linkedCostEntryId: null,
      linkedCostEntryDescription: null,
      linkedCostEntryAmount: null,
      extractionStatus: "NOT_STARTED",
      extractedAt: null,
      extractionError: null,
      extractionConfidence: null,
      extractionResult: null,
    });
    mockedAccountingApi.extractReceiptDetails.mockResolvedValue({
      receiptId: "receipt-1",
      status: "COMPLETED",
      extractedAt: "2026-03-11T12:00:00Z",
      error: null,
      confidence: 92,
      result: {
        vendorName: "ABC Supply",
        incurredAt: "2026-03-10T12:00:00Z",
        amount: 123.45,
        extractedSubtotal: 120.45,
        extractedTax: 3,
        extractedTotal: 123.45,
        extractedTaxRatePercent: 2.5,
        amountCandidates: [123.45, 120.45],
        amountConfidence: "LOW",
        suggestedCategory: "MATERIAL",
        notes: "Total due for materials",
        confidence: 92,
        rawExtractedText: "SUBTOTAL 120.45\nTOTAL 123.45",
        extractionWarnings: ["Please review the total before saving. We found multiple possible amounts."],
      },
    });
    mockedAccountingApi.getReceiptExtraction.mockResolvedValue({
      receiptId: "receipt-1",
      status: "COMPLETED",
      extractedAt: "2026-03-11T12:00:00Z",
      error: null,
      confidence: 92,
      result: {
        vendorName: "ABC Supply",
        incurredAt: "2026-03-10T12:00:00Z",
        amount: 123.45,
        extractedSubtotal: 120.45,
        extractedTax: 3,
        extractedTotal: 123.45,
        extractedTaxRatePercent: 2.5,
        amountCandidates: [123.45, 120.45],
        amountConfidence: "LOW",
        suggestedCategory: "MATERIAL",
        notes: "Total due for materials",
        confidence: 92,
        rawExtractedText: "SUBTOTAL 120.45\nTOTAL 123.45",
        extractionWarnings: ["Please review the total before saving. We found multiple possible amounts."],
      },
    });
    mockedAccountingApi.createCostFromReceipt.mockResolvedValue({
      id: "cost-from-receipt",
      jobId: "job-1",
      category: "MATERIAL",
      vendorName: "ABC Supply",
      description: "Receipt materials",
      amount: 123.45,
      incurredAt: "2026-03-10T12:00:00Z",
      notes: null,
      createdAt: "2026-03-10T12:00:00Z",
      updatedAt: "2026-03-10T12:00:00Z",
    });
    mockedAccountingApi.confirmReceiptCost.mockResolvedValue({
      id: "cost-from-extraction",
      jobId: "job-1",
      category: "MATERIAL",
      vendorName: "ABC Supply",
      description: "Receipt from ABC Supply",
      amount: 123.45,
      incurredAt: "2026-03-10T12:00:00Z",
      notes: "Total due for materials",
      createdAt: "2026-03-10T12:00:00Z",
      updatedAt: "2026-03-10T12:00:00Z",
    });
    mockedAccountingApi.linkReceiptToCost.mockResolvedValue({
      id: "receipt-1",
      fileName: "receipt-1.pdf",
      contentType: "application/pdf",
      fileSize: 2048,
      description: "ABC Supply receipt",
      uploadedAt: "2026-03-03T12:00:00Z",
      linkedCostEntryId: "cost-1",
      linkedCostEntryDescription: "Shingles",
      linkedCostEntryAmount: 3500,
      extractionStatus: "NOT_STARTED",
      extractedAt: null,
      extractionError: null,
      extractionConfidence: null,
      extractionResult: null,
    });
    mockedAccountingApi.unlinkReceiptFromCost.mockResolvedValue({
      id: "receipt-1",
      fileName: "receipt-1.pdf",
      contentType: "application/pdf",
      fileSize: 2048,
      description: "ABC Supply receipt",
      uploadedAt: "2026-03-03T12:00:00Z",
      linkedCostEntryId: null,
      linkedCostEntryDescription: null,
      linkedCostEntryAmount: null,
      extractionStatus: "NOT_STARTED",
      extractedAt: null,
      extractionError: null,
      extractionConfidence: null,
      extractionResult: null,
    });
    mockedAccountingApi.deleteJobReceipt.mockResolvedValue(undefined);
    mockedAttachmentsApi.downloadAttachment.mockResolvedValue(new Blob(["pdf"], { type: "application/pdf" }));
  });

  it("renders summary values from API data", async () => {
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByText("$12,000.00")).toBeInTheDocument();
    });

    expect(screen.getByText("$12,000.00")).toBeInTheDocument();
    expect(screen.getByText("$9,500.00")).toBeInTheDocument();
    expect(screen.getByText("$8,000.00")).toBeInTheDocument();
    expect(screen.getByText("$5,500.00")).toBeInTheDocument();
    expect(screen.getByText("$2,500.00")).toBeInTheDocument();
    expect(screen.getByText("31.3%")).toBeInTheDocument();
    expect(screen.getByText("Projected $6,500.00")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /receipts/i })).toBeInTheDocument();
    expect(screen.getByText("Not extracted")).toBeInTheDocument();
  });

  it("shows empty state when there are no costs", async () => {
    mockedAccountingApi.listJobCostEntries.mockResolvedValueOnce([]);

    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByText("No costs recorded yet.")).toBeInTheDocument();
    });
  });

  it("shows the transportation card in the wider responsive grid", async () => {
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(within(screen.getByTestId("category-totals-grid")).getByText("Transportation")).toBeInTheDocument();
    });

    expect(screen.getByTestId("category-totals-grid")).toHaveClass("sm:grid-cols-2");
    expect(screen.getByTestId("category-totals-grid").className).toContain("auto-fill");
  });

  it("uploads a receipt and refreshes the list", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /upload receipt/i })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/receipt label/i), "Home Depot receipt");
    const fileInput = screen.getByLabelText("Choose receipt file");
    const file = new File(["content"], "receipt.pdf", { type: "application/pdf" });

    await user.upload(fileInput, file);

    await waitFor(() => {
      expect(mockedAccountingApi.uploadJobReceipt).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        file,
        "Home Depot receipt"
      );
    });
  });

  it("adds a cost entry from the modal", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /add cost/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /^add cost$/i }));
    await user.type(screen.getByLabelText(/vendor name/i), "ABC Supply");
    await user.type(screen.getByLabelText(/description/i), "Starter shingles");
    await user.type(screen.getByLabelText(/amount/i), "450.25");
    await user.type(screen.getByLabelText(/notes/i), "Rush order");
    await user.click(screen.getAllByRole("button", { name: /^add cost$/i })[1]);

    await waitFor(() => {
      expect(mockedAccountingApi.createJobCostEntry).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        expect.objectContaining({
          category: "MATERIAL",
          vendorName: "ABC Supply",
          description: "Starter shingles",
          amount: 450.25,
          notes: "Rush order",
          incurredAt: expect.stringMatching(/T12:00:00Z$/),
        })
      );
    });
  });

  it("creates a cost from a receipt manually", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /create cost manually/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /create cost manually/i }));
    await user.type(screen.getByLabelText(/amount/i), "123.45");
    await user.clear(screen.getByLabelText(/description/i));
    await user.type(screen.getByLabelText(/description/i), "Receipt materials");
    await user.click(screen.getByRole("button", { name: /^create cost$/i }));

    await waitFor(() => {
      expect(mockedAccountingApi.createCostFromReceipt).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        "receipt-1",
        expect.objectContaining({
          description: "Receipt materials",
          amount: 123.45,
        })
      );
    });
  });

  it("extracts receipt details and opens the review modal with prefilled values", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /extract details/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /extract details/i }));

    await waitFor(() => {
      expect(mockedAccountingApi.extractReceiptDetails).toHaveBeenCalledWith(expect.anything(), "job-1", "receipt-1");
    });

    expect(await screen.findByRole("heading", { name: /review extracted receipt/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/vendor name/i)).toHaveValue("ABC Supply");
    expect(screen.getByLabelText(/description/i)).toHaveValue("Receipt from ABC Supply");
    expect(screen.getByLabelText(/amount/i)).toHaveValue(123.45);
    expect(screen.getByLabelText(/notes/i)).toHaveValue("Total due for materials");
    expect(screen.getByText("Please review the total before saving. We found multiple possible amounts.")).toBeInTheDocument();
    expect(screen.queryByText("Amount candidates")).not.toBeInTheDocument();
    expect(screen.queryByText("Reconciled totals")).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Tax total \(reference\)/i)).not.toBeInTheDocument();
    expect(screen.queryByTestId("receipt-suggestions")).not.toBeInTheDocument();
    expect(screen.queryByTestId("toggle-detected-text")).not.toBeInTheDocument();
  });

  it("does not surface backend reconciliation warnings in the review modal", async () => {
    const user = userEvent.setup();
    mockedAccountingApi.extractReceiptDetails.mockResolvedValueOnce({
      receiptId: "receipt-1",
      status: "COMPLETED",
      extractedAt: "2026-03-11T12:00:00Z",
      error: null,
      confidence: 92,
      result: {
        vendorName: "ABC Supply",
        incurredAt: "2026-03-10T12:00:00Z",
        amount: 1564.38,
        extractedSubtotal: 1455.24,
        extractedTax: 109.14,
        extractedTotal: 1564.38,
        amountConfidence: "HIGH",
        suggestedCategory: "MATERIAL",
        notes: "OK",
        confidence: 92,
        extractionWarnings: [
          "Tax total was derived from subtotal and tax rate.",
          "Total was derived from subtotal and tax.",
          "Amount paid was aligned to the reconciled total.",
        ],
      },
    });

    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /extract details/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /extract details/i }));
    await screen.findByRole("heading", { name: /review extracted receipt/i });

    expect(screen.queryByText(/Tax total was derived from subtotal and tax rate/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Total was derived from subtotal and tax/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Amount paid was aligned/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Please review the total before saving/i)).not.toBeInTheDocument();
  });

  it("lets the user edit extracted fields before confirming the cost", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /extract details/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /extract details/i }));
    await screen.findByRole("heading", { name: /review extracted receipt/i });

    await user.clear(screen.getByLabelText(/description/i));
    await user.type(screen.getByLabelText(/description/i), "Reviewed receipt expense");
    await user.clear(screen.getByLabelText(/amount/i));
    await user.type(screen.getByLabelText(/amount/i), "129.99");
    await user.click(screen.getByRole("button", { name: /review & save cost/i }));

    await waitFor(() => {
      expect(mockedAccountingApi.confirmReceiptCost).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        "receipt-1",
        expect.objectContaining({
          description: "Reviewed receipt expense",
          amount: 129.99,
        })
      );
    });
  });

  it("shows a failed extraction message and still allows manual entry", async () => {
    const user = userEvent.setup();
    mockedAccountingApi.extractReceiptDetails.mockResolvedValueOnce({
      receiptId: "receipt-1",
      status: "FAILED",
      extractedAt: "2026-03-11T12:00:00Z",
      error: "We couldn't reliably extract details from this receipt. You can retry or enter it manually.",
      confidence: null,
      result: null,
    });

    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /extract details/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /extract details/i }));

    expect(
      await screen.findByText("We couldn't reliably extract details from this receipt. You can retry or enter it manually.")
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /create cost manually/i })).toBeInTheDocument();
  });

  it("shows a review cue when overall extraction confidence is low", async () => {
    const user = userEvent.setup();
    mockedAccountingApi.extractReceiptDetails.mockResolvedValueOnce({
      receiptId: "receipt-1",
      status: "COMPLETED",
      extractedAt: "2026-03-11T12:00:00Z",
      error: null,
      confidence: 40,
      result: {
        vendorName: "Vendor",
        incurredAt: "2026-03-10T12:00:00Z",
        amount: 100,
        extractedTax: 5,
        extractedTotal: 105,
        amountConfidence: "HIGH",
        suggestedCategory: "MATERIAL",
        notes: "",
        confidence: 40,
        extractionWarnings: [],
      },
    });

    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /extract details/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /extract details/i }));
    await screen.findByRole("heading", { name: /review extracted receipt/i });

    expect(screen.getAllByTestId("review-cue").length).toBeGreaterThan(0);
  });

  it("filters entries by category", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByText("Shingles")).toBeInTheDocument();
      expect(screen.getByText("Install crew")).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /^Labor$/i }));

    await waitFor(() => {
      expect(screen.queryByText("Shingles")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Install crew")).toBeInTheDocument();
  });

  it("links a receipt to an existing cost", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /link to cost/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /link to cost/i }));
    await user.selectOptions(screen.getByLabelText(/cost entry/i), "cost-1");
    await user.click(screen.getByRole("button", { name: /link receipt/i }));

    await waitFor(() => {
      expect(mockedAccountingApi.linkReceiptToCost).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        "receipt-1",
        "cost-1"
      );
    });
  });

  it("unlinks a receipt from an existing cost", async () => {
    const user = userEvent.setup();
    mockedAccountingApi.listJobReceipts.mockResolvedValueOnce([
      {
        id: "receipt-1",
        fileName: "receipt-1.pdf",
        contentType: "application/pdf",
        fileSize: 2048,
        description: "ABC Supply receipt",
        uploadedAt: "2026-03-03T12:00:00Z",
        linkedCostEntryId: "cost-1",
        linkedCostEntryDescription: "Shingles",
        linkedCostEntryAmount: 3500,
      },
    ]);

    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /unlink/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /unlink/i }));

    await waitFor(() => {
      expect(mockedAccountingApi.unlinkReceiptFromCost).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        "receipt-1"
      );
    });
  });
});
