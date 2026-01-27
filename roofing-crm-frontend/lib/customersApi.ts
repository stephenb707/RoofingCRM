import type { AxiosInstance } from "axios";
import type { CustomerDto, PageResponse, CreateCustomerRequest, UpdateCustomerRequest } from "./types";

export interface ListCustomersParams {
  page?: number;
  size?: number;
  q?: string | null;
}

/**
 * Fetch paginated list of customers with optional search.
 */
export async function listCustomers(
  api: AxiosInstance,
  params: ListCustomersParams = {}
): Promise<PageResponse<CustomerDto>> {
  const queryParams: Record<string, number | string> = {};
  if (params.page !== undefined) queryParams.page = params.page;
  if (params.size !== undefined) queryParams.size = params.size;
  if (params.q) queryParams.q = params.q;

  const res = await api.get<PageResponse<CustomerDto>>("/api/v1/customers", {
    params: queryParams,
  });
  return res.data;
}

/**
 * Fetch a single customer by ID.
 */
export async function getCustomer(
  api: AxiosInstance,
  customerId: string
): Promise<CustomerDto> {
  const res = await api.get<CustomerDto>(`/api/v1/customers/${customerId}`);
  return res.data;
}

/**
 * Create a new customer.
 */
export async function createCustomer(
  api: AxiosInstance,
  payload: CreateCustomerRequest
): Promise<CustomerDto> {
  const res = await api.post<CustomerDto>("/api/v1/customers", payload);
  return res.data;
}

/**
 * Update an existing customer.
 */
export async function updateCustomer(
  api: AxiosInstance,
  customerId: string,
  payload: UpdateCustomerRequest
): Promise<CustomerDto> {
  const res = await api.put<CustomerDto>(`/api/v1/customers/${customerId}`, payload);
  return res.data;
}
