import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import InvoiceDetailPage from "@/app/app/invoices/[invoiceId]/page";
import * as invoicesApi from "@/lib/invoicesApi";
import * as tasksApi from "@/lib/tasksApi";
import type { InvoiceDto } from "@/lib/types";

jest.mock("@/lib/invoicesApi");
jest.mock("@/lib/tasksApi");

const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/invoices/inv-1",
  useParams: () => ({ invoiceId: "inv-1" }),
}));

const mockedInvoicesApi = invoicesApi as jest.Mocked<typeof invoicesApi>;
const mockedTasksApi = tasksApi as jest.Mocked<typeof tasksApi>;

const mockInvoice: InvoiceDto = {
  id: "inv-1",
  invoiceNumber: "INV-1",
  status: "DRAFT",
  issuedAt: "2024-01-01T00:00:00Z",
  sentAt: null,
  dueAt: null,
  paidAt: null,
  total: 500,
  notes: null,
  jobId: "job-1",
  estimateId: "est-1",
  items: [{ id: "it-1", name: "Labor", description: null, quantity: 1, unitPrice: 500, lineTotal: 500 }],
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("InvoiceDetailPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedInvoicesApi.getInvoice.mockResolvedValue(mockInvoice);
    mockedTasksApi.createTask.mockResolvedValue({
      taskId: "task-1",
      title: "Follow up",
      status: "TODO",
      priority: "MEDIUM",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });
  });

  it("generates share link, copies, and preview opens public invoice", async () => {
    mockedInvoicesApi.shareInvoice.mockResolvedValue({
      token: "tok-abc",
      expiresAt: "2026-03-15T00:00:00Z",
    });
    mockedInvoicesApi.buildPublicInvoiceUrl.mockReturnValue("http://localhost:3000/invoice/tok-abc");

    const writeTextMock = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: writeTextMock },
      configurable: true,
    });
    const openMock = jest.fn();
    Object.defineProperty(window, "open", { value: openMock, configurable: true });

    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Invoice INV-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Generate link/i }));

    await waitFor(() => {
      expect(mockedInvoicesApi.shareInvoice).toHaveBeenCalledWith(
        expect.anything(),
        "inv-1",
        expect.objectContaining({ expiresInDays: 14 })
      );
    });

    expect(writeTextMock).toHaveBeenCalledWith("http://localhost:3000/invoice/tok-abc");
    expect(screen.getByText("Link copied")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^Copy$/i })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Copy link/i }));
    await waitFor(() => {
      expect(writeTextMock).toHaveBeenCalledTimes(2);
    });

    fireEvent.click(screen.getByTestId("invoice-share-next-step-preview"));
    expect(openMock).toHaveBeenCalledWith(
      "http://localhost:3000/invoice/tok-abc",
      "_blank",
      "noopener,noreferrer"
    );
  });
});
