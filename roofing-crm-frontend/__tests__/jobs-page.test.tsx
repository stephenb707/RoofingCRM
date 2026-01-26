import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import JobsPage from "@/app/app/jobs/page";
import * as jobsApi from "@/lib/jobsApi";
import { JobDto, PageResponse } from "@/lib/types";
import { __setPathname } from "./pathnameState";

jest.mock("@/lib/jobsApi");
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;

const mockJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  status: "SCHEDULED",
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
  status: "IN_PROGRESS",
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
    __setPathname("/app/jobs");
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
    fireEvent.change(statusSelect, { target: { value: "COMPLETED" } });

    await waitFor(() => {
      expect(mockedJobsApi.listJobs).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ status: "COMPLETED" })
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
});
