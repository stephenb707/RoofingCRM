"use client";

import { useState, useMemo, useCallback } from "react";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import Link from "next/link";
import { useAuth } from "@/lib/AuthContext";
import { listTasks } from "@/lib/tasksApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  TASK_STATUS_OPTIONS,
  TASK_STATUS_LABELS,
  TASK_STATUS_COLORS,
  TASK_PRIORITY_LABELS,
  TASK_PRIORITY_COLORS,
} from "@/lib/tasksConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatDateTime } from "@/lib/format";
import { StatusBadge } from "@/components/StatusBadge";
import type { TaskStatus } from "@/lib/types";

function TaskRelatedCell({ task }: { task: { leadId?: string | null; jobId?: string | null; customerId?: string | null } }) {
  if (task.leadId) {
    return (
      <Link href={`/app/leads/${task.leadId}`} className="text-sky-600 hover:text-sky-700 text-sm">
        Lead
      </Link>
    );
  }
  if (task.jobId) {
    return (
      <Link href={`/app/jobs/${task.jobId}`} className="text-sky-600 hover:text-sky-700 text-sm">
        Job
      </Link>
    );
  }
  if (task.customerId) {
    return (
      <Link href={`/app/customers/${task.customerId}`} className="text-sky-600 hover:text-sky-700 text-sm">
        Customer
      </Link>
    );
  }
  return <span className="text-slate-400 text-sm">—</span>;
}

export default function TasksPage() {
  const { api, auth } = useAuth();
  const [statusFilter, setStatusFilter] = useState<TaskStatus | "">("");
  const [myTasksOnly, setMyTasksOnly] = useState(false);
  const [dueBefore, setDueBefore] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const filters = useMemo(
    () => {
      const dueBeforeIso = dueBefore
        ? `${dueBefore}T23:59:59.999Z`
        : undefined;
      return {
        status: statusFilter || undefined,
        assignedToUserId: myTasksOnly && auth.userId ? auth.userId : undefined,
        dueBefore: dueBeforeIso,
        page,
        size: pageSize,
      };
    },
    [statusFilter, myTasksOnly, auth.userId, dueBefore, page]
  );

  const queryKey = useMemo(
    () => queryKeys.tasksList(auth.selectedTenantId, filters),
    [auth.selectedTenantId, filters]
  );

  const { data, isLoading, isError, error, isFetching } = useQuery({
    queryKey,
    queryFn: () => listTasks(api, { ...filters }),
    enabled: !!(auth.token && auth.selectedTenantId),
    placeholderData: keepPreviousData,
  });

  const handleStatusChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      setStatusFilter(e.target.value as TaskStatus | "");
      setPage(0);
    },
    []
  );

  const handleClearFilters = useCallback(() => {
    setStatusFilter("");
    setMyTasksOnly(false);
    setDueBefore("");
    setPage(0);
  }, []);

  const tasks = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const hasFilters = statusFilter || myTasksOnly || dueBefore;

  return (
    <div className="max-w-6xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Tasks</h1>
          <p className="text-sm text-slate-500 mt-1">Manage tasks and follow-ups</p>
        </div>
        <Link
          href="/app/tasks/new"
          className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors shadow-sm"
        >
          + New Task
        </Link>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-4 mb-6">
        <div className="flex flex-wrap items-center gap-4">
          <div className="flex items-center gap-2">
            <label htmlFor="status-filter" className="text-sm font-medium text-slate-700">
              Status:
            </label>
            <select
              id="status-filter"
              value={statusFilter}
              onChange={handleStatusChange}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            >
              <option value="">All</option>
              {TASK_STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={myTasksOnly}
              onChange={(e) => {
                setMyTasksOnly(e.target.checked);
                setPage(0);
              }}
              className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
            />
            <span className="text-sm font-medium text-slate-700">My tasks</span>
          </label>

          <div className="flex items-center gap-2">
            <label htmlFor="due-before" className="text-sm font-medium text-slate-700">
              Due before:
            </label>
            <input
              id="due-before"
              type="date"
              value={dueBefore}
              onChange={(e) => {
                setDueBefore(e.target.value);
                setPage(0);
              }}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            />
          </div>

          {hasFilters && (
            <button
              onClick={handleClearFilters}
              className="text-sm text-slate-500 hover:text-slate-700 underline"
            >
              Clear filters
            </button>
          )}
        </div>
      </div>

      {isLoading && tasks.length === 0 && (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4"></div>
          <p className="text-sm text-slate-500">Loading tasks...</p>
        </div>
      )}

      {isError && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <div className="flex items-start gap-3">
            <svg className="w-5 h-5 text-red-500 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
            <div>
              <h3 className="text-sm font-medium text-red-800">Failed to load tasks</h3>
              <p className="text-sm text-red-600 mt-1">Check that the backend is running and try again.</p>
              <p className="text-xs text-red-500 mt-2 font-mono">{getApiErrorMessage(error, "Unknown error")}</p>
            </div>
          </div>
        </div>
      )}

      {!isLoading && !isError && tasks.length === 0 && (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="w-16 h-16 bg-slate-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
              />
            </svg>
          </div>
          <h3 className="text-lg font-medium text-slate-800 mb-1">
            {hasFilters ? "No tasks match your filters" : "No tasks yet"}
          </h3>
          <p className="text-sm text-slate-500 mb-4">
            {hasFilters ? "Try adjusting your filters or create a new task." : "Get started by adding your first task."}
          </p>
          <Link
            href="/app/tasks/new"
            className="inline-flex px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            + New Task
          </Link>
        </div>
      )}

      {!isError && tasks.length > 0 && (
        <>
          <div className="bg-white shadow-sm rounded-xl border border-slate-200 overflow-hidden">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Title</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Status</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Priority</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Due</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Assignee</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Related</th>
                  <th className="text-right px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {tasks.map((task) => (
                  <tr key={task.taskId} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4">
                      <Link
                        href={`/app/tasks/${task.taskId}`}
                        className="font-medium text-slate-800 hover:text-sky-600"
                      >
                        {task.title || "—"}
                      </Link>
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge label={TASK_STATUS_LABELS[task.status]} className={TASK_STATUS_COLORS[task.status]} />
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge label={TASK_PRIORITY_LABELS[task.priority]} className={TASK_PRIORITY_COLORS[task.priority]} />
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {task.dueAt ? formatDateTime(task.dueAt) : "—"}
                    </td>
                    <td className="px-6 py-4 text-sm text-slate-600">
                      {task.assignedToName || "Unassigned"}
                    </td>
                    <td className="px-6 py-4">
                      <TaskRelatedCell task={task} />
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link
                        href={`/app/tasks/${task.taskId}`}
                        className="text-sm text-sky-600 hover:text-sky-700 font-medium"
                      >
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-slate-500">
                {isFetching ? <span className="text-slate-500">Loading…</span> : <>Page {page + 1} of {totalPages}</>}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1.5 text-sm font-medium border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1.5 text-sm font-medium border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
