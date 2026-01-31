"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getLead, updateLeadStatus, convertLeadToJob } from "@/lib/leadsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  LEAD_STATUSES,
  STATUS_LABELS,
  STATUS_COLORS,
  SOURCE_LABELS,
} from "@/lib/leadsConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatAddress, formatDateTime, formatPhone } from "@/lib/format";
import { listLeadAttachments, uploadLeadAttachment, downloadAttachment } from "@/lib/attachmentsApi";
import {
  listLeadCommunicationLogs,
  addLeadCommunicationLog,
} from "@/lib/communicationLogsApi";
import { AttachmentSection } from "@/components/AttachmentSection";
import { CommunicationLogSection } from "@/components/CommunicationLogSection";
import { ActivitySection } from "@/components/ActivitySection";
import { TasksSection } from "@/components/TasksSection";
import { StatusBadge } from "@/components/StatusBadge";
import { JOB_TYPES, JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import {
  LeadStatus,
  CreateCommunicationLogRequest,
  ConvertLeadToJobRequest,
  JobType,
  AttachmentTag,
} from "@/lib/types";

export default function LeadDetailPage() {
  const params = useParams();
  const router = useRouter();
  const leadId = params.leadId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<LeadStatus | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [attachmentError, setAttachmentError] = useState<string | null>(null);
  const [commLogError, setCommLogError] = useState<string | null>(null);
  const [showConvertModal, setShowConvertModal] = useState(false);
  const [convertJobType, setConvertJobType] = useState<JobType | "">("");
  const [convertNotes, setConvertNotes] = useState("");

  const queryKey = queryKeys.lead(auth.selectedTenantId, leadId);

  const { data: lead, isLoading, isError, error } = useQuery({
    queryKey,
    queryFn: () => getLead(api, leadId),
    enabled: ready && !!leadId,
  });

  const convertMutation = useMutation({
    mutationFn: (payload: ConvertLeadToJobRequest) => convertLeadToJob(api, leadId, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: ["leads", auth.selectedTenantId] });
      queryClient.invalidateQueries({ queryKey: ["jobs", auth.selectedTenantId] });
      router.push(`/app/jobs/${data.id}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to convert lead:", err);
    },
  });

  const handleConvertSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!convertJobType) return;
    convertMutation.mutate({
      type: convertJobType,
      internalNotes: convertNotes.trim() || null,
    });
  };

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
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: ["leads", auth.selectedTenantId] });
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

  const attachmentsQuery = useQuery({
    queryKey: queryKeys.leadAttachments(auth.selectedTenantId, leadId),
    queryFn: () => listLeadAttachments(api, leadId),
    enabled: ready && !!leadId,
  });

  const uploadAttachmentMutation = useMutation({
    mutationFn: ({
      file,
      tag,
      description,
    }: {
      file: File;
      tag?: AttachmentTag;
      description?: string;
    }) => uploadLeadAttachment(api, leadId, file, { tag, description }),
    onSuccess: () => {
      setAttachmentError(null);
      queryClient.invalidateQueries({
        queryKey: queryKeys.leadAttachments(auth.selectedTenantId, leadId),
      });
    },
    onError: (err: unknown) => {
      console.error("Failed to upload attachment:", err);
      setAttachmentError(getApiErrorMessage(err, "Failed to upload. Please try again."));
    },
  });

  const handleDownloadAttachment = async (attachmentId: string, fileName: string) => {
    try {
      const blob = await downloadAttachment(api, attachmentId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName || attachmentId;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Failed to download attachment:", err);
      setAttachmentError(getApiErrorMessage(err, "Failed to download."));
    }
  };

  const commLogsQuery = useQuery({
    queryKey: queryKeys.leadCommLogs(auth.selectedTenantId, leadId),
    queryFn: () => listLeadCommunicationLogs(api, leadId),
    enabled: ready && !!leadId,
  });

  const addCommLogMutation = useMutation({
    mutationFn: (payload: CreateCommunicationLogRequest) =>
      addLeadCommunicationLog(api, leadId, payload),
    onSuccess: () => {
      setCommLogError(null);
      queryClient.invalidateQueries({
        queryKey: queryKeys.leadCommLogs(auth.selectedTenantId, leadId),
      });
    },
    onError: (err: unknown) => {
      console.error("Failed to add communication log:", err);
      setCommLogError(getApiErrorMessage(err, "Failed to add log. Please try again."));
    },
  });

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

      {lead.convertedJobId && (
        <div className="mb-6 bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-3">Converted to Job</h2>
          <p className="text-sm text-slate-600 mb-4">
            This lead has been converted to a job. View the job or create an estimate.
          </p>
          <div className="flex flex-wrap gap-3">
            <Link
              href={`/app/jobs/${lead.convertedJobId}`}
              className="inline-flex items-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
            >
              View Job
            </Link>
            <Link
              href={`/app/jobs/${lead.convertedJobId}/estimates/new`}
              className="inline-flex items-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
            >
              Create Estimate
            </Link>
          </div>
        </div>
      )}

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
                  {formatPhone(lead.customerPhone)}
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
            <p className="text-sm text-slate-800">{formatAddress(lead.propertyAddress)}</p>
          </div>

          {/* Activity */}
          <ActivitySection entityType="LEAD" entityId={leadId} />

          {/* Tasks */}
          <TasksSection entityType="lead" entityId={leadId} />

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

          {/* Attachments */}
          <AttachmentSection
            title="Attachments"
            attachments={attachmentsQuery.data ?? []}
            onUpload={(file, options) =>
              uploadAttachmentMutation.mutate({ file, tag: options?.tag, description: options?.description })
            }
            onDownload={handleDownloadAttachment}
            isLoading={attachmentsQuery.isLoading}
            isUploading={uploadAttachmentMutation.isPending}
            errorMessage={attachmentError}
          />

          {/* Communication Logs */}
          <CommunicationLogSection
            title="Communication Logs"
            logs={commLogsQuery.data ?? []}
            onAdd={(payload) => addCommLogMutation.mutate(payload)}
            isLoading={commLogsQuery.isLoading}
            isSubmitting={addCommLogMutation.isPending}
            errorMessage={commLogError}
          />
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
                  {formatDateTime(lead.createdAt)}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">
                  Last Updated
                </dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {formatDateTime(lead.updatedAt)}
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
                href={`/app/tasks/new?leadId=${leadId}`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
              >
                Create Task
              </Link>
              {lead.convertedJobId ? (
                <>
                  <Link
                    href={`/app/jobs/${lead.convertedJobId}/estimates/new`}
                    className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
                  >
                    Create Estimate
                  </Link>
                  <Link
                    href={`/app/jobs/${lead.convertedJobId}`}
                    className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
                  >
                    View Job
                  </Link>
                </>
              ) : lead.status !== "LOST" ? (
                <button
                  type="button"
                  onClick={() => setShowConvertModal(true)}
                  className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
                >
                  Convert to Job
                </button>
              ) : null}
              <Link
                href={`/app/leads/${leadId}/edit`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                Edit Lead
              </Link>
            </div>
          </div>

          {/* Convert to Job Modal */}
          {showConvertModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => !convertMutation.isPending && setShowConvertModal(false)}>
              <div className="bg-white rounded-xl shadow-xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
                <h3 className="text-lg font-semibold text-slate-800 mb-4">Convert to Job</h3>
                <form onSubmit={handleConvertSubmit} className="space-y-4">
                  {convertMutation.isError && (
                    <p className="text-sm text-red-600">{getApiErrorMessage(convertMutation.error, "Failed to convert")}</p>
                  )}
                  <div>
                    <label htmlFor="convertJobType" className="block text-sm font-medium text-slate-700 mb-1">Job Type *</label>
                    <select
                      id="convertJobType"
                      value={convertJobType}
                      onChange={(e) => setConvertJobType(e.target.value as JobType)}
                      required
                      className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    >
                      <option value="">Select type</option>
                      {JOB_TYPES.map((t) => (
                        <option key={t} value={t}>{JOB_TYPE_LABELS[t]}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label htmlFor="convertNotes" className="block text-sm font-medium text-slate-700 mb-1">Notes (optional)</label>
                    <textarea
                      id="convertNotes"
                      value={convertNotes}
                      onChange={(e) => setConvertNotes(e.target.value)}
                      rows={2}
                      placeholder="Internal notes for the job"
                      className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button
                      type="submit"
                      disabled={!convertJobType || convertMutation.isPending}
                      className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60"
                    >
                      {convertMutation.isPending ? "Converting…" : "Convert"}
                    </button>
                    <button
                      type="button"
                      onClick={() => setShowConvertModal(false)}
                      disabled={convertMutation.isPending}
                      className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-60"
                    >
                      Cancel
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
