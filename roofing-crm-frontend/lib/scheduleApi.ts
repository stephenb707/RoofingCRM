import type { AxiosInstance } from "axios";
import type { JobDto, JobStatus, PageResponse } from "./types";

export interface ListScheduleJobsParams {
  startDate: string;
  endDate: string;
  status?: JobStatus | null;
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
  if (params.status != null && String(params.status) !== "") {
    queryParams.status = params.status;
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
