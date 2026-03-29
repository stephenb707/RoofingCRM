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

    expect(screen.getByTestId("category-totals-grid")).toHaveClass("xl:grid-cols-4");
    expect(screen.getByTestId("category-totals-grid")).toHaveClass("sm:grid-cols-2");
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

  it("creates a cost from a receipt", async () => {
    const user = userEvent.setup();
    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /create cost/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /create cost/i }));
    await user.type(screen.getByLabelText(/amount/i), "123.45");
    await user.clear(screen.getByLabelText(/description/i));
    await user.type(screen.getByLabelText(/description/i), "Receipt materials");
    await user.click(screen.getAllByRole("button", { name: /^create cost$/i })[1]);

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
