import React from "react";
import { AxiosError } from "axios";
import { render, screen, waitFor } from "./test-utils";
import DashboardPage from "@/app/app/page";
import * as dashboardApi from "@/lib/dashboardApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import type { DashboardSummaryDto } from "@/lib/types";

jest.mock("@/lib/dashboardApi");
const mockedDashboardApi = dashboardApi as jest.Mocked<typeof dashboardApi>;

jest.mock("@/lib/pipelineStatusesApi");
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

jest.mock("@/lib/preferencesApi", () => ({
  getAppPreferences: jest.fn().mockResolvedValue({
    dashboard: {
      widgets: ["metrics", "quickActions", "leadPipeline", "jobPipeline", "nextBestActions", "recentLeads", "upcomingJobs", "openTasks"],
    },
    jobsList: { visibleFields: [] },
    leadsList: { visibleFields: [] },
    customersList: { visibleFields: [] },
    tasksList: { visibleFields: [] },
    estimatesList: { visibleFields: [] },
    updatedAt: null,
  }),
}));

jest.mock("next/navigation", () => ({
  usePathname: () => "/app",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
}));

const mockSummary: DashboardSummaryDto = {
  customerCount: 2,
  leadCount: 5,
  jobCount: 3,
  estimateCount: 4,
  invoiceCount: 1,
  openTaskCount: 2,
  leadCountByStatus: {
    NEW: 1,
    CONTACTED: 1,
    INSPECTION_SCHEDULED: 0,
    QUOTE_SENT: 1,
    WON: 1,
    LOST: 1,
  },
  jobCountByStatus: {
    SCHEDULED: 2,
    IN_PROGRESS: 1,
  },
  jobsScheduledThisWeek: 2,
  unscheduledJobsCount: 1,
  estimatesSentCount: 1,
  unpaidInvoiceCount: 1,
  activePipelineLeadCount: 3,
  recentLeads: [
    {
      id: "lead-1",
      statusKey: "NEW",
      statusLabel: "New",
      customerLabel: "Pat Smith",
      propertyLine1: "100 Oak St",
      updatedAt: "2026-04-01T12:00:00Z",
    },
  ],
  upcomingJobs: [
    {
      id: "job-1",
      statusKey: "SCHEDULED",
      statusLabel: "Scheduled",
      scheduledStartDate: "2026-04-12",
      propertyLine1: "200 Pine Rd",
      customerLabel: "Pat Smith",
    },
  ],
  openTasks: [
    {
      taskId: "task-1",
      title: "Call back homeowner",
      status: "TODO",
      dueAt: "2026-04-11T15:00:00Z",
      leadId: "lead-1",
      jobId: null,
      customerId: null,
    },
  ],
};

describe("DashboardPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedDashboardApi.getDashboardSummary.mockResolvedValue(mockSummary);
    mockedPipelineApi.listPipelineStatuses.mockResolvedValue([
      { id: "d1", pipelineType: "LEAD", systemKey: "NEW", label: "New", sortOrder: 0, builtIn: true, active: true },
      { id: "d2", pipelineType: "LEAD", systemKey: "CONTACTED", label: "Contacted", sortOrder: 1, builtIn: true, active: true },
      {
        id: "d3",
        pipelineType: "LEAD",
        systemKey: "INSPECTION_SCHEDULED",
        label: "Inspection",
        sortOrder: 2,
        builtIn: true,
        active: true,
      },
      { id: "d4", pipelineType: "LEAD", systemKey: "QUOTE_SENT", label: "Quote sent", sortOrder: 3, builtIn: true, active: true },
      { id: "d5", pipelineType: "LEAD", systemKey: "WON", label: "Won", sortOrder: 4, builtIn: true, active: true },
      { id: "d6", pipelineType: "LEAD", systemKey: "LOST", label: "Lost", sortOrder: 5, builtIn: true, active: true },
    ]);
  });

  it("renders dashboard title and loads summary", async () => {
    render(<DashboardPage />);

    expect(screen.getByRole("heading", { name: /^Dashboard$/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(mockedDashboardApi.getDashboardSummary).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.getByRole("link", { name: /2\s*customers/i })).toBeInTheDocument();
    });

    expect(screen.getByRole("link", { name: /2\s*customers/i })).toHaveAttribute("href", "/app/customers");
    expect(screen.getByRole("link", { name: /^new customer$/i })).toHaveAttribute("href", "/app/customers/new");
    expect(screen.getByRole("link", { name: /^new lead$/i })).toHaveAttribute("href", "/app/leads/new");
    expect(screen.getByRole("link", { name: /^create task$/i })).toHaveAttribute("href", "/app/tasks/new");
    expect(screen.getByRole("link", { name: /^schedule$/i })).toHaveAttribute("href", "/app/schedule");
    expect(screen.getByRole("link", { name: /jobs & estimates/i })).toHaveAttribute("href", "/app/jobs");
    expect(screen.getByRole("link", { name: /^reports$/i })).toHaveAttribute("href", "/app/reports");
  });

  it("links recent lead and upcoming job rows", async () => {
    render(<DashboardPage />);

    await waitFor(() => {
      const patLinks = screen.getAllByRole("link", { name: /pat smith/i });
      expect(patLinks.some((el) => el.getAttribute("href") === "/app/leads/lead-1")).toBe(true);
      expect(patLinks.some((el) => el.getAttribute("href") === "/app/jobs/job-1")).toBe(true);
    });
  });

  it("links open tasks to task detail", async () => {
    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: /call back homeowner/i })).toHaveAttribute(
        "href",
        "/app/tasks/task-1"
      );
    });
  });

  it("does not show session-expired copy for non-401 dashboard errors", async () => {
    mockedDashboardApi.getDashboardSummary.mockRejectedValue(
      new AxiosError("fail", "ERR_BAD_RESPONSE", undefined, undefined, {
        status: 500,
        statusText: "Internal Server Error",
        data: { message: "Could not aggregate dashboard" },
        headers: {},
        config: {} as never,
      })
    );

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/could not aggregate dashboard/i)).toBeInTheDocument();
    });
    expect(screen.queryByText(/session expired/i)).not.toBeInTheDocument();
  });

  it("shows session-expired copy for 401 from dashboard summary", async () => {
    mockedDashboardApi.getDashboardSummary.mockRejectedValue(
      new AxiosError("Unauthorized", "ERR_BAD_REQUEST", undefined, undefined, {
        status: 401,
        statusText: "Unauthorized",
        data: {},
        headers: {},
        config: {} as never,
      })
    );

    render(<DashboardPage />);

    await waitFor(() => {
      expect(screen.getByText(/session expired/i)).toBeInTheDocument();
    });
  });
});
