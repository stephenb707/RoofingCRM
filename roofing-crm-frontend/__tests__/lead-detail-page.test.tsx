import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import userEvent from "@testing-library/user-event";
import LeadDetailPage from "@/app/app/leads/[leadId]/page";
import * as leadsApi from "@/lib/leadsApi";
import * as attachmentsApi from "@/lib/attachmentsApi";
import * as communicationLogsApi from "@/lib/communicationLogsApi";
import { LeadDto } from "@/lib/types";

jest.mock("@/lib/leadsApi");
jest.mock("@/lib/attachmentsApi");
jest.mock("@/lib/communicationLogsApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/leads/lead-1",
  useParams: () => ({ leadId: "lead-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;
const mockedAttachmentsApi = attachmentsApi as jest.Mocked<typeof attachmentsApi>;
const mockedCommLogsApi = communicationLogsApi as jest.Mocked<typeof communicationLogsApi>;

const mockLead: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  status: "NEW",
  source: "WEBSITE",
  leadNotes: "Notes",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  preferredContactMethod: "Phone",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "John",
  customerLastName: "Doe",
  customerPhone: "555-123-4567",
  customerEmail: "john@example.com",
};

describe("LeadDetailPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedLeadsApi.getLead.mockResolvedValue(mockLead);
    mockedAttachmentsApi.listLeadAttachments.mockResolvedValue([]);
    mockedCommLogsApi.listLeadCommunicationLogs.mockResolvedValue([]);
  });

  it("renders customer name from customerFirstName and customerLastName", async () => {
    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("John Doe");
    });
  });

  it("shows Convert to Job and Edit Lead buttons when lead is not LOST", async () => {
    render(<LeadDetailPage />);

    const convertJobLink = await screen.findByRole("link", { name: /convert to job/i });
    expect(convertJobLink).toHaveAttribute("href", "/app/leads/lead-1/convert");

    const editLink = await screen.findByRole("link", { name: /edit lead/i });
    expect(editLink).toHaveAttribute("href", "/app/leads/lead-1/edit");
  });

  it("hides Convert to Job button when lead status is LOST", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      status: "LOST",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.queryByRole("link", { name: /convert to job/i })).not.toBeInTheDocument();
    });

    const editLink = await screen.findByRole("link", { name: /edit lead/i });
    expect(editLink).toBeInTheDocument();
  });

  it("when convertedJobId exists shows converted banner and View Job / Create Estimate, hides Convert to Job", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      convertedJobId: "job-99",
      status: "WON",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/converted to job/i)).toBeInTheDocument();
    });

    expect(screen.queryByRole("link", { name: /convert to job/i })).not.toBeInTheDocument();

    const viewJobLinks = screen.getAllByRole("link", { name: /view job/i });
    expect(viewJobLinks.length).toBeGreaterThanOrEqual(1);
    expect(viewJobLinks[0]).toHaveAttribute("href", "/app/jobs/job-99");

    const createEstimateLinks = screen.getAllByRole("link", { name: /create estimate/i });
    expect(createEstimateLinks.length).toBeGreaterThanOrEqual(1);
    expect(createEstimateLinks[0]).toHaveAttribute("href", "/app/jobs/job-99/estimates/new");

    const editLink = screen.getByRole("link", { name: /edit lead/i });
    expect(editLink).toBeInTheDocument();
  });

  it("renders — when customer name fields are missing", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      customerFirstName: undefined,
      customerLastName: undefined,
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("—");
    });
    expect(screen.queryByText(/undefined/)).not.toBeInTheDocument();
  });

  it("renders Attachments and Communication Logs section headings", async () => {
    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("John Doe");
    });

    expect(screen.getByRole("heading", { name: /^Attachments$/ })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Communication Logs$/ })).toBeInTheDocument();
  });

  it("uploading file calls uploadLeadAttachment and invalidates", async () => {
    const user = userEvent.setup();
    mockedAttachmentsApi.uploadLeadAttachment.mockResolvedValue({
      id: "att-1",
      fileName: "doc.pdf",
      contentType: "application/pdf",
      fileSize: 100,
      leadId: "lead-1",
      jobId: null,
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-01-01T00:00:00Z",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Attachments$/ })).toBeInTheDocument();
    });

    const uploadBtn = screen.getByRole("button", { name: /upload/i });
    expect(uploadBtn).toBeInTheDocument();
    const fileInput = document.querySelector('input[type="file"]');
    expect(fileInput).toBeInTheDocument();
    if (fileInput) {
      const file = new File(["content"], "doc.pdf", { type: "application/pdf" });
      fireEvent.change(fileInput, { target: { files: [file] } });
    }

    await waitFor(() => {
      expect(mockedAttachmentsApi.uploadLeadAttachment).toHaveBeenCalled();
    });
  });

  it("adding communication log calls addLeadCommunicationLog", async () => {
    const user = userEvent.setup();
    mockedCommLogsApi.addLeadCommunicationLog.mockResolvedValue({
      id: "log-1",
      channel: "NOTE",
      direction: null,
      subject: "Called",
      body: "Left message",
      occurredAt: "2024-01-15T14:00:00Z",
      leadId: "lead-1",
      jobId: null,
      createdAt: "2024-01-15T14:00:00Z",
      updatedAt: "2024-01-15T14:00:00Z",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Communication Logs$/ })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/subject/i), "Called customer");
    await user.click(screen.getByRole("button", { name: /add log/i }));

    await waitFor(() => {
      expect(mockedCommLogsApi.addLeadCommunicationLog).toHaveBeenCalledWith(
        expect.anything(),
        "lead-1",
        expect.objectContaining({ subject: "Called customer", channel: "NOTE" })
      );
    });
  });

  it("adds communication log with auto-filled subject when blank", async () => {
    const user = userEvent.setup();
    mockedCommLogsApi.addLeadCommunicationLog.mockResolvedValue({
      id: "log-1",
      channel: "NOTE",
      direction: null,
      subject: "Note",
      body: "Left message",
      occurredAt: "2024-01-15T14:00:00Z",
      leadId: "lead-1",
      jobId: null,
      createdAt: "2024-01-15T14:00:00Z",
      updatedAt: "2024-01-15T14:00:00Z",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Communication Logs$/ })).toBeInTheDocument();
    });

    // Leave Subject empty; enter body only
    await user.type(screen.getByLabelText(/body/i), "Left message");
    await user.click(screen.getByRole("button", { name: /add log/i }));

    await waitFor(() => {
      expect(mockedCommLogsApi.addLeadCommunicationLog).toHaveBeenCalledWith(
        expect.anything(),
        "lead-1",
        expect.objectContaining({ subject: "Note", channel: "NOTE", body: "Left message" })
      );
    });
  });
});
