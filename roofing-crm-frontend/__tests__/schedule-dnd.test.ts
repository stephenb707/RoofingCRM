import {
  computeScheduleUpdate,
  applyOptimisticSchedulingTagChange,
} from "@/lib/scheduleDnd";
import type { JobDto } from "@/lib/types";

const baseJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  status: "SCHEDULED",
  type: "REPLACEMENT",
  propertyAddress: null,
  scheduledStartDate: "2026-02-01",
  scheduledEndDate: "2026-02-03",
  internalNotes: null,
  crewName: null,
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
};

describe("computeScheduleUpdate", () => {
  it("drop unscheduled returns { clearSchedule: true }", () => {
    const result = computeScheduleUpdate(baseJob, { type: "unscheduled" });
    expect(result).toEqual({ clearSchedule: true });
  });

  it("drop scheduled 1-day job onto date → start=end=dateKey", () => {
    const oneDayJob: JobDto = {
      ...baseJob,
      scheduledStartDate: "2026-02-01",
      scheduledEndDate: "2026-02-01",
    };
    const result = computeScheduleUpdate(oneDayJob, {
      type: "date",
      dateKey: "2026-02-15",
    });
    expect(result.clearSchedule).toBe(false);
    expect(result.scheduledStartDate).toBe("2026-02-15");
    expect(result.scheduledEndDate).toBe("2026-02-15");
  });

  it("drop scheduled multi-day job onto date preserves duration", () => {
    const result = computeScheduleUpdate(baseJob, {
      type: "date",
      dateKey: "2026-02-10",
    });
    expect(result.clearSchedule).toBe(false);
    expect(result.scheduledStartDate).toBe("2026-02-10");
    expect(result.scheduledEndDate).toBe("2026-02-12");
  });
});

describe("applyOptimisticSchedulingTagChange", () => {
  it("drop to date column: status SCHEDULED, dates set", () => {
    const unscheduledJob: JobDto = {
      ...baseJob,
      status: "UNSCHEDULED",
      scheduledStartDate: null,
      scheduledEndDate: null,
    };
    const update = {
      clearSchedule: false,
      scheduledStartDate: "2026-02-15",
      scheduledEndDate: "2026-02-15",
    };
    const result = applyOptimisticSchedulingTagChange(unscheduledJob, update);
    expect(result.status).toBe("SCHEDULED");
    expect(result.scheduledStartDate).toBe("2026-02-15");
    expect(result.scheduledEndDate).toBe("2026-02-15");
  });

  it("drop to unscheduled column: status UNSCHEDULED, dates cleared", () => {
    const update = { clearSchedule: true };
    const result = applyOptimisticSchedulingTagChange(baseJob, update);
    expect(result.status).toBe("UNSCHEDULED");
    expect(result.scheduledStartDate).toBeNull();
    expect(result.scheduledEndDate).toBeNull();
  });
});
