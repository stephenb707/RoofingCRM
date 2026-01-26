/**
 * Centralized query key factory for React Query.
 * Keeps keys consistent and tenant-scoped.
 */

export const queryKeys = {
  customers: (tenantId: string | null) => ["customers", tenantId] as const,

  leadsList: (
    tenantId: string | null,
    status: string,
    source: string,
    search: string,
    page: number
  ) => ["leads", tenantId, status, source, search, page] as const,

  lead: (tenantId: string | null, leadId: string) =>
    ["lead", tenantId, leadId] as const,

  jobsList: (tenantId: string | null, status: string, page: number) =>
    ["jobs", tenantId, status, page] as const,

  job: (tenantId: string | null, jobId: string) =>
    ["job", tenantId, jobId] as const,

  estimatesForJob: (tenantId: string | null, jobId: string) =>
    ["estimates", tenantId, jobId] as const,

  estimate: (tenantId: string | null, estimateId: string) =>
    ["estimate", tenantId, estimateId] as const,
};
