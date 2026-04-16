import React from "react";
import { render, screen, waitFor, fireEvent, mockAuthValue } from "./test-utils";
import JobsPage from "@/app/app/jobs/page";
import LeadsPage from "@/app/app/leads/page";
import CustomersPage from "@/app/app/customers/page";
import TasksPage from "@/app/app/tasks/page";
import ListViewSettingsPage from "@/app/app/settings/list-views/page";
import * as jobsApi from "@/lib/jobsApi";
import * as leadsApi from "@/lib/leadsApi";
import * as customersApi from "@/lib/customersApi";
import * as tasksApi from "@/lib/tasksApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import * as preferencesApi from "@/lib/preferencesApi";
import type { JobDto, LeadDto, CustomerDto, TaskDto, PageResponse, AppPreferencesDto } from "@/lib/types";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn(), refresh: jest.fn(), prefetch: jest.fn() }),
  usePathname: () => "/app/jobs",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

jest.mock("@/lib/jobsApi");
jest.mock("@/lib/leadsApi");
jest.mock("@/lib/customersApi");
jest.mock("@/lib/tasksApi");
jest.mock("@/lib/pipelineStatusesApi");
jest.mock("@/lib/preferencesApi");

const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;
const mockedTasksApi = tasksApi as jest.Mocked<typeof tasksApi>;
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;
const mockedPrefsApi = preferencesApi as jest.Mocked<typeof preferencesApi>;

const defaultPrefs: AppPreferencesDto = {
  dashboard: { widgets: ["metrics"] },
  jobsList: { visibleFields: ["type", "status", "propertyAddress", "scheduledStartDate", "updatedAt"] },
  leadsList: { visibleFields: ["customer", "propertyAddress", "status", "source", "createdAt"] },
  customersList: { visibleFields: ["name", "phone", "email"] },
  tasksList: { visibleFields: ["title", "status", "priority", "dueAt", "assignedTo", "related"] },
  estimatesList: { visibleFields: ["title", "status", "total"] },
  updatedAt: null,
};

const mockJob: JobDto = {
  id: "job-1", customerId: "cust-1", leadId: null,
  statusDefinitionId: "def-1", statusKey: "SCHEDULED", statusLabel: "Scheduled",
  type: "REPLACEMENT",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  scheduledStartDate: "2024-06-01", scheduledEndDate: null,
  internalNotes: null, crewName: "Crew A",
  createdAt: "2024-01-01T00:00:00Z", updatedAt: "2024-01-15T00:00:00Z",
  customerFirstName: "Jane", customerLastName: "Doe",
};

const mockLead: LeadDto = {
  id: "lead-1", customerId: "cust-1",
  statusDefinitionId: "def-1", statusKey: "NEW", statusLabel: "New",
  source: "WEBSITE", leadNotes: null,
  propertyAddress: { line1: "456 Oak Ave", city: "Boulder", state: "CO" },
  pipelinePosition: 0,
  createdAt: "2024-01-01T00:00:00Z", updatedAt: "2024-01-15T00:00:00Z",
  customerFirstName: "John", customerLastName: "Smith",
  customerPhone: "555-0100",
};

const mockCustomer: CustomerDto = {
  id: "cust-1", firstName: "Jane", lastName: "Doe",
  email: "jane@example.com", primaryPhone: "555-0100",
};

const mockTask: TaskDto = {
  taskId: "task-1", title: "Follow up", status: "TODO", priority: "HIGH",
  dueAt: null, assignedToName: "Sam",
  leadId: "lead-1", jobId: null, customerId: null,
  createdAt: "2024-01-01T00:00:00Z", updatedAt: "2024-01-02T00:00:00Z",
};

function makePage<T>(items: T[]): PageResponse<T> {
  return { content: items, totalElements: items.length, totalPages: 1, number: 0, size: 20, first: true, last: true };
}

beforeEach(() => {
  jest.clearAllMocks();
  mockAuthValue.auth.tenants = [{ tenantId: "tenant-123", tenantName: "Test", tenantSlug: "test", role: "ADMIN" as const }];
  mockAuthValue.auth.selectedTenantId = "tenant-123";
  mockAuthValue.auth.token = "t";
  mockedPipelineApi.listPipelineStatuses.mockResolvedValue([]);
  mockedPrefsApi.getAppPreferences.mockResolvedValue(defaultPrefs);
});

// ====== Settings page tests ======

