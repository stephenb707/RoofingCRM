"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { listEstimatesForJob } from "@/lib/estimatesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { ESTIMATE_STATUS_LABELS, ESTIMATE_STATUS_COLORS } from "@/lib/estimatesConstants";
import type { EstimateStatus } from "@/lib/types";
import { formatDate, formatMoney } from "@/lib/format";
import { StatusBadge } from "@/components/StatusBadge";

export default function JobEstimatesPage() {
  const params = useParams();
  const jobId = params.jobId as string;
  const { api, auth } = useAuth();

  const { data: estimates, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, jobId),
    queryFn: () => listEstimatesForJob(api, jobId),
    enabled: !!auth.selectedTenantId && !!jobId,
  });

  const list = estimates ?? [];

  return (
    <div className="max-w-6xl mx-auto">
      <div className="mb-6">
        <Link
          href={`/app/jobs/${jobId}`}
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Job
        </Link>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Estimates</h1>
            <p className="text-sm text-slate-500 mt-1">Estimates for this job</p>
          </div>
          <Link
            href={`/app/jobs/${jobId}/estimates/new`}
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors shadow-sm"
          >
            + New Estimate
          </Link>
        </div>
      </div>

      {isLoading && (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading estimates…</p>
        </div>
      )}

      {isError && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load estimates</h3>
          <p className="text-sm text-red-600 mt-1">
            {getApiErrorMessage(error, "An error occurred. Please try again.")}
          </p>
        </div>
      )}

      {!isLoading && !isError && list.length === 0 && (
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <h3 className="text-lg font-medium text-slate-800 mb-1">No estimates yet</h3>
          <p className="text-sm text-slate-500 mb-4">Create an estimate to get started.</p>
          <Link
            href={`/app/jobs/${jobId}/estimates/new`}
            className="inline-flex px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg"
          >
            + New Estimate
          </Link>
        </div>
      )}

      {!isError && list.length > 0 && (
        <div className="bg-white shadow-sm rounded-xl border border-slate-200 overflow-hidden">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  Title / #
                </th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  Status
                </th>
                <th className="text-right px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  Total
                </th>
                <th className="text-left px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  Updated
                </th>
                <th className="text-right px-6 py-3 text-xs font-semibold text-slate-600 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {list.map((est) => (
                <tr key={est.id} className="hover:bg-slate-50 transition-colors">
                  <td className="px-6 py-4">
                    <div className="font-medium text-slate-800">
                      {est.title || `Estimate ${est.id.slice(0, 8)}…`}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge
                      label={ESTIMATE_STATUS_LABELS[est.status as EstimateStatus]}
                      className={ESTIMATE_STATUS_COLORS[est.status as EstimateStatus]}
                    />
                  </td>
                  <td className="px-6 py-4 text-right text-sm text-slate-600">
                    {formatMoney(est.total ?? est.subtotal)}
                  </td>
                  <td className="px-6 py-4 text-sm text-slate-600">
                    {formatDate(est.updatedAt ?? est.createdAt)}
                  </td>
                  <td className="px-6 py-4 text-right">
                    <Link
                      href={`/app/estimates/${est.id}`}
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
      )}
    </div>
  );
}
