import React from "react";
import { render, screen, waitFor, fireEvent, mockAuthValue } from "./test-utils";
import DashboardPage from "@/app/app/page";
import DashboardSettingsPage from "@/app/app/settings/dashboard/page";
import * as dashboardApi from "@/lib/dashboardApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import * as preferencesApi from "@/lib/preferencesApi";
import type { DashboardSummaryDto, AppPreferencesDto } from "@/lib/types";

jest.mock("next/navigation", () => ({
  usePathname: () => "/app",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn(), refresh: jest.fn(), prefetch: jest.fn() }),
}));

jest.mock("@/lib/dashboardApi");
jest.mock("@/lib/pipelineStatusesApi");
jest.mock("@/lib/preferencesApi");

const mockedDashboardApi = dashboardApi as jest.Mocked<typeof dashboardApi>;
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;
const mockedPrefsApi = preferencesApi as jest.Mocked<typeof preferencesApi>;

const defaultPrefs: AppPreferencesDto = {
  dashboard: {
    widgets: ["metrics", "quickActions", "leadPipeline", "jobPipeline", "nextBestActions", "recentLeads", "upcomingJobs", "openTasks"],
  },
  jobsList: { visibleFields: [] },
  leadsList: { visibleFields: [] },
  customersList: { visibleFields: [] },
  tasksList: { visibleFields: [] },
  estimatesList: { visibleFields: [] },
  updatedAt: null,
};

const mockSummary: DashboardSummaryDto = {
  customerCount: 2,
  leadCount: 5,
  jobCount: 3,
  estimateCount: 4,
  invoiceCount: 1,
  openTaskCount: 2,
  leadCountByStatus: { NEW: 1, CONTACTED: 1 },
  jobCountByStatus: { SCHEDULED: 2, IN_PROGRESS: 1 },
  jobsScheduledThisWeek: 2,
  unscheduledJobsCount: 1,
  estimatesSentCount: 1,
  unpaidInvoiceCount: 1,
  activePipelineLeadCount: 3,
  recentLeads: [
    { id: "lead-1", statusKey: "NEW", statusLabel: "New", customerLabel: "Pat Smith", propertyLine1: "100 Oak St", updatedAt: "2026-04-01T12:00:00Z" },
  ],
  upcomingJobs: [
    { id: "job-1", statusKey: "SCHEDULED", statusLabel: "Scheduled", scheduledStartDate: "2026-04-12", propertyLine1: "200 Pine Rd", customerLabel: "Pat Smith" },
  ],
  openTasks: [
    { taskId: "task-1", title: "Call back homeowner", status: "TODO", dueAt: "2026-04-11T15:00:00Z", leadId: "lead-1", jobId: null, customerId: null },
  ],
};

const leadDefs = [
  { id: "d1", pipelineType: "LEAD" as const, systemKey: "NEW", label: "New", sortOrder: 0, builtIn: true, active: true },
  { id: "d2", pipelineType: "LEAD" as const, systemKey: "CONTACTED", label: "Contacted", sortOrder: 1, builtIn: true, active: true },
];

const jobDefs = [
  { id: "j1", pipelineType: "JOB" as const, systemKey: "SCHEDULED", label: "Scheduled", sortOrder: 0, builtIn: true, active: true },
  { id: "j2", pipelineType: "JOB" as const, systemKey: "IN_PROGRESS", label: "In Progress", sortOrder: 1, builtIn: true, active: true },
];

beforeEach(() => {
  jest.clearAllMocks();
  mockAuthValue.auth.tenants = [{ tenantId: "tenant-123", tenantName: "Test", tenantSlug: "test", role: "ADMIN" as const }];
  mockAuthValue.auth.selectedTenantId = "tenant-123";
  mockAuthValue.auth.token = "t";
  mockedDashboardApi.getDashboardSummary.mockResolvedValue(mockSummary);
  mockedPipelineApi.listPipelineStatuses.mockImplementation((_api, type) =>
    Promise.resolve(type === "JOB" ? jobDefs : leadDefs)
  );
  mockedPrefsApi.getAppPreferences.mockResolvedValue(defaultPrefs);
});

function getHeadingTexts(): string[] {
  return screen.getAllByRole("heading").map((h) => h.textContent ?? "");
}

// ====== Dashboard page tests ======

