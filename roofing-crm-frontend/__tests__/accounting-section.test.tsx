import React from "react";
import userEvent from "@testing-library/user-event";
import { render, screen, waitFor } from "./test-utils";
import { AccountingSection } from "@/components/AccountingSection";
import * as accountingApi from "@/lib/accountingApi";

jest.mock("@/lib/accountingApi");

const mockedAccountingApi = accountingApi as jest.Mocked<typeof accountingApi>;

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
  });

  it("shows empty state when there are no costs", async () => {
    mockedAccountingApi.listJobCostEntries.mockResolvedValueOnce([]);

    render(<AccountingSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByText("No costs recorded yet.")).toBeInTheDocument();
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
});
