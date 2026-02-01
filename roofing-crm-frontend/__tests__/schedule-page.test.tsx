import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import SchedulePage from "@/app/app/schedule/page";
import * as scheduleApi from "@/lib/scheduleApi";
import * as jobsApi from "@/lib/jobsApi";

jest.mock("@/lib/scheduleApi");
jest.mock("@/lib/jobsApi");

jest.mock("@/components/DateRangePicker", () => ({
  DateRangePicker: ({
    startDate,
    endDate,
    onChange,
    id,
  }: {
    startDate: string;
    endDate: string;
    onChange: (start: string, end: string) => void;
    id?: string;
  }) => (
    <div data-testid="date-range-picker" id={id}>
      <input
        id={id === "schedule-daterange" ? "schedule-date-start" : "reschedule-start"}
        data-testid={id === "schedule-daterange" ? "schedule-date-start" : "reschedule-start"}
        type="date"
        value={startDate}
        onChange={(e) => onChange(e.target.value, endDate)}
      />
      <input
        id={id === "schedule-daterange" ? "schedule-date-end" : "reschedule-end"}
        data-testid={id === "schedule-daterange" ? "schedule-date-end" : "reschedule-end"}
        type="date"
        value={endDate}
        onChange={(e) => onChange(startDate, e.target.value)}
      />
    </div>
  ),
}));

const mockedScheduleApi = scheduleApi as jest.Mocked<typeof scheduleApi>;
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;

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
    jest.clearAllMocks();
    mockedScheduleApi.listScheduleJobs.mockResolvedValue({
      content: [job1, job2],
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 200,
      first: true,
      last: true,
    });
  });

  it("renders grouped day sections and job cards based on returned jobs", async () => {
    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    const startInput = document.getElementById("schedule-date-start");
    const endInput = document.getElementById("schedule-date-end");
    if (!startInput || !endInput) throw new Error("Date inputs not found");
    fireEvent.change(startInput, { target: { value: "2026-01-01" } });
    fireEvent.change(endInput, { target: { value: "2026-01-31" } });

    await waitFor(() => {
      expect(mockedScheduleApi.listScheduleJobs).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ startDate: "2026-01-01", endDate: "2026-01-31" })
      );
    });

    await waitFor(() => {
      expect(screen.getByText(/123 Main St/)).toBeInTheDocument();
    });
    expect(screen.getByText(/456 Oak Ave/)).toBeInTheDocument();
    expect(screen.getByText(/456 Oak Ave/)).toBeInTheDocument();
    expect(screen.getAllByRole("link", { name: /Open/i }).length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByRole("button", { name: /Reschedule/i }).length).toBeGreaterThanOrEqual(2);
  });

  it("shows Unscheduled section when includeUnscheduled is checked and data contains unscheduled jobs", async () => {
    mockedScheduleApi.listScheduleJobs.mockImplementation((_, params) => {
      const content = params?.includeUnscheduled ? [job1, unscheduledJob] : [job1, job2];
      return Promise.resolve({
        content,
        totalElements: content.length,
        totalPages: 1,
        number: 0,
        size: 200,
        first: true,
        last: true,
      });
    });

    render(<SchedulePage />);

    await waitFor(() => {
      expect(screen.getByText("Schedule")).toBeInTheDocument();
    });

    const checkbox = document.getElementById("schedule-unscheduled");
    if (!checkbox) throw new Error("schedule-unscheduled not found");
    fireEvent.click(checkbox);

    await waitFor(() => {
      expect(mockedScheduleApi.listScheduleJobs).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ includeUnscheduled: true })
      );
    });

    expect(await screen.findByRole("heading", { name: "Unscheduled" })).toBeInTheDocument();
    expect(screen.getByText(/123 Main St/)).toBeInTheDocument();
  });

  it("Reschedule flow: click Reschedule, change dates/crew, click Save -> updateJob called with expected payload", async () => {
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

    const startInput = document.getElementById("schedule-date-start");
    const endInput = document.getElementById("schedule-date-end");
    if (!startInput || !endInput) throw new Error("Date inputs not found");
    fireEvent.change(startInput, { target: { value: "2026-01-01" } });
    fireEvent.change(endInput, { target: { value: "2026-01-31" } });

    await waitFor(() => {
      expect(screen.getByText(/123 Main St/)).toBeInTheDocument();
    });

    const rescheduleBtns = screen.getAllByRole("button", { name: /Reschedule/i });
    fireEvent.click(rescheduleBtns[0]);

    await waitFor(() => {
      expect(document.getElementById("reschedule-start")).toBeInTheDocument();
    });

    const rescheduleStartInput = document.getElementById("reschedule-start");
    const rescheduleEndInput = document.getElementById("reschedule-end");
    const crewInput = document.getElementById("reschedule-crew");
    if (!rescheduleStartInput || !rescheduleEndInput || !crewInput)
      throw new Error("Reschedule inputs not found");

    fireEvent.change(rescheduleStartInput, { target: { value: "2026-01-20" } });
    fireEvent.change(rescheduleEndInput, { target: { value: "2026-01-22" } });
    fireEvent.change(crewInput, { target: { value: "Bravo" } });

    const saveBtn = screen.getByRole("button", { name: /^Save$/ });
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
});
