import React from "react";
import userEvent from "@testing-library/user-event";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import { InvoicesSection } from "@/components/InvoicesSection";
import * as invoicesApi from "@/lib/invoicesApi";
import * as estimatesApi from "@/lib/estimatesApi";
import type { InvoiceDto, EstimateDto } from "@/lib/types";

jest.mock("@/lib/invoicesApi");
jest.mock("@/lib/estimatesApi");
const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
}));

const mockedInvoicesApi = invoicesApi as jest.Mocked<typeof invoicesApi>;
const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;

const mockInvoice: InvoiceDto = {
  id: "inv-1",
  invoiceNumber: "INV-1",
  status: "DRAFT",
  total: 5000,
  jobId: "job-1",
  estimateId: "est-1",
  issuedAt: "2026-01-15T00:00:00Z",
  items: [],
};

const mockEstimate: EstimateDto = {
  id: "est-1",
  jobId: "job-1",
  status: "DRAFT",
  title: "Roof Estimate",
  notes: null,
  items: [{ id: "i1", name: "Shingles", quantity: 100, unitPrice: 50 }],
  subtotal: 5000,
  total: 5000,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("InvoicesSection", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockedInvoicesApi.listInvoicesForJob.mockResolvedValue([]);
  });

  it("renders invoices table when data exists", async () => {
    mockedInvoicesApi.listInvoicesForJob.mockResolvedValue([mockInvoice]);

    render(<InvoicesSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByText("INV-1")).toBeInTheDocument();
    });

    expect(screen.getByText("Draft")).toBeInTheDocument();
    expect(screen.getByText("$5,000.00")).toBeInTheDocument();
  });

  it("clicking an invoice row navigates to invoice detail", async () => {
    mockedInvoicesApi.listInvoicesForJob.mockResolvedValue([mockInvoice]);

    render(<InvoicesSection jobId="job-1" />);

    const row = await screen.findByTestId("invoice-row-inv-1");
    fireEvent.click(row);

    expect(mockPush).toHaveBeenCalledWith("/app/invoices/inv-1");
  });

  it("create invoice opens modal and calls API on submit", async () => {
    const user = userEvent.setup();
    mockedEstimatesApi.listEstimatesForJob.mockResolvedValue([mockEstimate]);
    mockedInvoicesApi.createInvoiceFromEstimate.mockResolvedValue({
      ...mockInvoice,
      id: "inv-new",
      invoiceNumber: "INV-2",
    });

    render(<InvoicesSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Create Invoice/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /Create Invoice/i }));

    const select = await screen.findByLabelText(/^Estimate$/i);
    await user.selectOptions(select, "est-1");

    const createBtn = screen.getByRole("button", { name: /^Create$/ });
    await user.click(createBtn);

    await waitFor(() => {
      expect(mockedInvoicesApi.createInvoiceFromEstimate).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ estimateId: "est-1" })
      );
    });
    expect(mockPush).toHaveBeenCalledWith("/app/invoices/inv-new");
  });

  it("status change calls updateInvoiceStatus", async () => {
    mockedInvoicesApi.listInvoicesForJob.mockResolvedValue([{ ...mockInvoice, status: "SENT" }]);
    mockedInvoicesApi.updateInvoiceStatus.mockResolvedValue({
      ...mockInvoice,
      status: "PAID",
    });

    render(<InvoicesSection jobId="job-1" />);

    await waitFor(() => {
      expect(screen.getByText("INV-1")).toBeInTheDocument();
    });

    const markPaidBtn = screen.getByRole("button", { name: /Mark paid/i });
    fireEvent.click(markPaidBtn);

    await waitFor(() => {
      expect(mockedInvoicesApi.updateInvoiceStatus).toHaveBeenCalledWith(
        expect.anything(),
        "inv-1",
        "PAID"
      );
    });
    expect(mockPush).not.toHaveBeenCalled();
  });
});
