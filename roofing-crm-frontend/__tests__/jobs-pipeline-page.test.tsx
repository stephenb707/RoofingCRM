import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import JobsPipelinePage from "@/app/app/jobs/pipeline/page";
import * as jobsApi from "@/lib/jobsApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import { JobDto, PageResponse } from "@/lib/types";

jest.mock("@/lib/jobsApi");
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;

jest.mock("@/lib/pipelineStatusesApi");
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/pipeline",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

const defUnscheduled = {
  id: "def-unsched",
  pipelineType: "JOB" as const,
  systemKey: "UNSCHEDULED",
  label: "Unscheduled",
  sortOrder: 0,
  builtIn: true,
  active: true,
};

const defScheduled = {
  id: "def-sched",
  pipelineType: "JOB" as const,
  systemKey: "SCHEDULED",
  label: "Scheduled",
  sortOrder: 1,
  builtIn: true,
  active: true,
};

const baseJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  statusDefinitionId: defUnscheduled.id,
  statusKey: "UNSCHEDULED",
  statusLabel: "Unscheduled",
  type: "REPLACEMENT",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  scheduledStartDate: null,
  scheduledEndDate: null,
  internalNotes: null,
  crewName: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-10T00:00:00Z",
  customerFirstName: "Alice",
  customerLastName: "Smith",
  customerPhone: "5551234567",
};

const jobScheduled: JobDto = {
  ...baseJob,
  id: "job-2",
  statusDefinitionId: defScheduled.id,
  statusKey: "SCHEDULED",
  statusLabel: "Scheduled",
  scheduledStartDate: "2024-06-01",
  scheduledEndDate: "2024-06-01",
  propertyAddress: { line1: "456 Oak Ave", city: "Boulder", state: "CO" },
  customerFirstName: "Bob",
  customerLastName: "Jones",
  customerPhone: "3035550100",
  updatedAt: "2024-01-12T00:00:00Z",
};

describe("JobsPipelinePage", () => {
  const pipelineResponse: PageResponse<JobDto> = {
    content: [baseJob, jobScheduled],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 200,
    first: true,
    last: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedJobsApi.listJobs.mockResolvedValue(pipelineResponse);
    mockedPipelineApi.listPipelineStatuses.mockResolvedValue([defUnscheduled, defScheduled]);
  });

  it("loads definitions and renders a column per definition with jobs grouped by statusDefinitionId", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Pipeline$/i })).toBeInTheDocument();
    });

    expect(mockedPipelineApi.listPipelineStatuses).toHaveBeenCalledWith(expect.anything(), "JOB");
    expect(mockedJobsApi.listJobs).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ page: 0, size: 200 })
    );

    expect(screen.getByRole("heading", { name: "Unscheduled" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Scheduled" })).toBeInTheDocument();
    expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();

    const unschedCol = screen.getByTestId(`job-pipeline-col-${defUnscheduled.id}`);
    const schedCol = screen.getByTestId(`job-pipeline-col-${defScheduled.id}`);
    expect(unschedCol).toHaveTextContent("1 jobs");
    expect(schedCol).toHaveTextContent("1 jobs");
  });

  it("uses tenant-configured labels on columns", async () => {
    mockedPipelineApi.listPipelineStatuses.mockResolvedValue([
      { ...defUnscheduled, label: "Not yet on calendar" },
      defScheduled,
    ]);

    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Not yet on calendar" })).toBeInTheDocument();
    });
  });

  it("shows customer phone on cards when present", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.getByText("(555) 123-4567")).toBeInTheDocument();
    expect(screen.getByText("(303) 555-0100")).toBeInTheDocument();
  });

  it("does not render compact view toggle", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Pipeline$/i })).toBeInTheDocument();
    });

    expect(screen.queryByRole("checkbox", { name: /compact view/i })).not.toBeInTheDocument();
  });

  it("does not render a dedicated drag handle control", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Drag to change status/i)).not.toBeInTheDocument();
  });

  it("whole card is the drag target when editable (grab cursor)", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByTestId("job-pipeline-card-job-1")).toBeInTheDocument();
    });

    expect(screen.getByTestId("job-pipeline-card-job-1")).toHaveClass("cursor-grab");
  });

  it("shows statusLabel on each card", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.getByTestId("job-pipeline-card-status-job-1")).toHaveTextContent("Unscheduled");
    expect(screen.getByTestId("job-pipeline-card-status-job-2")).toHaveTextContent("Scheduled");
  });

  it("filters cards by search input", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search jobs in pipeline/i), {
      target: { value: "Bob" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
  });

  it("filters by phone digits in search", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Bob Jones")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search jobs in pipeline/i), {
      target: { value: "303555" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
  });

  it("tip links to jobs list for details", async () => {
    render(<JobsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText(/Drag cards to change status/i)).toBeInTheDocument();
    });

    const openJobLink = screen.getByRole("link", { name: /open a job/i });
    expect(openJobLink).toHaveAttribute("href", "/app/jobs");
  });
});
