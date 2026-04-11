import React from "react";
import { render, screen, waitFor, fireEvent, within } from "./test-utils";
import SchedulePage from "@/app/app/schedule/page";
import * as jobsApi from "@/lib/jobsApi";

jest.mock("@/lib/jobsApi");

const mockSearchParamsGet = jest.fn();
jest.mock("next/navigation", () => ({
  useSearchParams: () => ({
    get: (key: string) => mockSearchParamsGet(key),
  }),
}));

const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;

/** January 2026 calendar grid (Sun start): 2025-12-28 … 2026-01-31 */
const JAN_2026_GRID_FROM = "2025-12-28";
const JAN_2026_GRID_TO = "2026-01-31";

const job1 = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  status: "SCHEDULED" as const,
  type: "REPLACEMENT" as const,
  propertyAddress: { line1: "123 Main St", city: "Chicago", state: "IL", zip: "60601" },
  scheduledStartDate: "2026-01-15",
  scheduledEndDate: "2026-01-17",
  internalNotes: null,
  crewName: "Alpha",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  customerFirstName: "Jane",
  customerLastName: "Doe",
};

const job2 = {
  ...job1,
  id: "job-2",
  scheduledStartDate: "2026-01-16",
  scheduledEndDate: "2026-01-16",
  propertyAddress: { line1: "456 Oak Ave", city: "Chicago", state: "IL", zip: "60602" },
};

const unscheduledJob = {
  ...job1,
  id: "job-3",
  scheduledStartDate: null,
  scheduledEndDate: null,
  crewName: null,
};

describe("SchedulePage", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date(2026, 0, 10, 12, 0, 0));
    jest.clearAllMocks();
    mockSearchParamsGet.mockReturnValue(null);
    mockedJobsApi.listJobSchedule.mockResolvedValue([job1, job2]);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("renders month title, weekday headers, and loads schedule for the visible grid range", async () => {
    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    expect(screen.getByRole("heading", { level: 2, name: "January 2026" })).toBeInTheDocument();

    await waitFor(() => {
      expect(mockedJobsApi.listJobSchedule).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          from: JAN_2026_GRID_FROM,
          to: JAN_2026_GRID_TO,
        })
      );
    });

    await waitFor(() => {
      expect(screen.queryByText("Loading schedule…")).not.toBeInTheDocument();
    });

    for (const day of ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]) {
      expect(screen.getByText(day)).toBeInTheDocument();
    }

    expect(screen.queryByTestId("schedule-drag-overlay-preview")).not.toBeInTheDocument();
  });

  it("renders Unscheduled + day cells when listJobSchedule returns mixed jobs", async () => {
    mockedJobsApi.listJobSchedule.mockResolvedValue([job1, job2, unscheduledJob]);

    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getAllByText(/123 Main St/).length).toBeGreaterThanOrEqual(1);
    });
    expect(screen.getByText(/456 Oak Ave/)).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: /Open/i }).length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByRole("button", { name: /Edit/i }).length).toBeGreaterThanOrEqual(2);
  });

  it("shows Unscheduled section when includeUnscheduled is checked and data contains unscheduled jobs", async () => {
    mockedJobsApi.listJobSchedule.mockImplementation((_, params) => {
      const jobs = params?.includeUnscheduled ? [job1, unscheduledJob] : [job1, job2];
      return Promise.resolve(jobs);
    });

    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    expect(mockedJobsApi.listJobSchedule).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ includeUnscheduled: true })
    );

    expect(await screen.findByRole("heading", { name: "Unscheduled" })).toBeInTheDocument();
    expect(screen.getAllByText(/123 Main St/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByTestId("schedule-col-unscheduled-capacity")).toHaveTextContent("Jobs: 1");
  });

  it("Editing a job and clicking Save calls jobsApi.updateJob with correct payload", async () => {
    mockedJobsApi.updateJob.mockResolvedValue({
      ...job1,
      scheduledStartDate: "2026-01-20",
      scheduledEndDate: "2026-01-22",
      crewName: "Bravo",
    });

    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getAllByText(/123 Main St/).length).toBeGreaterThanOrEqual(1);
    });

    const job1Card = screen.getByTestId("schedule-card-job-1-date-2026-01-15");
    const editBtn = within(job1Card).getByRole("button", { name: /Edit/i });
    fireEvent.click(editBtn);

    await waitFor(() => {
      expect(within(job1Card).getAllByRole("button", { name: /^Save$/i }).length).toBeGreaterThan(0);
    });

    const startDateInput = document.getElementById("edit-start-date");
    const endDateInput = document.getElementById("edit-end-date");
    const crewInput = document.getElementById("edit-crew");
    if (startDateInput) fireEvent.change(startDateInput, { target: { value: "2026-01-20" } });
    if (endDateInput) fireEvent.change(endDateInput, { target: { value: "2026-01-22" } });
    if (crewInput) fireEvent.change(crewInput, { target: { value: "Bravo" } });

    const saveBtn = within(job1Card).getByRole("button", { name: /^Save$/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(mockedJobsApi.updateJob).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        expect.objectContaining({
          scheduledStartDate: "2026-01-20",
          scheduledEndDate: "2026-01-22",
          crewName: "Bravo",
          clearSchedule: false,
        })
      );
    });
  });

  it("renders multi-day jobs in each day they span", async () => {
    mockedJobsApi.listJobSchedule.mockResolvedValue([job1]);

    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(mockedJobsApi.listJobSchedule).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ from: JAN_2026_GRID_FROM, to: JAN_2026_GRID_TO })
      );
    });

    await waitFor(() => {
      expect(screen.getAllByText(/123 Main St/).length).toBe(3);
    });

    expect(screen.queryByTestId("schedule-drag-handle-job-1")).not.toBeInTheDocument();
    expect(screen.getByTestId("schedule-card-job-1-date-2026-01-15")).toHaveClass("cursor-grab");
    expect(screen.getByTestId("schedule-card-job-1-date-2026-01-16")).not.toHaveClass("cursor-grab");

    expect(screen.getByTestId("schedule-col-2026-01-15-capacity")).toHaveTextContent("Jobs: 1");
    expect(screen.getByTestId("schedule-col-2026-01-16-capacity")).toHaveTextContent("Jobs: 1");
    expect(screen.getByTestId("schedule-col-2026-01-17-capacity")).toHaveTextContent("Jobs: 1");
  });

  it("navigates to the next month and refetches with the new grid range", async () => {
    render(<SchedulePage />);

    await waitFor(() => {
      expect(mockedJobsApi.listJobSchedule).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ from: JAN_2026_GRID_FROM, to: JAN_2026_GRID_TO })
      );
    });

    const initialCalls = mockedJobsApi.listJobSchedule.mock.calls.length;
    fireEvent.click(screen.getByRole("button", { name: /Next/i }));

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 2, name: "February 2026" })).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(mockedJobsApi.listJobSchedule.mock.calls.length).toBeGreaterThan(initialCalls);
    });

    const lastCall = mockedJobsApi.listJobSchedule.mock.calls[mockedJobsApi.listJobSchedule.mock.calls.length - 1];
    expect(lastCall[1]).toEqual(
      expect.objectContaining({
        from: "2026-02-01",
        to: "2026-02-28",
      })
    );
  });
});
