"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { getJob, updateJobStatus } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  JOB_STATUSES,
  JOB_STATUS_LABELS,
  JOB_STATUS_COLORS,
  JOB_TYPE_LABELS,
} from "@/lib/jobsConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatAddress, formatDate, formatDateTime } from "@/lib/format";
import { listJobAttachments, uploadJobAttachment, downloadAttachment } from "@/lib/attachmentsApi";
import {
  listJobCommunicationLogs,
  addJobCommunicationLog,
} from "@/lib/communicationLogsApi";
import { AttachmentSection } from "@/components/AttachmentSection";
import { CommunicationLogSection } from "@/components/CommunicationLogSection";
import type { JobStatus, CreateCommunicationLogRequest } from "@/lib/types";

export default function JobDetailPage() {
  const params = useParams();
  const jobId = params.jobId as string;
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<JobStatus | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [attachmentError, setAttachmentError] = useState<string | null>(null);
  const [commLogError, setCommLogError] = useState<string | null>(null);

  const { data: job, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.job(auth.selectedTenantId, jobId),
    queryFn: () => getJob(api, jobId),
    enabled: !!auth.selectedTenantId && !!jobId,
  });

  useEffect(() => {
    if (job) setSelectedStatus(job.status);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sync only when server status changes
  }, [job?.status]);

  const statusMutation = useMutation({
    mutationFn: (newStatus: JobStatus) => updateJobStatus(api, jobId, newStatus),
    onSuccess: () => {
      setUpdateError(null);
      queryClient.invalidateQueries({ queryKey: queryKeys.job(auth.selectedTenantId, jobId) });
      queryClient.invalidateQueries({ queryKey: ["jobs", auth.selectedTenantId] });
    },
    onError: (err: unknown) => {
      console.error("Failed to update job status:", err);
      setUpdateError(getApiErrorMessage(err, "Failed to update status. Please try again."));
      if (job) setSelectedStatus(job.status);
    },
  });

  const handleStatusChange = (newStatus: JobStatus) => {
    if (newStatus !== job?.status) {
      setSelectedStatus(newStatus);
      statusMutation.mutate(newStatus);
    }
  };

  const attachmentsQuery = useQuery({
    queryKey: queryKeys.jobAttachments(auth.selectedTenantId, jobId),
    queryFn: () => listJobAttachments(api, jobId),
    enabled: !!auth.selectedTenantId && !!jobId,
  });

  const uploadAttachmentMutation = useMutation({
    mutationFn: (file: File) => uploadJobAttachment(api, jobId, file),
    onSuccess: () => {
      setAttachmentError(null);
      queryClient.invalidateQueries({
        queryKey: queryKeys.jobAttachments(auth.selectedTenantId, jobId),
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
    queryKey: queryKeys.jobCommLogs(auth.selectedTenantId, jobId),
    queryFn: () => listJobCommunicationLogs(api, jobId),
    enabled: !!auth.selectedTenantId && !!jobId,
  });

  const addCommLogMutation = useMutation({
    mutationFn: (payload: CreateCommunicationLogRequest) =>
      addJobCommunicationLog(api, jobId, payload),
    onSuccess: () => {
      setCommLogError(null);
      queryClient.invalidateQueries({
        queryKey: queryKeys.jobCommLogs(auth.selectedTenantId, jobId),
      });
    },
    onError: (err: unknown) => {
      console.error("Failed to add communication log:", err);
      setCommLogError(getApiErrorMessage(err, "Failed to add log. Please try again."));
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading job details…</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-4xl mx-auto">
        <Link href="/app/jobs" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Jobs
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load job</h3>
          <p className="text-sm text-red-600 mt-1">
            {getApiErrorMessage(error, "The job could not be found or an error occurred.")}
          </p>
        </div>
      </div>
    );
  }

  if (!job) return null;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Link href="/app/jobs" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Jobs
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">
              {JOB_TYPE_LABELS[job.type]} — {formatAddress(job.propertyAddress)}
            </h1>
            <p className="text-sm text-slate-500 mt-1">Job Details</p>
          </div>
          <span className={`inline-flex px-3 py-1.5 text-sm font-medium rounded-full border ${JOB_STATUS_COLORS[job.status]}`}>
            {JOB_STATUS_LABELS[job.status]}
          </span>
        </div>
      </div>

      {job.leadId && (
        <div className="mb-6 bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-sm font-semibold text-slate-500 uppercase tracking-wider mb-2">Created from Lead</h2>
          <Link
            href={`/app/leads/${job.leadId}`}
            className="text-sm font-medium text-sky-600 hover:text-sky-700"
          >
            View Lead
          </Link>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Overview</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Type</dt>
                <dd className="mt-1 text-sm text-slate-800">{JOB_TYPE_LABELS[job.type]}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Status</dt>
                <dd className="mt-1">
                  <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full border ${JOB_STATUS_COLORS[job.status]}`}>
                    {JOB_STATUS_LABELS[job.status]}
                  </span>
                </dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Property address</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatAddress(job.propertyAddress)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Scheduled start</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDate(job.scheduledStartDate)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Scheduled end</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDate(job.scheduledEndDate)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Crew</dt>
                <dd className="mt-1 text-sm text-slate-800">{job.crewName ?? "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Created</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDateTime(job.createdAt)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Updated</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDateTime(job.updatedAt)}</dd>
              </div>
            </dl>
            {job.internalNotes && (
              <div className="mt-4 pt-4 border-t border-slate-100">
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Internal notes</dt>
                <dd className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">{job.internalNotes}</dd>
              </div>
            )}
          </div>

          {/* Attachments */}
          <AttachmentSection
            title="Attachments"
            attachments={attachmentsQuery.data ?? []}
            onUpload={(file) => uploadAttachmentMutation.mutate(file)}
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

        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Update status</h2>
            {updateError && (
              <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {updateError}
              </div>
            )}
            <div className="space-y-2">
              {JOB_STATUSES.map((status) => (
                <button
                  key={status}
                  onClick={() => handleStatusChange(status)}
                  disabled={statusMutation.isPending}
                  className={`w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                    selectedStatus === status
                      ? JOB_STATUS_COLORS[status] + " border"
                      : "bg-slate-50 text-slate-700 hover:bg-slate-100 border border-transparent"
                  } ${statusMutation.isPending ? "opacity-60 cursor-not-allowed" : ""}`}
                >
                  <div className="flex items-center justify-between">
                    <span>{JOB_STATUS_LABELS[status]}</span>
                    {selectedStatus === status && (
                      <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                      </svg>
                    )}
                  </div>
                </button>
              ))}
            </div>
            {statusMutation.isPending && (
              <div className="mt-3 flex items-center gap-2 text-xs text-slate-500">
                <svg className="animate-spin h-3 w-3" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Updating status…
              </div>
            )}
          </div>

          {/* Actions */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">
              Actions
            </h2>
            <div className="space-y-2">
              <Link
                href={`/app/jobs/${jobId}/estimates/new`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
              >
                Create Estimate
              </Link>
              <Link
                href={`/app/jobs/${jobId}/estimates`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                View Estimates
              </Link>
              <Link
                href={`/app/jobs/${jobId}/edit`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                Edit Job
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
