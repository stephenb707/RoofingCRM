import React from "react";
import { render, screen, waitFor } from "./test-utils";
import CombinedPipelinePage from "@/app/app/pipeline/combined/page";
import * as leadsApi from "@/lib/leadsApi";
import * as jobsApi from "@/lib/jobsApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import type { JobDto, LeadDto } from "@/lib/types";

jest.mock("@/lib/leadsApi");
jest.mock("@/lib/jobsApi");
jest.mock("@/lib/pipelineStatusesApi");

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/pipeline/combined",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

const defLead = {
  id: "def-l",
  pipelineType: "LEAD" as const,
  systemKey: "NEW",
  label: "New",
  sortOrder: 0,
  builtIn: true,
  active: true,
};

const defJob = {
  id: "def-j",
  pipelineType: "JOB" as const,
  systemKey: "SCHEDULED",
  label: "Scheduled",
  sortOrder: 0,
  builtIn: true,
  active: true,
};

const mockLead: LeadDto = {
  id: "lead-x",
  customerId: "c1",
  statusDefinitionId: defLead.id,
  statusKey: "NEW",
  statusLabel: "New",
  source: "WEBSITE",
  leadNotes: "",
  propertyAddress: { line1: "1 St", city: "Denver", state: "CO" },
  pipelinePosition: 0,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "Pat",
  customerLastName: "Lee",
  customerPhone: "5551112222",
};

const mockJob: JobDto = {
  id: "job-x",
  customerId: "c1",
  leadId: null,
  statusDefinitionId: defJob.id,
  statusKey: "SCHEDULED",
  statusLabel: "Scheduled",
  type: "REPLACEMENT",
  propertyAddress: { line1: "2 St", city: "Denver", state: "CO" },
  scheduledStartDate: null,
  scheduledEndDate: null,
  internalNotes: null,
  crewName: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-10T00:00:00Z",
  customerFirstName: "Sam",
  customerLastName: "Kim",
};

describe("CombinedPipelinePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedPipelineApi.listPipelineStatuses.mockImplementation((_api, type) => {
      if (type === "LEAD") return Promise.resolve([defLead]);
      return Promise.resolve([defJob]);
    });
    mockedLeadsApi.listLeads.mockResolvedValue({
      content: [mockLead],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 200,
      first: true,
      last: true,
    });
    mockedJobsApi.listJobs.mockResolvedValue({
      content: [mockJob],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 200,
      first: true,
      last: true,
    });
  });

  it("renders lead and job pipeline sections and view switcher", async () => {
    render(<CombinedPipelinePage />);

    expect(screen.getByTestId("combined-pipeline-page")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByTestId("combined-pipeline-lead-section")).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByTestId("combined-pipeline-job-section")).toBeInTheDocument();
    });

    expect(screen.getByRole("heading", { name: /Lead pipeline/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Job pipeline/i })).toBeInTheDocument();
    expect(screen.getByTestId("pipeline-view-switcher")).toBeInTheDocument();
    expect(screen.getByTestId("pipeline-view-switch-combined")).toHaveAttribute("aria-current", "page");
  });
});
