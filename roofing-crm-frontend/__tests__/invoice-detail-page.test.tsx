import React from "react";
import { render, screen, waitFor, fireEvent, within } from "./test-utils";
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
  customerName: "Jane Doe",
  customerEmail: "jane@example.com",
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

  it("Share section shows Send email as the primary action before link actions", async () => {
    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Invoice INV-1")).toBeInTheDocument();
    });

    expect(screen.getByText(/send by email for the fastest delivery/i)).toBeInTheDocument();
    const sendBtn = screen.getByTestId("invoice-share-send-email");
    const linkBtn = screen.getByTestId("invoice-share-generate-or-copy-link");
    expect(sendBtn.compareDocumentPosition(linkBtn) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(sendBtn.className).toMatch(/bg-sky-600/);
    expect(linkBtn.className).toMatch(/border-sky-300/);
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

  it("opens send-email modal and submits invoice email request", async () => {
    mockedInvoicesApi.sendInvoiceEmail.mockResolvedValue({
      success: true,
      sentAt: "2026-03-15T00:00:00Z",
      publicUrl: "http://localhost:3000/invoice/tok-abc",
      reusedExistingToken: true,
    });

    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Invoice INV-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Send email/i }));
    const dialog = screen.getByRole("dialog", { name: /send invoice by email/i });
    fireEvent.change(within(dialog).getByLabelText(/recipient email/i), {
      target: { value: "customer@example.com" },
    });
    fireEvent.change(within(dialog).getByLabelText(/recipient name/i), {
      target: { value: "Jane" },
    });
    fireEvent.change(within(dialog).getByLabelText(/^message \(optional\)$/i), {
      target: { value: "Please review this invoice." },
    });
    fireEvent.click(within(dialog).getByRole("button", { name: /^Send email$/i }));

    await waitFor(() => {
      expect(mockedInvoicesApi.sendInvoiceEmail).toHaveBeenCalledWith(
        expect.anything(),
        "inv-1",
        expect.objectContaining({
          recipientEmail: "customer@example.com",
          recipientName: "Jane",
          message: "Please review this invoice.",
          expiresInDays: 14,
        })
      );
    });

    expect(screen.getByText("Email sent to customer@example.com.")).toBeInTheDocument();
  });

  it("prefills invoice send-email modal from customer and keeps fields editable", async () => {
    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Invoice INV-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Send email/i }));
    const dialog = screen.getByRole("dialog", { name: /send invoice by email/i });
    const emailInput = within(dialog).getByLabelText(/recipient email/i) as HTMLInputElement;
    const nameInput = within(dialog).getByLabelText(/recipient name/i) as HTMLInputElement;

    expect(emailInput.value).toBe("jane@example.com");
    expect(nameInput.value).toBe("Jane Doe");

    fireEvent.change(emailInput, { target: { value: "edited@example.com" } });
    fireEvent.change(nameInput, { target: { value: "Edited Name" } });

    expect(emailInput.value).toBe("edited@example.com");
    expect(nameInput.value).toBe("Edited Name");
  });

  it("leaves invoice send-email modal fields blank when customer info is missing", async () => {
    mockedInvoicesApi.getInvoice.mockResolvedValue({
      ...mockInvoice,
      customerName: null,
      customerEmail: null,
    });

    render(<InvoiceDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Invoice INV-1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Send email/i }));
    const dialog = screen.getByRole("dialog", { name: /send invoice by email/i });

    expect((within(dialog).getByLabelText(/recipient email/i) as HTMLInputElement).value).toBe("");
    expect((within(dialog).getByLabelText(/recipient name/i) as HTMLInputElement).value).toBe("");
  });
});
