"use client";

import { useState, useEffect, useMemo, useRef, useCallback } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getJob, updateJobStatus } from "@/lib/jobsApi";
import { listEstimatesForJob } from "@/lib/estimatesApi";
import { getCustomer } from "@/lib/customersApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import type { PipelineStatusDefinitionDto } from "@/lib/pipelineStatusesApi";
import { jobStatusBadgeClass } from "@/lib/pipelineStatusVisuals";
import { queryKeys } from "@/lib/queryKeys";
import { formatAddress, formatDate, formatDateTime, formatMoney, formatPhone } from "@/lib/format";
import { listJobAttachments, uploadJobAttachment, downloadAttachment, deleteAttachment } from "@/lib/attachmentsApi";
import {
  listJobCommunicationLogs,
  addJobCommunicationLog,
} from "@/lib/communicationLogsApi";
import { AttachmentSection } from "@/components/AttachmentSection";
import { CommunicationLogSection } from "@/components/CommunicationLogSection";
import { ActivitySection } from "@/components/ActivitySection";
import { TasksSection } from "@/components/TasksSection";
import { InvoicesSection } from "@/components/InvoicesSection";
import { AccountingSection } from "@/components/AccountingSection";
import { DetailSectionNav, type DetailSectionNavItem } from "@/components/JobDetailSectionNav";
import { StatusBadge } from "@/components/StatusBadge";
import { NextBestActions } from "@/components/NextBestActions";
import { EstimateSharePanel } from "@/components/EstimateSharePanel";
import { ESTIMATE_STATUS_COLORS, ESTIMATE_STATUS_LABELS } from "@/lib/estimatesConstants";
import type { EstimateStatus, CreateCommunicationLogRequest, AttachmentTag } from "@/lib/types";
import { PREFERRED_CONTACT_LABELS } from "@/lib/preferredContactConstants";

const SECTION_SCROLL_MARGIN_CLASS = "scroll-mt-24";