describe("List View Settings page controls", () => {
  it("renders expand toggles for Jobs, Leads, Customers, Tasks", async () => {
    render(<ListViewSettingsPage />);
    await waitFor(() => {
      expect(screen.getByText("Jobs")).toBeInTheDocument();
      expect(screen.getByText("Leads")).toBeInTheDocument();
      expect(screen.getByText("Customers")).toBeInTheDocument();
      expect(screen.getByText("Tasks")).toBeInTheDocument();
    });
  });

  it("expanding Jobs shows field checkboxes", async () => {
    render(<ListViewSettingsPage />);
    await waitFor(() => expect(screen.getByText("Jobs")).toBeInTheDocument());

    fireEvent.click(screen.getByText("Jobs"));

    await waitFor(() => {
      expect(screen.getByLabelText("Type")).toBeInTheDocument();
      expect(screen.getByLabelText("Status")).toBeInTheDocument();
      expect(screen.getByLabelText("Property Address")).toBeInTheDocument();
      expect(screen.getByLabelText("Customer")).toBeInTheDocument();
    });
  });

  it("toggling a field calls updateAppPreferences", async () => {
    mockedPrefsApi.updateAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      jobsList: { visibleFields: ["type", "status", "propertyAddress", "scheduledStartDate"] },
      updatedAt: "2026-04-15T00:00:00Z",
    });

    render(<ListViewSettingsPage />);
    await waitFor(() => expect(screen.getByText("Jobs")).toBeInTheDocument());

    fireEvent.click(screen.getByText("Jobs"));
    await waitFor(() => expect(screen.getByLabelText("Updated")).toBeInTheDocument());

    fireEvent.click(screen.getByLabelText("Updated"));

    await waitFor(() => {
      expect(mockedPrefsApi.updateAppPreferences).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          jobsList: { visibleFields: expect.not.arrayContaining(["updatedAt"]) },
        })
      );
    });
  });

  it("restore defaults button resets list", async () => {
    const customPrefs = {
      ...defaultPrefs,
      jobsList: { visibleFields: ["status"] },
      updatedAt: "2026-04-15T00:00:00Z",
    };
    mockedPrefsApi.getAppPreferences.mockResolvedValue(customPrefs);
    mockedPrefsApi.updateAppPreferences.mockResolvedValue(defaultPrefs);

    render(<ListViewSettingsPage />);
    await waitFor(() => expect(screen.getByText("Jobs")).toBeInTheDocument());

    fireEvent.click(screen.getByText("Jobs"));
    await waitFor(() => expect(screen.getByText("Restore defaults")).toBeInTheDocument());

    fireEvent.click(screen.getByText("Restore defaults"));

    await waitFor(() => {
      expect(mockedPrefsApi.updateAppPreferences).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          jobsList: { visibleFields: ["type", "status", "propertyAddress", "scheduledStartDate", "updatedAt"] },
        })
      );
    });
  });
});

// ====== Jobs list page tests ======

describe("Jobs list respects visibleFields", () => {
  it("hides updatedAt column when not in visibleFields", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      jobsList: { visibleFields: ["type", "status", "propertyAddress"] },
    });
    mockedJobsApi.listJobs.mockResolvedValue(makePage([mockJob]));

    render(<JobsPage />);
    await waitFor(() => expect(screen.getByText("Replacement")).toBeInTheDocument());

    const headers = screen.getAllByRole("columnheader");
    const headerTexts = headers.map((h) => h.textContent);
    expect(headerTexts).toContain("Type");
    expect(headerTexts).toContain("Status");
    expect(headerTexts).not.toContain("Updated");
  });

  it("shows customer column when added to visibleFields", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      jobsList: { visibleFields: ["type", "status", "customer"] },
    });
    mockedJobsApi.listJobs.mockResolvedValue(makePage([mockJob]));

    render(<JobsPage />);
    await waitFor(() => expect(screen.getByText("Replacement")).toBeInTheDocument());

    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    const headers = screen.getAllByRole("columnheader");
    expect(headers.map((h) => h.textContent)).toContain("Customer");
  });
});

// ====== Leads list page tests ======

describe("Leads list respects visibleFields", () => {
  it("hides source column when not in visibleFields", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      leadsList: { visibleFields: ["customer", "status"] },
    });
    mockedLeadsApi.listLeads.mockResolvedValue(makePage([mockLead]));

    render(<LeadsPage />);
    await waitFor(() => expect(screen.getByText("John Smith")).toBeInTheDocument());

    const headers = screen.getAllByRole("columnheader");
    const headerTexts = headers.map((h) => h.textContent);
    expect(headerTexts).toContain("Customer");
    expect(headerTexts).not.toContain("Source");
    expect(headerTexts).not.toContain("Property Address");
  });
});

// ====== Customers list page tests ======

describe("Customers list respects visibleFields", () => {
  it("hides email column when not in visibleFields", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      customersList: { visibleFields: ["name", "phone"] },
    });
    mockedCustomersApi.listCustomers.mockResolvedValue(makePage([mockCustomer]));

    render(<CustomersPage />);
    await waitFor(() => expect(screen.getByText("Jane Doe")).toBeInTheDocument());

    const headers = screen.getAllByRole("columnheader");
    const headerTexts = headers.map((h) => h.textContent);
    expect(headerTexts).toContain("Name");
    expect(headerTexts).toContain("Phone");
    expect(headerTexts).not.toContain("Email");
  });
});

// ====== Tasks list page tests ======

describe("Tasks list respects visibleFields", () => {
  it("hides assignee column when not in visibleFields", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...defaultPrefs,
      tasksList: { visibleFields: ["title", "status", "priority"] },
    });
    mockedTasksApi.listTasks.mockResolvedValue(makePage([mockTask]));

    render(<TasksPage />);
    await waitFor(() => expect(screen.getByText("Follow up")).toBeInTheDocument());

    const headers = screen.getAllByRole("columnheader");
    const headerTexts = headers.map((h) => h.textContent);
    expect(headerTexts).toContain("Title");
    expect(headerTexts).not.toContain("Assignee");
    expect(headerTexts).not.toContain("Related");
  });
});
