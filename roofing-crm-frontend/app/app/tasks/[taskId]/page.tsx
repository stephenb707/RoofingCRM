"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getTask, updateTask } from "@/lib/tasksApi";
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
import type { TaskStatus } from "@/lib/types";

export default function TaskDetailPage() {
  const params = useParams();
  const taskId = params.taskId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [updateError, setUpdateError] = useState<string | null>(null);

  const queryKey = queryKeys.taskDetail(auth.selectedTenantId, taskId);

  const { data: task, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: () => getTask(api, taskId),
    enabled: ready && !!taskId,
  });

  const mutation = useMutation({
    mutationFn: async (status: TaskStatus) => updateTask(api, taskId, { status }),
    onSuccess: () => {
      setUpdateError(null);
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: ["tasks", auth.selectedTenantId] });
    },
    onError: (err: unknown) => {
      console.error("Failed to update task:", err);
      setUpdateError(getApiErrorMessage(err, "Failed to update. Please try again."));
    },
  });

  const handleQuickStatus = (newStatus: TaskStatus) => {
    if (newStatus === task?.status) return;
    setUpdateError(null);
    mutation.mutate(newStatus);
  };

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4"></div>
          <p className="text-sm text-slate-500">Loading task details...</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-4xl mx-auto">
        <Link href="/app/tasks" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Tasks
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load task</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The task could not be found.")}</p>
        </div>
      </div>
    );
  }

  if (!task) return null;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Link href="/app/tasks" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Tasks
        </Link>
        <div className="flex items-start justify-between">
          <h1 className="text-2xl font-bold text-slate-800">{task.title}</h1>
          <StatusBadge label={TASK_STATUS_LABELS[task.status]} className={TASK_STATUS_COLORS[task.status]} />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Task Details</h2>
            {task.description && (
              <p className="text-sm text-slate-700 whitespace-pre-wrap mb-4">{task.description}</p>
            )}
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Status</dt>
                <dd className="mt-1">
                  <StatusBadge label={TASK_STATUS_LABELS[task.status]} className={TASK_STATUS_COLORS[task.status]} />
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Priority</dt>
                <dd className="mt-1">
                  <StatusBadge label={TASK_PRIORITY_LABELS[task.priority]} className={TASK_PRIORITY_COLORS[task.priority]} />
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Due</dt>
                <dd className="mt-1 text-sm text-slate-800">{task.dueAt ? formatDateTime(task.dueAt) : "—"}</dd>
              </div>
              {task.completedAt && (
                <div>
                  <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Completed</dt>
                  <dd className="mt-1 text-sm text-slate-800">{formatDateTime(task.completedAt)}</dd>
                </div>
              )}
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Assignee</dt>
                <dd className="mt-1 text-sm text-slate-800">{task.assignedToName || "Unassigned"}</dd>
              </div>
            </dl>
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Quick Actions</h2>
            {updateError && (
              <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {updateError}
              </div>
            )}
            <div className="space-y-2">
              {task.status !== "IN_PROGRESS" && (
                <button
                  onClick={() => handleQuickStatus("IN_PROGRESS")}
                  disabled={mutation.isPending}
                  className="w-full px-4 py-2.5 text-sm font-medium text-amber-700 bg-amber-50 border border-amber-200 rounded-lg hover:bg-amber-100 transition-colors disabled:opacity-60"
                >
                  Mark In Progress
                </button>
              )}
              {task.status !== "COMPLETED" && (
                <button
                  onClick={() => handleQuickStatus("COMPLETED")}
                  disabled={mutation.isPending}
                  className="w-full px-4 py-2.5 text-sm font-medium text-green-700 bg-green-50 border border-green-200 rounded-lg hover:bg-green-100 transition-colors disabled:opacity-60"
                >
                  Mark Completed
                </button>
              )}
              {(task.status === "COMPLETED" || task.status === "CANCELED") && (
                <button
                  onClick={() => handleQuickStatus("TODO")}
                  disabled={mutation.isPending}
                  className="w-full px-4 py-2.5 text-sm font-medium text-slate-700 bg-slate-50 border border-slate-200 rounded-lg hover:bg-slate-100 transition-colors disabled:opacity-60"
                >
                  Reopen
                </button>
              )}
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Related</h2>
            <div className="space-y-2">
              {task.leadId && (
                <Link href={`/app/leads/${task.leadId}`} className="block text-sm text-sky-600 hover:text-sky-700">
                  View Lead
                </Link>
              )}
              {task.jobId && (
                <Link href={`/app/jobs/${task.jobId}`} className="block text-sm text-sky-600 hover:text-sky-700">
                  View Job
                </Link>
              )}
              {task.customerId && (
                <Link href={`/app/customers/${task.customerId}`} className="block text-sm text-sky-600 hover:text-sky-700">
                  View Customer
                </Link>
              )}
              {!task.leadId && !task.jobId && !task.customerId && (
                <p className="text-sm text-slate-500">No related entities</p>
              )}
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Details</h2>
            <dl className="space-y-3">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Created</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDateTime(task.createdAt)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Last Updated</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDateTime(task.updatedAt)}</dd>
              </div>
            </dl>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Actions</h2>
            <div className="space-y-2">
              <Link
                href={`/app/tasks/${taskId}/edit`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                Edit
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
