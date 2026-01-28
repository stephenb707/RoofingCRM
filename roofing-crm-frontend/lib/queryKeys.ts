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

  jobsList: (tenantId: string | null, status: string | null, customerId: string | null, page: number) =>
    ["jobs", tenantId, status ?? "", customerId ?? "", page] as const,

  job: (tenantId: string | null, jobId: string) =>
    ["job", tenantId, jobId] as const,

  estimatesForJob: (tenantId: string | null, jobId: string) =>
    ["estimates", tenantId, jobId] as const,

  estimate: (tenantId: string | null, estimateId: string) =>
    ["estimate", tenantId, estimateId] as const,

  leadAttachments: (tenantId: string | null, leadId: string) =>
    ["attachments", "lead", tenantId, leadId] as const,
  jobAttachments: (tenantId: string | null, jobId: string) =>
    ["attachments", "job", tenantId, jobId] as const,
  leadCommLogs: (tenantId: string | null, leadId: string) =>
    ["commLogs", "lead", tenantId, leadId] as const,
  jobCommLogs: (tenantId: string | null, jobId: string) =>
    ["commLogs", "job", tenantId, jobId] as const,
};
