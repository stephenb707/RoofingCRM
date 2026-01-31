import type { AxiosInstance } from "axios";
import type {
  TaskDto,
  TaskStatus,
  CreateTaskRequest,
  UpdateTaskRequest,
  PageResponse,
} from "./types";

export interface ListTasksFilters {
  status?: TaskStatus | null;
  assignedToUserId?: string | null;
  leadId?: string | null;
  jobId?: string | null;
  customerId?: string | null;
  dueBefore?: string | null;
  dueAfter?: string | null;
  page?: number;
  size?: number;
}

/**
 * Fetch paginated list of tasks with optional filters.
 * Uses the authenticated api instance (Authorization + X-Tenant-Id headers).
 * Only includes defined filter values in the request (no empty strings).
 */
export async function listTasks(
  api: AxiosInstance,
  filters: ListTasksFilters = {}
): Promise<PageResponse<TaskDto>> {
  const params: Record<string, string | number> = {};
  if (filters.status) params.status = filters.status;
  if (filters.assignedToUserId != null && filters.assignedToUserId !== "")
    params.assignedToUserId = filters.assignedToUserId;
  if (filters.leadId != null && filters.leadId !== "") params.leadId = filters.leadId;
  if (filters.jobId != null && filters.jobId !== "") params.jobId = filters.jobId;
  if (filters.customerId != null && filters.customerId !== "")
    params.customerId = filters.customerId;
  if (filters.dueBefore != null && filters.dueBefore !== "")
    params.dueBefore = filters.dueBefore;
  if (filters.dueAfter != null && filters.dueAfter !== "")
    params.dueAfter = filters.dueAfter;
  if (filters.page !== undefined) params.page = filters.page;
  if (filters.size !== undefined) params.size = filters.size;

  const res = await api.get<PageResponse<TaskDto>>("/api/v1/tasks", { params });
  return res.data;
}

/**
 * Fetch a single task by ID.
 */
export async function getTask(
  api: AxiosInstance,
  taskId: string
): Promise<TaskDto> {
  const res = await api.get<TaskDto>(`/api/v1/tasks/${taskId}`);
  return res.data;
}

/**
 * Create a new task.
 */
export async function createTask(
  api: AxiosInstance,
  payload: CreateTaskRequest
): Promise<TaskDto> {
  const res = await api.post<TaskDto>("/api/v1/tasks", payload);
  return res.data;
}

/**
 * Update an existing task.
 */
export async function updateTask(
  api: AxiosInstance,
  taskId: string,
  payload: UpdateTaskRequest
): Promise<TaskDto> {
  const res = await api.put<TaskDto>(`/api/v1/tasks/${taskId}`, payload);
  return res.data;
}
