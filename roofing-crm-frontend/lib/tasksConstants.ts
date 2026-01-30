import type { TaskStatus, TaskPriority } from "./types";

export const TASK_STATUS_LABELS: Record<TaskStatus, string> = {
  TODO: "To Do",
  IN_PROGRESS: "In Progress",
  COMPLETED: "Completed",
  CANCELED: "Canceled",
};

export const TASK_STATUS_COLORS: Record<TaskStatus, string> = {
  TODO: "bg-slate-100 text-slate-700 border-slate-200",
  IN_PROGRESS: "bg-amber-100 text-amber-700 border-amber-200",
  COMPLETED: "bg-green-100 text-green-700 border-green-200",
  CANCELED: "bg-red-100 text-red-700 border-red-200",
};

export const TASK_PRIORITY_LABELS: Record<TaskPriority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  URGENT: "Urgent",
};

export const TASK_PRIORITY_COLORS: Record<TaskPriority, string> = {
  LOW: "bg-slate-100 text-slate-700 border-slate-200",
  MEDIUM: "bg-blue-100 text-blue-700 border-blue-200",
  HIGH: "bg-amber-100 text-amber-700 border-amber-200",
  URGENT: "bg-red-100 text-red-700 border-red-200",
};

export const TASK_STATUS_OPTIONS: { value: TaskStatus; label: string }[] = [
  { value: "TODO", label: "To Do" },
  { value: "IN_PROGRESS", label: "In Progress" },
  { value: "COMPLETED", label: "Completed" },
  { value: "CANCELED", label: "Canceled" },
];

export const TASK_PRIORITY_OPTIONS: { value: TaskPriority; label: string }[] = [
  { value: "LOW", label: "Low" },
  { value: "MEDIUM", label: "Medium" },
  { value: "HIGH", label: "High" },
  { value: "URGENT", label: "Urgent" },
];

// Legacy aliases for backward compatibility
export const TASK_STATUSES: TaskStatus[] = [
  "TODO",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELED",
];
export const PRIORITIES: TaskPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
export const STATUS_LABELS = TASK_STATUS_LABELS;
export const STATUS_COLORS = TASK_STATUS_COLORS;
export const PRIORITY_LABELS = TASK_PRIORITY_LABELS;
export const PRIORITY_COLORS = TASK_PRIORITY_COLORS;
