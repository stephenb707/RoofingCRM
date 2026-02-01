import type { AxiosInstance } from "axios";

export interface UserPickerItem {
  id: string;
  name: string;
  email: string;
}

export interface ListUsersParams {
  q?: string | null;
  limit?: number;
}

/**
 * Search users in the current tenant (for assignee picker, etc.)
 */
export async function listUsers(
  api: AxiosInstance,
  params: ListUsersParams = {}
): Promise<UserPickerItem[]> {
  const queryParams: Record<string, string | number> = {};
  if (params.q != null && params.q !== "") {
    queryParams.q = params.q;
  }
  if (params.limit != null) {
    queryParams.limit = params.limit;
  }

  const res = await api.get<UserPickerItem[]>("/api/v1/users", {
    params: queryParams,
  });
  return res.data;
}
