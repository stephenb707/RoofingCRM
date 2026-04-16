import React from "react";
import { render, screen, mockAuthValue, waitFor } from "./test-utils";
import SettingsPage from "@/app/app/settings/page";

jest.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: jest.fn(),
    push: jest.fn(),
    back: jest.fn(),
    refresh: jest.fn(),
    prefetch: jest.fn(),
  }),
  usePathname: () => "/app/settings",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

jest.mock("@/lib/preferencesApi", () => ({
  getAppPreferences: jest.fn().mockResolvedValue({
    dashboard: {
      widgets: [
        "metrics",
        "quickActions",
        "leadPipeline",
        "jobPipeline",
        "nextBestActions",
        "recentLeads",
        "upcomingJobs",
        "openTasks",
      ],
    },
    jobsList: {
      visibleFields: [
        "type",
        "status",
        "propertyAddress",
        "scheduledStartDate",
        "updatedAt",
      ],
    },
    leadsList: {
      visibleFields: ["customer", "propertyAddress", "status", "source", "createdAt"],
    },
    customersList: { visibleFields: ["name", "phone", "email"] },
    tasksList: {
      visibleFields: ["title", "status", "priority", "dueAt", "assignedTo", "related"],
    },
    estimatesList: {
      visibleFields: ["title", "status", "total", "issueDate", "validUntil"],
    },
    updatedAt: null,
  }),
  updateAppPreferences: jest.fn().mockResolvedValue({}),
}));

describe("SettingsPage (hub)", () => {
  beforeEach(() => {
    mockAuthValue.auth.tenants = [
      {
        tenantId: "tenant-123",
        tenantName: "Test Company",
        tenantSlug: "test-company",
        role: "ADMIN" as const,
      },
    ];
    mockAuthValue.auth.selectedTenantId = "tenant-123";
    mockAuthValue.auth.token = "t";
  });

  it("renders the Settings heading", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /^Settings$/i })
      ).toBeInTheDocument();
    });
  });

  it("renders Dashboard section with Manage link", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /^Dashboard$/i })
      ).toBeInTheDocument();
    });
    const manageLinks = screen.getAllByRole("link", { name: /Manage/i });
    const dashLink = manageLinks.find(
      (el) => el.getAttribute("href") === "/app/settings/dashboard"
    );
    expect(dashLink).toBeTruthy();
  });

  it("renders List Views section with Manage link", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /^List Views$/i })
      ).toBeInTheDocument();
    });
    const manageLinks = screen.getAllByRole("link", { name: /Manage/i });
    const listLink = manageLinks.find(
      (el) => el.getAttribute("href") === "/app/settings/list-views"
    );
    expect(listLink).toBeTruthy();
  });

  it("renders Pipeline Statuses section with Manage link for ADMIN", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /Pipeline Statuses/i })
      ).toBeInTheDocument();
    });
    const manageLinks = screen.getAllByRole("link", { name: /Manage/i });
    const pipelineLink = manageLinks.find(
      (el) => el.getAttribute("href") === "/app/settings/pipeline-statuses"
    );
    expect(pipelineLink).toBeTruthy();
  });

  it("hides Pipeline Statuses section for SALES role", async () => {
    mockAuthValue.auth.tenants[0]!.role = "SALES";

    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /^Settings$/i })
      ).toBeInTheDocument();
    });
    expect(
      screen.queryByRole("heading", { name: /Pipeline Statuses/i })
    ).not.toBeInTheDocument();
  });

  it("shows default preferences status when no prefs saved", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByText(/using default preferences/i)).toBeInTheDocument();
    });
  });

  it("does NOT render inline dashboard widget checkboxes", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /^Settings$/i })
      ).toBeInTheDocument();
    });
    expect(screen.queryByText("Summary Metrics")).not.toBeInTheDocument();
    expect(screen.queryByRole("checkbox")).not.toBeInTheDocument();
  });

  it("does NOT render inline list field editors", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(
        screen.getByRole("heading", { name: /^Settings$/i })
      ).toBeInTheDocument();
    });
    expect(screen.queryByText("Restore defaults")).not.toBeInTheDocument();
  });

  it("shows widget and field count summaries", async () => {
    render(<SettingsPage />);
    await waitFor(() => {
      expect(screen.getByText(/8 of 8 widgets active/i)).toBeInTheDocument();
    });
  });
});
