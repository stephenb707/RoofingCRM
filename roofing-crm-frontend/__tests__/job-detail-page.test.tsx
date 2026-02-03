import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import userEvent from "@testing-library/user-event";
import JobDetailPage from "@/app/app/jobs/[jobId]/page";
import * as jobsApi from "@/lib/jobsApi";
import * as attachmentsApi from "@/lib/attachmentsApi";
import * as communicationLogsApi from "@/lib/communicationLogsApi";
import * as tasksApi from "@/lib/tasksApi";
import * as activityApi from "@/lib/activityApi";
import * as invoicesApi from "@/lib/invoicesApi";
import { JobDto } from "@/lib/types";

jest.mock("@/lib/jobsApi");
jest.mock("@/lib/invoicesApi");
jest.mock("@/lib/attachmentsApi");
jest.mock("@/lib/communicationLogsApi");
jest.mock("@/lib/tasksApi");
jest.mock("@/lib/activityApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/job-1",
  useParams: () => ({ jobId: "job-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;
const mockedAttachmentsApi = attachmentsApi as jest.Mocked<typeof attachmentsApi>;
const mockedCommLogsApi = communicationLogsApi as jest.Mocked<typeof communicationLogsApi>;
const mockedTasksApi = tasksApi as jest.Mocked<typeof tasksApi>;
const mockedActivityApi = activityApi as jest.Mocked<typeof activityApi>;
const mockedInvoicesApi = invoicesApi as jest.Mocked<typeof invoicesApi>;

const mockJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  status: "SCHEDULED",
  type: "REPLACEMENT",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO", zip: "80202" },
  scheduledStartDate: "2024-06-01",
  scheduledEndDate: "2024-06-02",
  internalNotes: "Some notes",
  crewName: "Crew A",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-15T00:00:00Z",
};

describe("JobDetailPage", () => {
  const emptyTasksResponse = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 50, first: true, last: true };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedJobsApi.getJob.mockResolvedValue(mockJob);
    mockedAttachmentsApi.listJobAttachments.mockResolvedValue([]);
    mockedCommLogsApi.listJobCommunicationLogs.mockResolvedValue([]);
    mockedTasksApi.listTasks.mockImplementation(() => Promise.resolve(emptyTasksResponse));
    mockedActivityApi.listActivity.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
      first: true,
      last: true,
    });
    mockedInvoicesApi.listInvoicesForJob.mockResolvedValue([]);
  });

  it("renders job overview and status", async () => {
    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO, 80202")).toBeInTheDocument();
    });

    expect(screen.getByText("Replacement")).toBeInTheDocument();
    expect(screen.getByText("Crew A")).toBeInTheDocument();
    expect(screen.getByText("Some notes")).toBeInTheDocument();
  });

  it("renders Create Estimate, View Estimates, and Edit Job links in Actions section", async () => {
    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO, 80202")).toBeInTheDocument();
    });

    const createEstimateLink = screen.getByRole("link", { name: /Create Estimate/i });
    expect(createEstimateLink).toHaveAttribute("href", "/app/jobs/job-1/estimates/new");

    const viewEstimatesLink = screen.getByRole("link", { name: /View Estimates/i });
    expect(viewEstimatesLink).toHaveAttribute("href", "/app/jobs/job-1/estimates");

    const editLink = screen.getByRole("link", { name: /Edit Job/i });
    expect(editLink).toHaveAttribute("href", "/app/jobs/job-1/edit");
  });

  it("when job.leadId exists shows Created from Lead and View Lead link", async () => {
    mockedJobsApi.getJob.mockResolvedValue({
      ...mockJob,
      leadId: "lead-42",
    });

    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/created from lead/i)).toBeInTheDocument();
    });

    const viewLeadLink = screen.getByRole("link", { name: /view lead/i });
    expect(viewLeadLink).toHaveAttribute("href", "/app/leads/lead-42");
  });

  it("calls updateJobStatus when clicking a status button", async () => {
    mockedJobsApi.updateJobStatus.mockResolvedValue({ ...mockJob, status: "IN_PROGRESS" });

    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO, 80202")).toBeInTheDocument();
    });

    const inProgressBtn = screen.getByRole("button", { name: /^In Progress$/i });
    fireEvent.click(inProgressBtn);

    await waitFor(() => {
      expect(mockedJobsApi.updateJobStatus).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        "IN_PROGRESS"
      );
    });
  });

  it("renders Attachments and Communication Logs section headings", async () => {
    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO, 80202")).toBeInTheDocument();
    });

    expect(screen.getByRole("heading", { name: /^Attachments$/ })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /^Communication Logs$/ })).toBeInTheDocument();
  });

  it("uploading file calls uploadJobAttachment", async () => {
    mockedAttachmentsApi.uploadJobAttachment.mockResolvedValue({
      id: "att-1",
      fileName: "doc.pdf",
      contentType: "application/pdf",
      fileSize: 100,
      leadId: null,
      jobId: "job-1",
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-01-01T00:00:00Z",
    });

    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Attachments$/ })).toBeInTheDocument();
    });

    const fileInput = document.querySelector('input[type="file"]');
    expect(fileInput).toBeInTheDocument();
    if (fileInput) {
      const file = new File(["content"], "doc.pdf", { type: "application/pdf" });
      fireEvent.change(fileInput, { target: { files: [file] } });
    }

    await waitFor(() => {
      expect(mockedAttachmentsApi.uploadJobAttachment).toHaveBeenCalled();
    });
  });

  it("adding communication log calls addJobCommunicationLog", async () => {
    const user = userEvent.setup();
    mockedCommLogsApi.addJobCommunicationLog.mockResolvedValue({
      id: "log-1",
      channel: "CALL",
      direction: "OUTBOUND",
      subject: "Scheduled",
      body: null,
      occurredAt: "2024-01-15T14:00:00Z",
      leadId: null,
      jobId: "job-1",
      createdAt: "2024-01-15T14:00:00Z",
      updatedAt: "2024-01-15T14:00:00Z",
    });

    render(<JobDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Communication Logs$/ })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/subject/i), "Scheduled visit");
    await user.click(screen.getByRole("button", { name: /add log/i }));

    await waitFor(() => {
      expect(mockedCommLogsApi.addJobCommunicationLog).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        expect.objectContaining({ subject: "Scheduled visit", channel: "NOTE" })
      );
    });
  });
});