export default function JobDetailPage() {
  const params = useParams();
  const jobId = params.jobId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [selectedStatusId, setSelectedStatusId] = useState<string | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [attachmentError, setAttachmentError] = useState<string | null>(null);
  const [commLogError, setCommLogError] = useState<string | null>(null);
  const [latestEstimateError, setLatestEstimateError] = useState<string | null>(null);
  const [createFromEstimateId, setCreateFromEstimateId] = useState<string | null>(null);
  const [attachmentPreviewUrls, setAttachmentPreviewUrls] = useState<Record<string, string>>({});
  const [loadingAttachmentPreviewIds, setLoadingAttachmentPreviewIds] = useState<string[]>([]);
  const attachmentPreviewUrlRef = useRef<Record<string, string>>({});

  const { data: job, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.job(auth.selectedTenantId, jobId),
    queryFn: () => getJob(api, jobId),
    enabled: ready && !!jobId,
  });

  const pipelineDefsKey = queryKeys.pipelineStatuses(auth.selectedTenantId, "JOB");
  const { data: jobDefs = [] } = useQuery({
    queryKey: pipelineDefsKey,
    queryFn: () => listPipelineStatuses(api, "JOB"),
    enabled: ready,
  });

  const sortedJobDefs = useMemo(
    () => [...jobDefs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder),
    [jobDefs]
  );

  const { data: customer } = useQuery({
    queryKey: queryKeys.customer(auth.selectedTenantId, job?.customerId ?? ""),
    queryFn: () => getCustomer(api, job!.customerId!),
    enabled: ready && !!job?.customerId,
  });

  const estimatesQuery = useQuery({
    queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, jobId),
    queryFn: () => listEstimatesForJob(api, jobId),
    enabled: ready && !!jobId,
  });

  const selectedTenant = auth.tenants.find((t) => t.tenantId === auth.selectedTenantId);
  const canShare =
    selectedTenant?.role === "OWNER" ||
    selectedTenant?.role === "ADMIN" ||
    selectedTenant?.role === "SALES";

  const sectionNavItems = useMemo<DetailSectionNavItem[]>(
    () => [
      ...(job?.customerId
        ? [{ id: "customer-information", label: "Customer", icon: "customer" as const }]
        : []),
      { id: "overview", label: "Overview", icon: "overview" },
      { id: "activity", label: "Activity", icon: "activity" },
      { id: "tasks", label: "Tasks", icon: "tasks" },
      { id: "invoices", label: "Invoices", icon: "invoices" },
      { id: "accounting", label: "Accounting", icon: "accounting" },
      { id: "attachments", label: "Attachments", icon: "attachments" },
      { id: "communication", label: "Communication", icon: "communication" },
    ],
    [job?.customerId]
  );
  const sectionIds = useMemo(() => sectionNavItems.map((item) => item.id), [sectionNavItems]);
  const [activeSectionId, setActiveSectionId] = useState<string | null>(sectionNavItems[0]?.id ?? null);

  useEffect(() => {
    if (job) setSelectedStatusId(job.statusDefinitionId);
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sync only when server status changes
  }, [job?.statusDefinitionId]);

  useEffect(() => {
    if (sectionNavItems.length === 0) {
      setActiveSectionId(null);
      return;
    }

    if (!activeSectionId || !sectionIds.includes(activeSectionId)) {
      setActiveSectionId(sectionNavItems[0].id);
    }
  }, [activeSectionId, sectionIds, sectionNavItems]);

  const scrollToSection = useCallback((id: string, behavior: ScrollBehavior = "smooth") => {
    if (typeof window === "undefined" || !sectionIds.includes(id)) {
      return;
    }

    const element = document.getElementById(id);
    if (!element) {
      return;
    }

    element.scrollIntoView({ behavior, block: "start" });
  }, [sectionIds]);

  const handleSectionNavigate = useCallback((id: string) => {
    setActiveSectionId(id);
    if (typeof window !== "undefined") {
      window.history.replaceState(null, "", `#${id}`);
    }
    scrollToSection(id);
  }, [scrollToSection]);

  useEffect(() => {
    const scrollToHashSection = () => {
      if (typeof window === "undefined") return;
      const id = window.location.hash.replace(/^#/, "");
      if (!id || !sectionIds.includes(id)) {
        return;
      }

      setActiveSectionId(id);
      requestAnimationFrame(() => {
        scrollToSection(id, "smooth");
      });
    };

    scrollToHashSection();
    window.addEventListener("hashchange", scrollToHashSection);
    return () => window.removeEventListener("hashchange", scrollToHashSection);
  }, [scrollToSection, sectionIds]);

  useEffect(() => {
    if (typeof window === "undefined" || sectionIds.length === 0) {
      return;
    }

    const elements = sectionIds
      .map((id) => document.getElementById(id))
      .filter((element): element is HTMLElement => Boolean(element));

    if (elements.length === 0) {
      return;
    }

    if (typeof IntersectionObserver === "undefined") {
      setActiveSectionId((current) => current ?? sectionIds[0]);
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const visibleEntries = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

        if (visibleEntries.length > 0) {
          setActiveSectionId(visibleEntries[0].target.id);
        }
      },
      {
        rootMargin: "-20% 0px -65% 0px",
        threshold: [0.1, 0.25, 0.5],
      }
    );

    elements.forEach((element) => observer.observe(element));
    return () => observer.disconnect();
  }, [sectionIds]);

  const statusMutation = useMutation({
    mutationFn: (statusDefinitionId: string) => updateJobStatus(api, jobId, statusDefinitionId),
    onSuccess: () => {
      setUpdateError(null);
      queryClient.invalidateQueries({ queryKey: queryKeys.job(auth.selectedTenantId, jobId) });
      queryClient.invalidateQueries({ queryKey: ["jobs", auth.selectedTenantId] });
    },
    onError: (err: unknown) => {
      console.error("Failed to update job status:", err);
      setUpdateError(getApiErrorMessage(err, "Failed to update status. Please try again."));
      if (job) setSelectedStatusId(job.statusDefinitionId);
    },
  });

  const handleStatusChange = (statusDefinitionId: string) => {
    if (statusDefinitionId !== job?.statusDefinitionId) {
      setSelectedStatusId(statusDefinitionId);
      statusMutation.mutate(statusDefinitionId);
    }
  };

  const attachmentsQuery = useQuery({
    queryKey: queryKeys.jobAttachments(auth.selectedTenantId, jobId),
    queryFn: () => listJobAttachments(api, jobId),
    enabled: ready && !!jobId,
  });
  const nonReceiptAttachments = useMemo(
    () => (attachmentsQuery.data ?? []).filter((attachment) => attachment.tag !== "RECEIPT"),
    [attachmentsQuery.data]
  );

  useEffect(() => {
    return () => {
      if (typeof URL.revokeObjectURL !== "function") {
        return;
      }
      for (const url of Object.values(attachmentPreviewUrlRef.current)) {
        URL.revokeObjectURL(url);
      }
    };
  }, []);

  useEffect(() => {
    if (typeof URL.createObjectURL !== "function") {
      return;
    }
    const imageAttachments = nonReceiptAttachments.filter((attachment) =>
      (attachment.contentType ?? "").toLowerCase().startsWith("image/")
    );
    const missing = imageAttachments.filter(
      (attachment) => !Object.prototype.hasOwnProperty.call(attachmentPreviewUrlRef.current, attachment.id)
    );
    if (missing.length === 0) {
      return;
    }

    let cancelled = false;
    const ids = missing.map((attachment) => attachment.id);
    for (const attachment of missing) {
      attachmentPreviewUrlRef.current[attachment.id] = "";
    }
    setLoadingAttachmentPreviewIds((prev) => Array.from(new Set([...prev, ...ids])));

    void Promise.all(
      missing.map(async (attachment) => {
        try {
          const blob = await downloadAttachment(api, attachment.id);
          if (cancelled || !(blob instanceof Blob)) {
            return;
          }
          const url = URL.createObjectURL(blob);
          attachmentPreviewUrlRef.current[attachment.id] = url;
          setAttachmentPreviewUrls((prev) => ({ ...prev, [attachment.id]: url }));
        } catch {
          if (!cancelled) {
            attachmentPreviewUrlRef.current[attachment.id] = "";
            setAttachmentPreviewUrls((prev) => ({ ...prev, [attachment.id]: "" }));
          }
        } finally {
          if (!cancelled) {
            setLoadingAttachmentPreviewIds((prev) => prev.filter((id) => id !== attachment.id));
          }
        }
      })
    );

    return () => {
      cancelled = true;
    };
  }, [api, nonReceiptAttachments]);

  const uploadAttachmentMutation = useMutation({
    mutationFn: ({
      file,
      tag,
      description,
    }: {
      file: File;
      tag?: AttachmentTag;
      description?: string;
    }) => uploadJobAttachment(api, jobId, file, { tag, description }),
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

  const deleteAttachmentMutation = useMutation({
    mutationFn: (attachmentId: string) => deleteAttachment(api, attachmentId),
    onSuccess: () => {
      setAttachmentError(null);
      queryClient.invalidateQueries({
        queryKey: queryKeys.jobAttachments(auth.selectedTenantId, jobId),
      });
    },
    onError: (err: unknown) => {
      console.error("Failed to delete attachment:", err);
      setAttachmentError(getApiErrorMessage(err, "Failed to delete attachment."));
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

  const handleDeleteAttachment = async (attachmentId: string, fileName: string) => {
    if (!window.confirm(`Delete "${fileName}" from this job? This cannot be undone.`)) {
      return;
    }
    setAttachmentError(null);
    deleteAttachmentMutation.mutate(attachmentId);
  };

  const commLogsQuery = useQuery({
    queryKey: queryKeys.jobCommLogs(auth.selectedTenantId, jobId),
    queryFn: () => listJobCommunicationLogs(api, jobId),
    enabled: ready && !!jobId,
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

  const latestEstimate = [...(estimatesQuery.data ?? [])].sort((a, b) => {
    const aTs = new Date(a.updatedAt ?? a.createdAt ?? 0).getTime();
    const bTs = new Date(b.updatedAt ?? b.createdAt ?? 0).getTime();
    return bTs - aTs;
  })[0];

  return (
    <>
    <div className="mx-auto max-w-7xl lg:pl-60">
      <div
        className="hidden lg:block lg:fixed lg:top-[8.5rem] lg:left-6 lg:z-10 lg:w-52"
        data-testid="job-section-nav-rail-container"
      >
        <DetailSectionNav
          items={sectionNavItems}
          activeSectionId={activeSectionId}
          onNavigate={handleSectionNavigate}
          className="max-h-[calc(100vh-10rem)] overflow-y-auto"
        />
      </div>

      <div className="mx-auto max-w-6xl">
        <div className="mb-6">
          <Link href="/app/jobs" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Jobs
          </Link>
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-slate-800">
                {JOB_TYPE_LABELS[job.type]} — {formatAddress(job.propertyAddress)}
              </h1>
              <p className="text-sm text-slate-500 mt-1">Job Details</p>
            </div>
            <span className={`inline-flex px-3 py-1.5 text-sm font-medium rounded-full border ${jobStatusBadgeClass(job.statusKey)}`}>
              {job.statusLabel}
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

        {job.customerId && (
          <section id="customer-information" className={`mb-6 ${SECTION_SCROLL_MARGIN_CLASS}`}>
            <div className="bg-white rounded-xl border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Customer Information</h2>
              {customer ? (
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Name</dt>
                    <dd className="mt-1 text-sm text-slate-800">
                      {customer.firstName} {customer.lastName}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Phone</dt>
                    <dd className="mt-1 text-sm text-slate-800">{formatPhone(customer.primaryPhone)}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Email</dt>
                    <dd className="mt-1 text-sm text-slate-800">{customer.email || "—"}</dd>
                  </div>
                  <div>
                    <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Preferred contact</dt>
                    <dd className="mt-1 text-sm text-slate-800">
                      {customer.preferredContactMethod
                        ? PREFERRED_CONTACT_LABELS[customer.preferredContactMethod]
                        : "—"}
                    </dd>
                  </div>
                  {customer.billingAddress && (
                    <div className="sm:col-span-2">
                      <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Billing Address</dt>
                      <dd className="mt-1 text-sm text-slate-800">{formatAddress(customer.billingAddress)}</dd>
                    </div>
                  )}
                </dl>
              ) : (
                <p className="text-sm text-slate-500">Loading customer…</p>
              )}
              <Link
                href={`/app/customers/${job.customerId}`}
                className="mt-4 inline-block text-sm font-medium text-sky-600 hover:text-sky-700"
              >
                View Customer
              </Link>
            </div>
          </section>
        )}

        <div className="mb-6 lg:hidden">
          <DetailSectionNav
            items={sectionNavItems}
            activeSectionId={activeSectionId}
            onNavigate={handleSectionNavigate}
            variant="inline"
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <section id="overview" className={SECTION_SCROLL_MARGIN_CLASS}>
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
                    <span className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full border ${jobStatusBadgeClass(job.statusKey)}`}>
                      {job.statusLabel}
                    </span>
                  </dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Property address</dt>
                  <dd className="mt-1 text-sm text-slate-800">{formatAddress(job.propertyAddress)}</dd>
                </div>
                <div>
                  <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Scheduled start</dt>
                  <dd className="mt-1 text-sm text-slate-800">
                    {job.scheduledStartDate ? (
                      formatDate(job.scheduledStartDate)
                    ) : (
                      <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded bg-slate-100 text-slate-600 border border-slate-200">
                        Unscheduled
                      </span>
                    )}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Scheduled end</dt>
                  <dd className="mt-1 text-sm text-slate-800">
                    {job.scheduledEndDate ? (
                      formatDate(job.scheduledEndDate)
                    ) : (
                      <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded bg-slate-100 text-slate-600 border border-slate-200">
                        Unscheduled
                      </span>
                    )}
                  </dd>
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
            </section>

            {/* Activity */}
            <section id="activity" className={SECTION_SCROLL_MARGIN_CLASS}>
              <ActivitySection entityType="JOB" entityId={jobId} />
            </section>

            {/* Tasks */}
            <section id="tasks" className={SECTION_SCROLL_MARGIN_CLASS}>
              <TasksSection entityType="job" entityId={jobId} />
            </section>

            {/* Invoices */}
            <section id="invoices" className={SECTION_SCROLL_MARGIN_CLASS}>
              <InvoicesSection
                jobId={jobId}
                createFromEstimateId={createFromEstimateId}
                onCreateFromEstimateHandled={() => setCreateFromEstimateId(null)}
              />
            </section>

            {/* Accounting */}
            <section id="accounting" className={SECTION_SCROLL_MARGIN_CLASS}>
              <AccountingSection jobId={jobId} />
            </section>

            {/* Attachments — #attachments is used by Next Best Actions “Upload photos” */}
            <section id="attachments" className={SECTION_SCROLL_MARGIN_CLASS}>
              <AttachmentSection
                title="Attachments"
                attachments={nonReceiptAttachments}
                previewUrls={attachmentPreviewUrls}
                loadingPreviewIds={loadingAttachmentPreviewIds}
                scrollableList
                onUpload={(file, options) =>
                  uploadAttachmentMutation.mutate({ file, tag: options?.tag, description: options?.description })
                }
                onDownload={handleDownloadAttachment}
                onDelete={handleDeleteAttachment}
                isLoading={attachmentsQuery.isLoading}
                isUploading={uploadAttachmentMutation.isPending}
                deletingAttachmentId={deleteAttachmentMutation.variables ?? null}
                errorMessage={attachmentError}
              />
            </section>

            {/* Communication Logs */}
            <section id="communication" className={SECTION_SCROLL_MARGIN_CLASS}>
              <CommunicationLogSection
                title="Communication Logs"
                logs={commLogsQuery.data ?? []}
                onAdd={(payload) => addCommLogMutation.mutate(payload)}
                isLoading={commLogsQuery.isLoading}
                isSubmitting={addCommLogMutation.isPending}
                errorMessage={commLogError}
              />
            </section>
          </div>

          <div className="space-y-6">
            <NextBestActions entityType="job" status={job.statusKey} jobId={jobId} />

            <section id="latest-estimate" className={SECTION_SCROLL_MARGIN_CLASS}>
              <div className="bg-white rounded-xl border border-slate-200 p-6">
                <h2 className="text-lg font-semibold text-slate-800 mb-4">Latest Estimate</h2>
                {estimatesQuery.isLoading && (
                  <p className="text-sm text-slate-500">Loading estimate summary…</p>
                )}
                {!estimatesQuery.isLoading && !latestEstimate && (
                  <div className="space-y-3">
                    <p className="text-sm text-slate-500">No estimates yet for this job.</p>
                    <Link
                      href={`/app/jobs/${jobId}/estimates/new`}
                      className="inline-flex w-full justify-center rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-sky-700"
                    >
                      Create estimate
                    </Link>
                  </div>
                )}
                {latestEstimate && (
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <StatusBadge
                        label={ESTIMATE_STATUS_LABELS[latestEstimate.status as EstimateStatus]}
                        className={ESTIMATE_STATUS_COLORS[latestEstimate.status as EstimateStatus]}
                      />
                      <span className="text-sm font-medium text-slate-800">
                        {formatMoney(latestEstimate.total ?? latestEstimate.subtotal)}
                      </span>
                    </div>
                    <p className="text-sm text-slate-600">
                      Updated {formatDate(latestEstimate.updatedAt ?? latestEstimate.createdAt)}
                    </p>
                    {latestEstimateError && (
                      <p className="text-sm text-red-600">{latestEstimateError}</p>
                    )}
                    {latestEstimate.status === "DRAFT" && (
                      <Link
                        href={`/app/estimates/${latestEstimate.id}`}
                        className="inline-flex w-full justify-center rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-sky-700"
                      >
                        Finish & Share
                      </Link>
                    )}
                    {latestEstimate.status === "SENT" &&
                      (canShare ? (
                        <EstimateSharePanel
                          estimateId={latestEstimate.id}
                          jobId={jobId}
                          customerId={job.customerId}
                          customerEmail={customer?.email ?? ""}
                          customerName={
                            customer ? `${customer.firstName} ${customer.lastName}`.trim() : ""
                          }
                          estimateTitle={latestEstimate.title ?? null}
                          canShare
                          variant="compact"
                        />
                      ) : (
                        <Link
                          href={`/app/estimates/${latestEstimate.id}`}
                          className="inline-flex w-full justify-center rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-sky-700"
                        >
                          View estimate
                        </Link>
                      ))}
                    {latestEstimate.status === "ACCEPTED" && (
                      <button
                        type="button"
                        onClick={() => {
                          setLatestEstimateError(null);
                          setCreateFromEstimateId(latestEstimate.id);
                        }}
                        className="inline-flex w-full justify-center rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-sky-700 disabled:opacity-60"
                      >
                        Create invoice
                      </button>
                    )}
                    {latestEstimate.status === "REJECTED" && (
                      <Link
                        href={`/app/estimates/${latestEstimate.id}`}
                        className="inline-flex w-full justify-center rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-sky-700"
                      >
                        View estimate
                      </Link>
                    )}
                  </div>
                )}
              </div>
            </section>

            <div className="bg-white rounded-xl border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Update status</h2>
              {updateError && (
                <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                  {updateError}
                </div>
              )}
              <div className="space-y-2">
                {sortedJobDefs.map((def: PipelineStatusDefinitionDto) => (
                  <button
                    key={def.id}
                    onClick={() => handleStatusChange(def.id)}
                    disabled={statusMutation.isPending}
                    className={`w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                      selectedStatusId === def.id
                        ? jobStatusBadgeClass(def.systemKey) + " border"
                        : "bg-slate-50 text-slate-700 hover:bg-slate-100 border border-transparent"
                    } ${statusMutation.isPending ? "opacity-60 cursor-not-allowed" : ""}`}
                  >
                    <div className="flex items-center justify-between">
                      <span>{def.label}</span>
                      {selectedStatusId === def.id && (
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
                  href={`/app/tasks/new?jobId=${jobId}`}
                  className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
                >
                  Create Task
                </Link>
                <Link
                  href={`/app/jobs/${jobId}/estimates/new`}
                  className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
                >
                  Create Estimate
                </Link>
                <Link
                  href={`/app/jobs/${jobId}/edit`}
                  className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
                >
                  Edit Job
                </Link>
                <Link
                  href={`/app/jobs/${jobId}/estimates`}
                  className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
                >
                  View Estimates
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    </>
  );
}
