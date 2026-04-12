import {
  computeScheduleUpdate,
  applyOptimisticSchedulingTagChange,
} from "@/lib/scheduleDnd";
import type { PipelineStatusDefinitionDto } from "@/lib/pipelineStatusesApi";
import type { JobDto } from "@/lib/types";

const jobPipelineDefs: PipelineStatusDefinitionDto[] = [
  {
    id: "def-unsched",
    pipelineType: "JOB",
    systemKey: "UNSCHEDULED",
    label: "Unscheduled",
    sortOrder: 0,
    builtIn: true,
    active: true,
  },
  {
    id: "def-sched",
    pipelineType: "JOB",
    systemKey: "SCHEDULED",
    label: "Scheduled",
    sortOrder: 1,
    builtIn: true,
    active: true,
  },
];

const baseJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  statusDefinitionId: "def-sched",
  statusKey: "SCHEDULED",
  statusLabel: "Scheduled",
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

  it("drop job with no end date keeps single-day behavior", () => {
    const noEndJob: JobDto = {
      ...baseJob,
      scheduledStartDate: "2026-02-01",
      scheduledEndDate: null,
    };
    const result = computeScheduleUpdate(noEndJob, {
      type: "date",
      dateKey: "2026-02-10",
    });
    expect(result.clearSchedule).toBe(false);
    expect(result.scheduledStartDate).toBe("2026-02-10");
    expect(result.scheduledEndDate).toBe("2026-02-10");
  });
});

describe("applyOptimisticSchedulingTagChange", () => {
  it("drop to date column: status SCHEDULED, dates set", () => {
    const unscheduledJob: JobDto = {
      ...baseJob,
      statusDefinitionId: "def-unsched",
      statusKey: "UNSCHEDULED",
      statusLabel: "Unscheduled",
      scheduledStartDate: null,
      scheduledEndDate: null,
    };
    const update = {
      clearSchedule: false,
      scheduledStartDate: "2026-02-15",
      scheduledEndDate: "2026-02-15",
    };
    const result = applyOptimisticSchedulingTagChange(unscheduledJob, update, jobPipelineDefs);
    expect(result.statusKey).toBe("SCHEDULED");
    expect(result.statusDefinitionId).toBe("def-sched");
    expect(result.scheduledStartDate).toBe("2026-02-15");
    expect(result.scheduledEndDate).toBe("2026-02-15");
  });

  it("drop to unscheduled column: status UNSCHEDULED, dates cleared", () => {
    const update = { clearSchedule: true };
    const result = applyOptimisticSchedulingTagChange(baseJob, update, jobPipelineDefs);
    expect(result.statusKey).toBe("UNSCHEDULED");
    expect(result.statusDefinitionId).toBe("def-unsched");
    expect(result.scheduledStartDate).toBeNull();
    expect(result.scheduledEndDate).toBeNull();
  });

  it("keeps non-scheduling statuses while updating dates", () => {
    const inProgressJob: JobDto = {
      ...baseJob,
      statusDefinitionId: "def-ip",
      statusKey: "IN_PROGRESS",
      statusLabel: "In progress",
    };
    const update = {
      clearSchedule: false,
      scheduledStartDate: "2026-02-20",
      scheduledEndDate: "2026-02-21",
    };
    const result = applyOptimisticSchedulingTagChange(inProgressJob, update, jobPipelineDefs);
    expect(result.statusKey).toBe("IN_PROGRESS");
    expect(result.statusDefinitionId).toBe("def-ip");
    expect(result.scheduledStartDate).toBe("2026-02-20");
    expect(result.scheduledEndDate).toBe("2026-02-21");
  });
});
