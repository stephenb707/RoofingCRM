"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { getLead, updateLeadStatus } from "@/lib/leadsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  LEAD_STATUSES,
  STATUS_LABELS,
  STATUS_COLORS,
  SOURCE_LABELS,
} from "@/lib/leadsConstants";
import { queryKeys } from "@/lib/queryKeys";
import { LeadStatus, LeadDto } from "@/lib/types";

function formatAddress(lead: LeadDto): string {
  const addr = lead.propertyAddress;
  if (!addr) return "—";
  const parts = [addr.line1, addr.line2, addr.city, addr.state, addr.zip].filter(
    Boolean
  );
  return parts.length > 0 ? parts.join(", ") : "—";
}

function formatDate(dateString: string): string {
  try {
    return new Date(dateString).toLocaleDateString("en-US", {
      month: "long",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  } catch {
    return "—";
  }
}

export default function LeadDetailPage() {
  const params = useParams();
  const leadId = params.leadId as string;
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<LeadStatus | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);

  const queryKey = queryKeys.lead(auth.selectedTenantId, leadId);

  const { data: lead, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: () => getLead(api, leadId),
    enabled: !!auth.selectedTenantId && !!leadId,
  });

  useEffect(() => {
    if (lead) setSelectedStatus(lead.status);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sync only when server status changes
  }, [lead?.status]);

  const statusMutation = useMutation({
    mutationFn: async (newStatus: LeadStatus) => {
      return updateLeadStatus(api, leadId, newStatus);
    },
    onSuccess: () => {
      setUpdateError(null);
      // Invalidate both the detail and list queries
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({
        queryKey: ["leads", auth.selectedTenantId],
      });
    },
    onError: (err: unknown) => {
      console.error("Failed to update lead status:", err);
      setUpdateError(getApiErrorMessage(err, "Failed to update status. Please try again."));
      if (lead) setSelectedStatus(lead.status);
    },
  });

  const handleStatusChange = (newStatus: LeadStatus) => {
    if (newStatus !== lead?.status) {
      setSelectedStatus(newStatus);
      statusMutation.mutate(newStatus);
    }
  };

  // Loading State
  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4"></div>
          <p className="text-sm text-slate-500">Loading lead details...</p>
        </div>
      </div>
    );
  }

  // Error State
  if (isError) {
    return (
      <div className="max-w-4xl mx-auto">
        <Link
          href="/app/leads"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4"
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
          Back to Leads
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <div className="flex items-start gap-3">
            <svg
              className="w-5 h-5 text-red-500 mt-0.5"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
            <div>
              <h3 className="text-sm font-medium text-red-800">
                Failed to load lead
              </h3>
              <p className="text-sm text-red-600 mt-1">
                The lead could not be found or an error occurred.
              </p>
              <p className="text-xs text-red-500 mt-2 font-mono">
                {getApiErrorMessage(error, "Unknown error")}
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!lead) {
    return null;
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Back Link & Header */}
      <div className="mb-6">
        <Link
          href="/app/leads"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
          Back to Leads
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">
              {[lead.customerFirstName, lead.customerLastName].filter(Boolean).join(" ") || "—"}
            </h1>
            <p className="text-sm text-slate-500 mt-1">Lead Details</p>
          </div>
          <span
            className={`inline-flex px-3 py-1.5 text-sm font-medium rounded-full border ${STATUS_COLORS[lead.status]}`}
          >
            {STATUS_LABELS[lead.status]}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Customer Information */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <svg
                className="w-5 h-5 text-slate-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                />
              </svg>
              Customer Information
            </h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Name
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {[lead.customerFirstName, lead.customerLastName].filter(Boolean).join(" ") || "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Phone
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {lead.customerPhone || "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Email
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {lead.customerEmail || "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Preferred Contact
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {lead.preferredContactMethod || "—"}
                </dd>
              </div>
            </dl>
          </div>

          {/* Property Address */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
              <svg
                className="w-5 h-5 text-slate-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                />
              </svg>
              Property Address
            </h2>
            <p className="text-sm text-slate-800">{formatAddress(lead)}</p>
          </div>

          {/* Notes */}
          {lead.leadNotes && (
            <div className="bg-white rounded-xl border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4 flex items-center gap-2">
                <svg
                  className="w-5 h-5 text-slate-400"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                Notes
              </h2>
              <p className="text-sm text-slate-700 whitespace-pre-wrap">
                {lead.leadNotes}
              </p>
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status Update */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">
              Update Status
            </h2>

            {updateError && (
              <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {updateError}
              </div>
            )}

            <div className="space-y-2">
              {LEAD_STATUSES.map((status) => (
                <button
                  key={status}
                  onClick={() => handleStatusChange(status)}
                  disabled={statusMutation.isPending}
                  className={`w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    selectedStatus === status
                      ? STATUS_COLORS[status] + " border"
                      : "bg-slate-50 text-slate-700 hover:bg-slate-100 border border-transparent"
                  } ${statusMutation.isPending ? "opacity-60 cursor-not-allowed" : ""}`}
                >
                  <div className="flex items-center justify-between">
                    <span>{STATUS_LABELS[status]}</span>
                    {selectedStatus === status && (
                      <svg
                        className="w-4 h-4"
                        fill="currentColor"
                        viewBox="0 0 20 20"
                      >
                        <path
                          fillRule="evenodd"
                          d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                          clipRule="evenodd"
                        />
                      </svg>
                    )}
                  </div>
                </button>
              ))}
            </div>

            {statusMutation.isPending && (
              <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
                <svg
                  className="animate-spin h-3 w-3"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                Updating status...
              </div>
            )}
          </div>

          {/* Lead Details */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">
              Lead Details
            </h2>
            <dl className="space-y-3">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Source
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {lead.source ? SOURCE_LABELS[lead.source] || lead.source : "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Created
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {formatDate(lead.createdAt)}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Last Updated
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {formatDate(lead.updatedAt)}
                </dd>
              </div>
            </dl>
          </div>

          {/* Actions */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">
              Actions
            </h2>
            <div className="space-y-2">
              <Link
                href={`/app/leads/${leadId}/edit`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                Edit Lead
              </Link>
              <Link
                href={`/app/jobs/new?leadId=${leadId}`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
              >
                Create Job
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
