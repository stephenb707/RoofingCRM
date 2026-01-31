"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { listTasks } from "@/lib/tasksApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  TASK_STATUS_LABELS,
  TASK_STATUS_COLORS,
  TASK_PRIORITY_LABELS,
  TASK_PRIORITY_COLORS,
} from "@/lib/tasksConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/StatusBadge";

export interface TasksSectionProps {
  entityType: "lead" | "job";
  entityId: string;
}

export function TasksSection({ entityType, entityId }: TasksSectionProps) {
  const { api, auth, ready } = useAuthReady();

  const filters =
    entityType === "lead"
      ? { leadId: entityId, page: 0, size: 50 }
      : { jobId: entityId, page: 0, size: 50 };

  const tasksQuery = useQuery({
    queryKey:
      entityType === "lead"
        ? queryKeys.tasksForLead(auth.selectedTenantId, entityId)
        : queryKeys.tasksForJob(auth.selectedTenantId, entityId),
    queryFn: () => listTasks(api, filters),
    enabled: ready && !!entityId,
  });

  const newTaskHref =
    entityType === "lead"
      ? `/app/tasks/new?leadId=${entityId}`
      : `/app/tasks/new?jobId=${entityId}`;

  if (tasksQuery.isLoading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Tasks</h2>
        <p className="text-sm text-slate-500">Loading tasks…</p>
      </div>
    );
  }

  if (tasksQuery.isError) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Tasks</h2>
        <p className="text-sm text-red-600">
          {getApiErrorMessage(tasksQuery.error, "Failed to load tasks")}
        </p>
      </div>
    );
  }

  const tasks = tasksQuery.data?.content ?? [];
  const open = tasks.filter((t) => t.status === "TODO" || t.status === "IN_PROGRESS");
  const completed = tasks.filter((t) => t.status === "COMPLETED" || t.status === "CANCELED");
  const sorted = [...open, ...completed];

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">Tasks</h2>
      {sorted.length === 0 ? (
        <p className="text-sm text-slate-500">
          No tasks yet.{" "}
          <Link href={newTaskHref} className="text-sky-600 hover:text-sky-700">
            Create one
          </Link>
          .
        </p>
      ) : (
        <ul className="divide-y divide-slate-100">
          {sorted.map((task) => (
            <li key={task.taskId} className="py-3 first:pt-0">
              <Link
                href={`/app/tasks/${task.taskId}`}
                className="block hover:bg-slate-50 -mx-2 px-2 py-1 rounded-lg transition-colors"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-medium text-slate-800">{task.title}</span>
                  <StatusBadge
                    label={TASK_STATUS_LABELS[task.status]}
                    className={TASK_STATUS_COLORS[task.status]}
                  />
                </div>
                <div className="flex items-center gap-3 mt-1 text-xs text-slate-500">
                  <span
                    className={`${TASK_PRIORITY_COLORS[task.priority]} px-1.5 py-0.5 rounded text-xs`}
                  >
                    {TASK_PRIORITY_LABELS[task.priority]}
                  </span>
                  {task.dueAt && <span>Due {formatDateTime(task.dueAt)}</span>}
                  {task.assignedToName && <span>{task.assignedToName}</span>}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
