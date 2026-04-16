import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import JobsPage from "@/app/app/jobs/page";
import * as jobsApi from "@/lib/jobsApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import { JobDto, PageResponse } from "@/lib/types";
import { __setPathname } from "./pathnameState";

const mockPush = jest.fn();
jest.mock("next/navigation", () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const pathnameState = require("@/__tests__/pathnameState");
  return {
    useRouter: () => ({
      push: mockPush,
      replace: jest.fn(),
      back: jest.fn(),
      forward: jest.fn(),
      refresh: jest.fn(),
      prefetch: jest.fn(),
    }),
    usePathname: () => pathnameState.__pathname,
    useParams: () => ({}),
    useSearchParams: () => new URLSearchParams(),
  };
});

jest.mock("@/lib/jobsApi");
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;

jest.mock("@/lib/pipelineStatusesApi");
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

jest.mock("@/lib/preferencesApi", () => ({
  getAppPreferences: jest.fn().mockResolvedValue({
    dashboard: { widgets: [] },
    jobsList: { visibleFields: ["type", "status", "propertyAddress", "scheduledStartDate", "updatedAt"] },
    leadsList: { visibleFields: [] },
    customersList: { visibleFields: [] },
    tasksList: { visibleFields: [] },
    estimatesList: { visibleFields: [] },
    updatedAt: null,
  }),
}));

const defScheduled = {
  id: "def-scheduled",
  pipelineType: "JOB" as const,
  systemKey: "SCHEDULED",
  label: "Scheduled",
  sortOrder: 0,
  builtIn: true,
  active: true,
};

const defInProgress = {
  id: "def-in-progress",
  pipelineType: "JOB" as const,
  systemKey: "IN_PROGRESS",
  label: "In Progress",
  sortOrder: 1,
  builtIn: true,
  active: true,
};

const defCompleted = {
  id: "def-completed",
  pipelineType: "JOB" as const,
  systemKey: "COMPLETED",
  label: "Completed",
  sortOrder: 2,
  builtIn: true,
  active: true,
};

const mockJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  statusDefinitionId: defScheduled.id,
  statusKey: "SCHEDULED",
  statusLabel: "Scheduled",
  type: "REPLACEMENT",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  scheduledStartDate: "2024-06-01",
  scheduledEndDate: "2024-06-02",
  internalNotes: null,
  crewName: "Crew A",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-15T00:00:00Z",
};

const mockJob2: JobDto = {
  ...mockJob,
  id: "job-2",
  statusDefinitionId: defInProgress.id,
  statusKey: "IN_PROGRESS",
  statusLabel: "In Progress",
  type: "REPAIR",
  propertyAddress: { line1: "456 Oak Ave", city: "Boulder", state: "CO" },
};

const mockPage: PageResponse<JobDto> = {
  content: [mockJob, mockJob2],
  totalElements: 2,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("JobsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    __setPathname("/app/jobs");
    mockedPipelineApi.listPipelineStatuses.mockResolvedValue([
      defScheduled,
      defInProgress,
      defCompleted,
    ]);
  });

  afterEach(() => {
    __setPathname("/app/leads");
  });

  it("renders header and New Job button", async () => {
    mockedJobsApi.listJobs.mockResolvedValue(mockPage);

    render(<JobsPage />);

    await waitFor(() => {
      expect(screen.getByText("Jobs")).toBeInTheDocument();
    });
    expect(screen.getByText("Manage roofing jobs and schedules")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /\+ New Job/i })).toHaveAttribute("href", "/app/jobs/new");
    expect(screen.getByRole("link", { name: /pipeline view/i })).toHaveAttribute("href", "/app/jobs/pipeline");
  });

  it("renders job rows when data exists", async () => {
    mockedJobsApi.listJobs.mockResolvedValue(mockPage);

    render(<JobsPage />);

    await waitFor(() => {
      expect(screen.getByText(/123 Main St.*Denver/)).toBeInTheDocument();
    });
    expect(screen.getByText(/456 Oak Ave.*Boulder/)).toBeInTheDocument();
    expect(screen.getByText("Replacement")).toBeInTheDocument();
    expect(screen.getByText("Repair")).toBeInTheDocument();
    expect(screen.getByText("In Progress", { selector: "span" })).toBeInTheDocument();
  });

  it("filter change triggers new listJobs call", async () => {
    mockedJobsApi.listJobs.mockResolvedValue(mockPage);

    render(<JobsPage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO")).toBeInTheDocument();
    });

    mockedJobsApi.listJobs.mockClear();

    const statusSelect = screen.getByLabelText("Status:");
    fireEvent.change(statusSelect, { target: { value: defCompleted.id } });

    await waitFor(() => {
      expect(mockedJobsApi.listJobs).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ statusDefinitionId: defCompleted.id })
      );
    });
  });

  it("pagination Next/Previous buttons work", async () => {
    const page1 = { ...mockPage, totalPages: 2, totalElements: 2, last: false };
    const page2 = {
      ...mockPage,
      content: [mockJob2],
      number: 1,
      totalPages: 2,
      first: false,
      last: true,
    };
    mockedJobsApi.listJobs
      .mockResolvedValueOnce(page1)
      .mockResolvedValueOnce(page2);

    render(<JobsPage />);

    await waitFor(() => {
      expect(screen.getByText(/123 Main St/)).toBeInTheDocument();
    });

    const nextBtn = await screen.findByRole("button", { name: /Next/i });
    fireEvent.click(nextBtn);

    await waitFor(() => {
      expect(mockedJobsApi.listJobs).toHaveBeenLastCalledWith(
        expect.anything(),
        expect.objectContaining({ page: 1 })
      );
    });
  });

  it("View link goes to job detail", async () => {
    mockedJobsApi.listJobs.mockResolvedValue(mockPage);

    render(<JobsPage />);

    const viewLinks = await screen.findAllByRole("link", { name: "View" });
    expect(viewLinks[0]).toHaveAttribute("href", "/app/jobs/job-1");
    expect(viewLinks[1]).toHaveAttribute("href", "/app/jobs/job-2");
  });

  it("navigates to job detail when clicking the row", async () => {
    mockedJobsApi.listJobs.mockResolvedValue(mockPage);

    render(<JobsPage />);

    await waitFor(() => {
      expect(screen.getByText("Replacement")).toBeInTheDocument();
    });

    mockPush.mockClear();
    fireEvent.click(screen.getByLabelText("Open job: Replacement"));

    expect(mockPush).toHaveBeenCalledWith("/app/jobs/job-1");
  });

  it("does not fire row navigation when clicking the View link", async () => {
    mockedJobsApi.listJobs.mockResolvedValue(mockPage);

    render(<JobsPage />);

    const viewLinks = await screen.findAllByRole("link", { name: "View" });
    mockPush.mockClear();
    fireEvent.click(viewLinks[0]);

    expect(mockPush).not.toHaveBeenCalled();
  });
});
