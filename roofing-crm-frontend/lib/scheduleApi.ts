import type { AxiosInstance } from "axios";
import type { JobDto, PageResponse } from "./types";

export interface ListScheduleJobsParams {
  startDate: string;
  endDate: string;
  statusDefinitionId?: string | null;
  crewName?: string | null;
  includeUnscheduled?: boolean;
  page?: number;
  size?: number;
}

export async function listScheduleJobs(
  api: AxiosInstance,
  params: ListScheduleJobsParams
): Promise<PageResponse<JobDto>> {
  const queryParams: Record<string, string | number | boolean> = {
    startDate: params.startDate,
    endDate: params.endDate,
  };
  if (params.statusDefinitionId != null && String(params.statusDefinitionId) !== "") {
    queryParams.statusDefinitionId = params.statusDefinitionId;
  }
  if (params.crewName != null && params.crewName !== "") {
    queryParams.crewName = params.crewName;
  }
  if (params.includeUnscheduled !== undefined) {
    queryParams.includeUnscheduled = params.includeUnscheduled;
  }
  if (params.page !== undefined) {
    queryParams.page = params.page;
  }
  if (params.size !== undefined) {
    queryParams.size = params.size;
  }

  const res = await api.get<PageResponse<JobDto>>("/api/v1/schedule/jobs", {
    params: queryParams,
  });
  return res.data;
}
