import type { AxiosInstance } from "axios";
import type { CustomerDto, PageResponse } from "./types";

export interface ListCustomersParams {
  page?: number;
  size?: number;
}

/**
 * Fetch paginated list of customers.
 */
export async function listCustomers(
  api: AxiosInstance,
  params: ListCustomersParams = {}
): Promise<PageResponse<CustomerDto>> {
  const queryParams: Record<string, number> = {};
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;

  const res = await api.get<PageResponse<CustomerDto>>("/api/v1/customers", {
    params: queryParams,
  });
  return res.data;
}