describe("Dashboard renders widgets from preferences", () => {
  it("renders all default widgets", async () => {
    render(<DashboardPage />);
    await waitFor(() => expect(screen.getByText("Summary")).toBeInTheDocument());

    const headings = getHeadingTexts();
    expect(headings).toContain("Dashboard");
    expect(headings).toContain("Lead pipeline");
    expect(headings).toContain("Job pipeline");
    expect(headings).toContain("Next best actions");
    expect(headings).toContain("Recent leads");
    expect(headings).toContain("Upcoming jobs");
    expect(headings).toContain("Open tasks");
  });

  it("hides widgets not in preferences", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      dashboard: { widgets: ["metrics"] },
    });

    render(<DashboardPage />);
    await waitFor(() => expect(screen.getByText("Summary")).toBeInTheDocument());

    const headings = getHeadingTexts();
    expect(headings).not.toContain("Quick actions");
    expect(headings).not.toContain("Lead pipeline");
    expect(headings).not.toContain("Job pipeline");
    expect(headings).not.toContain("Next best actions");
    expect(headings).not.toContain("Recent leads");
    expect(headings).not.toContain("Upcoming jobs");
    expect(headings).not.toContain("Open tasks");
  });

  it("renders Job Pipeline widget with generic pipeline hub link", async () => {
    render(<DashboardPage />);
    await waitFor(() => {
      const headings = getHeadingTexts();
      expect(headings).toContain("Job pipeline");
    });
    const pipelineLinks = screen.getAllByRole("link", { name: /Open pipeline/ });
    const hubLinks = pipelineLinks.filter((el) => el.getAttribute("href") === "/app/pipeline");
    expect(hubLinks.length).toBeGreaterThanOrEqual(1);
  });

  it("only renders metrics and openTasks when configured", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      dashboard: { widgets: ["metrics", "openTasks"] },
    });

    render(<DashboardPage />);
    await waitFor(() => expect(screen.getByText("Summary")).toBeInTheDocument());

    const headings = getHeadingTexts();
    expect(headings).toContain("Open tasks");
    expect(headings).not.toContain("Lead pipeline");
    expect(headings).not.toContain("Job pipeline");
  });
});

// ====== Dashboard Settings page tests ======

describe("Dashboard Settings page", () => {
  it("renders dashboard widget checkboxes", async () => {
    render(<DashboardSettingsPage />);
    await waitFor(() => expect(screen.getByText("Summary Metrics")).toBeInTheDocument());

    expect(screen.getByText("Quick Actions")).toBeInTheDocument();
    expect(screen.getByText("Lead Pipeline")).toBeInTheDocument();
    expect(screen.getByText("Job Pipeline")).toBeInTheDocument();
    expect(screen.getByText("Next Best Actions")).toBeInTheDocument();
    expect(screen.getByText("Recent Leads")).toBeInTheDocument();
    expect(screen.getByText("Upcoming Jobs")).toBeInTheDocument();
    expect(screen.getByText("Open Tasks")).toBeInTheDocument();
  });

  it("includes a back link to Settings", async () => {
    render(<DashboardSettingsPage />);
    await waitFor(() => expect(screen.getByText("Summary Metrics")).toBeInTheDocument());

    const backLink = screen.getByRole("link", { name: /back to settings/i });
    expect(backLink).toHaveAttribute("href", "/app/settings");
  });

  it("toggling a widget calls updateAppPreferences", async () => {
    mockedPrefsApi.updateAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      dashboard: { widgets: ["metrics", "quickActions", "leadPipeline", "jobPipeline", "nextBestActions", "recentLeads", "upcomingJobs"] },
      updatedAt: "2026-04-15T00:00:00Z",
    });

    render(<DashboardSettingsPage />);
    await waitFor(() => expect(screen.getByText("Open Tasks")).toBeInTheDocument());

    const checkboxes = screen.getAllByRole("checkbox");
    const openTasksCheckbox = checkboxes.find((cb) => {
      const label = cb.closest("label");
      return label?.textContent?.includes("Open Tasks");
    });
    expect(openTasksCheckbox).toBeTruthy();
    fireEvent.click(openTasksCheckbox!);

    await waitFor(() => {
      expect(mockedPrefsApi.updateAppPreferences).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          dashboard: { widgets: expect.not.arrayContaining(["openTasks"]) },
        })
      );
    });
  });

  it("restore defaults sends default widget list", async () => {
    const customPrefs = {
      ...defaultPrefs,
      dashboard: { widgets: ["metrics"] },
      updatedAt: "2026-04-15T00:00:00Z",
    };
    mockedPrefsApi.getAppPreferences.mockResolvedValue(customPrefs);
    mockedPrefsApi.updateAppPreferences.mockResolvedValue(defaultPrefs);

    render(<DashboardSettingsPage />);
    await waitFor(() => expect(screen.getByText("Restore default dashboard")).toBeInTheDocument());

    fireEvent.click(screen.getByText("Restore default dashboard"));

    await waitFor(() => {
      expect(mockedPrefsApi.updateAppPreferences).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          dashboard: {
            widgets: ["metrics", "quickActions", "leadPipeline", "jobPipeline", "nextBestActions", "recentLeads", "upcomingJobs", "openTasks"],
          },
        })
      );
    });
  });
});
