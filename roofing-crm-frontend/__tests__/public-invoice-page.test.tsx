import React from "react";
import { render, screen, waitFor } from "./test-utils";
import PublicInvoicePage from "@/app/invoice/[token]/page";
import * as invoicesApi from "@/lib/invoicesApi";
import type { PublicInvoiceDto } from "@/lib/types";

jest.mock("@/lib/invoicesApi");
jest.mock("next/navigation", () => ({
  useParams: () => ({ token: "inv-token-abc" }),
}));

const mockedInvoicesApi = invoicesApi as jest.Mocked<typeof invoicesApi>;

const mockPublicInvoice: PublicInvoiceDto = {
  invoiceNumber: "INV-2001",
  status: "SENT",
  issuedAt: "2024-01-01T12:00:00Z",
  dueAt: "2024-02-01T12:00:00Z",
  sentAt: "2024-01-02T12:00:00Z",
  total: 1200,
  notes: null,
  publicExpiresAt: "2026-02-16T00:00:00Z",
  companyName: "Acme Roofing Co",
  customerName: "Jane Doe",
  customerAddress: "123 Main St, Chicago, IL",
  items: [
    { name: "Labor", description: null, quantity: 1, unitPrice: 1200, unit: null, lineTotal: 1200 },
  ],
};

describe("PublicInvoicePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedInvoicesApi.getPublicInvoice.mockResolvedValue(mockPublicInvoice);
  });

  it("renders invoice and company name in header", async () => {
    render(<PublicInvoicePage />);

    await waitFor(() => {
      expect(screen.getByText(/INV-2001/)).toBeInTheDocument();
    });

    expect(screen.getByTestId("public-invoice-company")).toHaveTextContent("Acme Roofing Co");
    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    expect(screen.getByText("Labor")).toBeInTheDocument();
  });
});
