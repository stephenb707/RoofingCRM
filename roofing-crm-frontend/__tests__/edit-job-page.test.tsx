import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import EditJobPage from "@/app/app/jobs/[jobId]/edit/page";
import * as jobsApi from "@/lib/jobsApi";
import { JobDto } from "@/lib/types";

const mockPush = jest.fn();

jest.mock("@/lib/jobsApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/job-1/edit",
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
  internalNotes: "Original notes",
  crewName: "Crew A",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("EditJobPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockedJobsApi.getJob.mockResolvedValue(mockJob);
    mockedJobsApi.updateJob.mockResolvedValue({ ...mockJob, internalNotes: "Updated notes" });
  });

  it("prefills form from job and submit calls updateJob then navigates to detail", async () => {
    render(<EditJobPage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("123 Main St")).toBeInTheDocument();
    });
    expect(screen.getByDisplayValue("Original notes")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Crew A")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Internal notes/i), { target: { value: "Updated notes" } });

    fireEvent.click(screen.getByRole("button", { name: /Save changes/i }));

    await waitFor(() => {
      expect(mockedJobsApi.updateJob).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        expect.objectContaining({
          internalNotes: "Updated notes",
          propertyAddress: expect.any(Object),
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/app/jobs/job-1");
    });
  });

  it("shows loading then form", async () => {
    let resolve: (v: JobDto) => void;
    mockedJobsApi.getJob.mockImplementation(() => new Promise((r) => { resolve = r; }));

    render(<EditJobPage />);
    expect(screen.getByText("Loading job…")).toBeInTheDocument();

    resolve!(mockJob);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Save changes/i })).toBeInTheDocument();
    });
  });
});
