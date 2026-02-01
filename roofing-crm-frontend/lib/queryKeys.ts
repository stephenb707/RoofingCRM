/**
 * Centralized query key factory for React Query.
 * Keeps keys consistent and tenant-scoped.
 */

export const queryKeys = {
  customers: (tenantId: string | null) => ["customers", tenantId] as const,
  customersList: (tenantId: string | null, q: string | null, page: number) =>
    ["customers", tenantId, q ?? "", page] as const,
  customer: (tenantId: string | null, customerId: string) =>
    ["customer", tenantId, customerId] as const,

  leadsList: (
    tenantId: string | null,
    status: string | null,
    customerId: string | null,
    page: number
  ) => ["leads", tenantId, status ?? "", customerId ?? "", page] as const,

  lead: (tenantId: string | null, leadId: string) =>
    ["lead", tenantId, leadId] as const,

  leadsPipeline: (tenantId: string | null) =>
    ["leadsPipeline", tenantId] as const,

  activityForEntity: (tenantId: string | null, entityType: string, entityId: string) =>
    ["activity", tenantId, entityType, entityId] as const,

  jobsList: (tenantId: string | null, status: string | null, customerId: string | null, page: number) =>
    ["jobs", tenantId, status ?? "", customerId ?? "", page] as const,

  job: (tenantId: string | null, jobId: string) =>
    ["job", tenantId, jobId] as const,

  jobSchedule: (
    tenantId: string | null,
    from: string,
    to: string,
    status: string | null,
    crewName: string | null,
    includeUnscheduled: boolean
  ) =>
    [
      "jobSchedule",
      tenantId,
      from,
      to,
      status ?? null,
      crewName ?? null,
      includeUnscheduled ?? true,
    ] as const,

  scheduleJobs: (
    tenantId: string | null,
    startDate: string,
    endDate: string,
    status: string | null,
    crewName: string | null,
    includeUnscheduled: boolean
  ) =>
    [
      "scheduleJobs",
      tenantId,
      startDate,
      endDate,
      status ?? "",
      crewName ?? "",
      includeUnscheduled,
    ] as const,

  estimatesForJob: (tenantId: string | null, jobId: string) =>
    ["estimates", tenantId, jobId] as const,

  estimate: (tenantId: string | null, estimateId: string) =>
    ["estimate", tenantId, estimateId] as const,

  tasksList: (tenantId: string | null, filters: object) =>
    ["tasks", tenantId, "list", filters] as const,

  taskDetail: (tenantId: string | null, taskId: string) =>
    ["tasks", tenantId, taskId] as const,

  tasksForLead: (tenantId: string | null, leadId: string) =>
    ["tasks", tenantId, "lead", leadId] as const,

  tasksForJob: (tenantId: string | null, jobId: string) =>
    ["tasks", tenantId, "job", jobId] as const,

  tasksForCustomer: (tenantId: string | null, customerId: string) =>
    ["tasks", tenantId, "customer", customerId] as const,

  teamMembers: (tenantId: string | null) =>
    ["teamMembers", tenantId] as const,
  teamInvites: (tenantId: string | null) =>
    ["teamInvites", tenantId] as const,

  leadAttachments: (tenantId: string | null, leadId: string) =>
    ["attachments", "lead", tenantId, leadId] as const,
  jobAttachments: (tenantId: string | null, jobId: string) =>
    ["attachments", "job", tenantId, jobId] as const,
  leadCommLogs: (tenantId: string | null, leadId: string) =>
    ["commLogs", "lead", tenantId, leadId] as const,
  jobCommLogs: (tenantId: string | null, jobId: string) =>
    ["commLogs", "job", tenantId, jobId] as const,
};
