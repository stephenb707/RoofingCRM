import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import JobDetailPage from "@/app/app/jobs/[jobId]/page";
import * as jobsApi from "@/lib/jobsApi";
import { JobDto } from "@/lib/types";

jest.mock("@/lib/jobsApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/job-1",
  useParams: () => ({ jobId: "job-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;

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
  beforeEach(() => {
    jest.clearAllMocks();
    mockedJobsApi.getJob.mockResolvedValue(mockJob);
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
});
