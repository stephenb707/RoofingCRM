import type { AxiosInstance } from "axios";
import type { DashboardSummaryDto } from "./types";

export async function getDashboardSummary(
  api: AxiosInstance
): Promise<DashboardSummaryDto> {
  const res = await api.get<DashboardSummaryDto>("/api/v1/dashboard/summary");
  return res.data;
}
