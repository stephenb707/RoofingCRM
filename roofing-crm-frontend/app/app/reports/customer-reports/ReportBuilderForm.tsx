"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useAuthReady } from "@/lib/AuthContext";
import {
  createCustomerPhotoReport,
  downloadCustomerPhotoReportPdf,
  getCustomerPhotoReport,
  listReportAttachmentCandidates,
  sendCustomerPhotoReportEmail,
  updateCustomerPhotoReport,
} from "@/lib/customerPhotoReportsApi";
import { listCustomers } from "@/lib/customersApi";
import { listJobs } from "@/lib/jobsApi";
import { downloadAttachment, uploadJobAttachment } from "@/lib/attachmentsApi";
import { queryKeys } from "@/lib/queryKeys";
import { getApiErrorMessage } from "@/lib/apiError";
import { sanitizeAttachmentIds } from "@/lib/customerPhotoReportPayload";
import { formatAddress, formatDate } from "@/lib/format";
import { triggerBrowserDownload } from "@/lib/reportsApi";
import type {
  AttachmentDto,
  CustomerPhotoReportDto,
  SendCustomerPhotoReportEmailRequest,
  UpsertCustomerPhotoReportRequest,
} from "@/lib/types";
import { SendEmailModal } from "@/components/SendEmailModal";

type SectionDraft = {
  localKey: string;
  title: string;
  body: string;
  attachmentIds: string[];
};

function newSection(): SectionDraft {
  return {
    localKey: crypto.randomUUID(),
    title: "New section",
    body: "",
    attachmentIds: [],
  };
}

function mapDtoToDrafts(report: CustomerPhotoReportDto): SectionDraft[] {
  if (!report.sections?.length) {
    return [newSection()];
  }
  return report.sections.map((s) => ({
    localKey: s.id?.trim() ? s.id : crypto.randomUUID(),
    title: s.title,
    body: s.body ?? "",
    attachmentIds: sanitizeAttachmentIds(
      (s.photos ?? []).map((p) => p.attachmentId).filter((id): id is string => typeof id === "string")
    ),
  }));
}

export function ReportBuilderForm({
  reportId,
  initialCustomerId,
}: {
  reportId?: string;
  initialCustomerId?: string | null;
}) {
  const router = useRouter();
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [customerId, setCustomerId] = useState(initialCustomerId ?? "");
  const [jobId, setJobId] = useState("");
  const [title, setTitle] = useState("");
  const [reportType, setReportType] = useState("");
  const [summary, setSummary] = useState("");
  const [sections, setSections] = useState<SectionDraft[]>([newSection()]);
  const [recentUploadsById, setRecentUploadsById] = useState<Record<string, AttachmentDto>>({});
  const [uploadingSectionKey, setUploadingSectionKey] = useState<string | null>(null);
  const [openPhotoPickerKeys, setOpenPhotoPickerKeys] = useState<string[]>([]);
  const [photoPreviewUrls, setPhotoPreviewUrls] = useState<Record<string, string>>({});
  const [loadingPhotoPreviewIds, setLoadingPhotoPreviewIds] = useState<string[]>([]);
  const [pdfError, setPdfError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [emailSuccess, setEmailSuccess] = useState<string | null>(null);
  const previewUrlRef = useRef<Record<string, string>>({});

  const { data: report, isLoading: reportLoading } = useQuery({
    queryKey: queryKeys.customerPhotoReport(auth.selectedTenantId, reportId ?? ""),
    queryFn: () => getCustomerPhotoReport(api, reportId!),
    enabled: ready && !!auth.selectedTenantId && !!reportId,
  });

  useEffect(() => {
    if (!report || !reportId) return;
    setCustomerId(report.customerId);
    setJobId(report.jobId ?? "");
    setTitle(report.title);
    setReportType(report.reportType ?? "");
    setSummary(report.summary ?? "");
    setSections(mapDtoToDrafts(report));
    setOpenPhotoPickerKeys([]);
    setRecentUploadsById({});
  }, [report, reportId]);

  const { data: customersPage } = useQuery({
    queryKey: queryKeys.customersList(auth.selectedTenantId, "", 0),
    queryFn: () => listCustomers(api, { page: 0, size: 200 }),
    enabled: ready && !!auth.selectedTenantId,
  });

  const { data: jobsPage } = useQuery({
    queryKey: queryKeys.jobsList(auth.selectedTenantId, null, customerId || null, 0),
    queryFn: () => listJobs(api, { customerId, page: 0, size: 100 }),
    enabled: ready && !!auth.selectedTenantId && !!customerId,
  });

  const { data: candidates = [] } = useQuery({
    queryKey: queryKeys.customerPhotoReportCandidates(auth.selectedTenantId, customerId, jobId || null),
    queryFn: () => listReportAttachmentCandidates(api, customerId, jobId || null),
    enabled: ready && !!auth.selectedTenantId && !!customerId,
  });

  const imageCandidates = useMemo(
    () => candidates.filter((c) => (c.contentType ?? "").toLowerCase().startsWith("image/")),
    [candidates]
  );

  const imageOptionById = useMemo(() => {
    const map = new Map<string, AttachmentDto>();
    for (const attachment of imageCandidates) {
      map.set(attachment.id, attachment);
    }
    for (const attachment of Object.values(recentUploadsById)) {
      if (!map.has(attachment.id) && (attachment.contentType ?? "").toLowerCase().startsWith("image/")) {
        map.set(attachment.id, attachment);
      }
    }
    return map;
  }, [imageCandidates, recentUploadsById]);

  useEffect(() => {
    return () => {
      if (typeof URL.revokeObjectURL !== "function") {
        return;
      }
      for (const url of Object.values(previewUrlRef.current)) {
        URL.revokeObjectURL(url);
      }
    };
  }, []);

  useEffect(() => {
    if (openPhotoPickerKeys.length === 0 || imageCandidates.length === 0) {
      return;
    }
    const missing = imageCandidates.filter((candidate) => !previewUrlRef.current[candidate.id]);
    if (missing.length === 0) {
      return;
    }

    let cancelled = false;
    const ids = missing.map((candidate) => candidate.id);
    setLoadingPhotoPreviewIds((prev) => Array.from(new Set([...prev, ...ids])));

    void Promise.all(
      missing.map(async (candidate) => {
        try {
          const blob = await downloadAttachment(api, candidate.id);
          if (cancelled) {
            return;
          }
          const url = URL.createObjectURL(blob);
          previewUrlRef.current[candidate.id] = url;
          setPhotoPreviewUrls((prev) => ({ ...prev, [candidate.id]: url }));
        } catch {
          if (!cancelled) {
            setPhotoPreviewUrls((prev) => ({ ...prev, [candidate.id]: "" }));
          }
        } finally {
          if (!cancelled) {
            setLoadingPhotoPreviewIds((prev) => prev.filter((id) => id !== candidate.id));
          }
        }
      })
    );

    return () => {
      cancelled = true;
    };
  }, [api, imageCandidates, openPhotoPickerKeys]);

  const selectedCustomer = useMemo(
    () => customersPage?.content?.find((c) => c.id === customerId) ?? null,
    [customersPage, customerId]
  );
  const selectedJob = useMemo(
    () => jobsPage?.content?.find((j) => j.id === jobId) ?? null,
    [jobsPage, jobId]
  );
  const selectedTenant = auth.tenants.find((t) => t.tenantId === auth.selectedTenantId) ?? null;
  const relatedJobLabel =
    report?.jobDisplayName ??
    (selectedJob
      ? formatAddress(selectedJob.propertyAddress) !== "—"
        ? formatAddress(selectedJob.propertyAddress)
        : selectedJob.statusLabel ?? selectedJob.statusKey ?? "Related job"
      : null);
  const reportDate = report?.updatedAt ?? report?.createdAt ?? null;
  const defaultEmailSubject = `${selectedTenant?.tenantName ?? "Your roofing team"} - ${
    title.trim() || report?.title || "Customer photo report"
  }`;
  const defaultEmailMessage = `Attached is your customer photo report${
    selectedCustomer ? ` for ${selectedCustomer.firstName} ${selectedCustomer.lastName}` : ""
  }. Please let us know if you have any questions.`;

  const updateSection = (sectionKey: string, updater: (section: SectionDraft) => SectionDraft) => {
    setSections((prev) => prev.map((section) => (section.localKey === sectionKey ? updater(section) : section)));
  };

  const buildPayload = useCallback((): UpsertCustomerPhotoReportRequest => {
    const cid = customerId.trim();
    const jid = jobId.trim();
    return {
      customerId: cid,
      jobId: jid.length > 0 ? jid : null,
      title: title.trim() || "Untitled report",
      reportType: reportType.trim() || null,
      summary: summary.trim() || null,
      sections: sections.map((s) => ({
        title: s.title.trim() || "Section",
        body: s.body.trim() || null,
        attachmentIds: sanitizeAttachmentIds(s.attachmentIds),
      })),
    };
  }, [customerId, jobId, title, reportType, summary, sections]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      setSaveError(null);
      if (!customerId.trim()) throw new Error("Choose a customer.");
      if (reportId) {
        return updateCustomerPhotoReport(api, reportId, buildPayload());
      }
      return createCustomerPhotoReport(api, buildPayload());
    },
    onSuccess: (dto) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.customerPhotoReports(auth.selectedTenantId) });
      queryClient.setQueryData(queryKeys.customerPhotoReport(auth.selectedTenantId, dto.id), dto);
      if (!reportId) {
        router.replace(`/app/reports/customer-reports/${dto.id}`);
      }
    },
    onError: (e: unknown) => {
      setSaveError(getApiErrorMessage(e, "Could not save report."));
    },
  });

  const sendEmailMutation = useMutation({
    mutationFn: (payload: SendCustomerPhotoReportEmailRequest) => {
      if (!reportId) {
        throw new Error("Save the report before emailing it.");
      }
      return sendCustomerPhotoReportEmail(api, reportId, payload);
    },
    onSuccess: (_data, variables) => {
      setEmailSuccess(`Email sent to ${variables.recipientEmail}.`);
      setShowEmailModal(false);
    },
    onError: () => {
      setEmailSuccess(null);
    },
  });

  const handleDownloadPdf = async () => {
    if (!reportId) return;
    setPdfError(null);
    try {
      const { blob, filename } = await downloadCustomerPhotoReportPdf(api, reportId);
      triggerBrowserDownload(blob, filename);
    } catch {
      setPdfError("Could not download PDF.");
    }
  };

  const togglePhoto = (sectionKey: string, attachmentId: string) => {
    updateSection(sectionKey, (section) => {
      const has = section.attachmentIds.includes(attachmentId);
      return {
        ...section,
        attachmentIds: has
          ? section.attachmentIds.filter((id) => id !== attachmentId)
          : [...section.attachmentIds, attachmentId],
      };
    });
  };

  const openPhotoPicker = (sectionKey: string) => {
    setOpenPhotoPickerKeys((prev) => (prev.includes(sectionKey) ? prev : [...prev, sectionKey]));
  };

  const closePhotoPicker = (sectionKey: string) => {
    setOpenPhotoPickerKeys((prev) => prev.filter((key) => key !== sectionKey));
  };

  const renderPhotoThumbnail = (
    attachmentId: string,
    fileName: string | undefined,
    testId: string,
    altLabel: string
  ) => {
    const previewUrl = photoPreviewUrls[attachmentId];
    const previewLoading = loadingPhotoPreviewIds.includes(attachmentId);

    return (
      <div
        data-testid={testId}
        className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-md border border-slate-200 bg-slate-100"
      >
        {previewUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={previewUrl} alt={altLabel} className="h-full w-full object-cover" />
        ) : previewLoading ? (
          <span className="text-[10px] text-slate-400">Loading…</span>
        ) : (
          <span className="text-[10px] text-slate-400">No preview</span>
        )}
      </div>
    );
  };

  const removePhotoFromSection = (sectionKey: string, attachmentId: string) => {
    updateSection(sectionKey, (section) => ({
      ...section,
      attachmentIds: section.attachmentIds.filter((id) => id !== attachmentId),
    }));
  };

  const movePhotoInSection = (sectionKey: string, index: number, dir: -1 | 1) => {
    updateSection(sectionKey, (section) => {
      const target = index + dir;
      if (target < 0 || target >= section.attachmentIds.length) {
        return section;
      }
      const next = [...section.attachmentIds];
      const current = next[index];
      next[index] = next[target]!;
      next[target] = current!;
      return { ...section, attachmentIds: next };
    });
  };

  const moveSection = (index: number, dir: -1 | 1) => {
    setSections((prev) => {
      const j = index + dir;
      if (j < 0 || j >= prev.length) return prev;
      const next = [...prev];
      const t = next[index]!;
      next[index] = next[j]!;
      next[j] = t;
      return next;
    });
  };

  const onUploadToSection = async (sectionKey: string, e: React.ChangeEvent<HTMLInputElement>) => {
    if (!jobId.trim()) return;
    const files = e.target.files;
    if (!files?.length) return;
    setSaveError(null);
    setUploadingSectionKey(sectionKey);
    try {
      const uploaded: AttachmentDto[] = [];
      for (const file of Array.from(files)) {
        const attachment = await uploadJobAttachment(api, jobId, file, { tag: "OTHER" });
        uploaded.push(attachment);
      }
      setRecentUploadsById((prev) => {
        const next = { ...prev };
        for (const attachment of uploaded) {
          next[attachment.id] = attachment;
        }
        return next;
      });
      updateSection(sectionKey, (section) => ({
        ...section,
        attachmentIds: Array.from(new Set([...section.attachmentIds, ...uploaded.map((item) => item.id)])),
      }));
      await queryClient.invalidateQueries({
        queryKey: queryKeys.customerPhotoReportCandidates(auth.selectedTenantId, customerId, jobId),
      });
      await queryClient.invalidateQueries({
        queryKey: queryKeys.jobAttachments(auth.selectedTenantId, jobId),
      });
    } catch (err) {
      setSaveError(getApiErrorMessage(err, "Upload failed."));
    } finally {
      setUploadingSectionKey(null);
    }
    e.target.value = "";
  };

  if (reportId && reportLoading && !report) {
    return (
      <div className="text-sm text-slate-500 py-12 text-center">Loading report…</div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-8 pb-16">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link
          href="/app/reports/customer-reports"
          className="text-sm text-sky-600 hover:text-sky-700"
        >
          ← All photo reports
        </Link>
        <div className="flex flex-wrap gap-2">
          {reportId && (
            <button
              type="button"
              onClick={() => {
                setEmailSuccess(null);
                setShowEmailModal(true);
              }}
              disabled={sendEmailMutation.isPending}
              className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-800 hover:bg-slate-50 disabled:opacity-60"
            >
              {sendEmailMutation.isPending ? "Sending…" : "Send Email"}
            </button>
          )}
          {reportId && (
            <button
              type="button"
              onClick={() => handleDownloadPdf()}
              className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-200 bg-white text-slate-800 hover:bg-slate-50"
            >
              Download PDF
            </button>
          )}
          <button
            type="button"
            disabled={saveMutation.isPending || !customerId}
            onClick={() => saveMutation.mutate()}
            className="px-4 py-1.5 text-sm font-medium rounded-lg bg-sky-600 text-white hover:bg-sky-700 disabled:opacity-50"
          >
            {saveMutation.isPending ? "Saving…" : "Save"}
          </button>
        </div>
      </div>

      {pdfError && <p className="text-sm text-red-600">{pdfError}</p>}
      {saveError && <p className="text-sm text-red-600">{saveError}</p>}
      {emailSuccess && <p className="text-sm text-green-700">{emailSuccess}</p>}

      <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-sm space-y-4">
        <h2 className="text-sm font-semibold text-slate-800">Report details</h2>
        {(relatedJobLabel || reportDate) && (
          <div className="grid gap-3 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 sm:grid-cols-2">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Related Job</p>
              <p data-testid="report-related-job">{relatedJobLabel ?? "No specific job"}</p>
            </div>
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Report Date</p>
              <p data-testid="report-date">{formatDate(reportDate)}</p>
            </div>
          </div>
        )}
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Customer</label>
          <select
            value={customerId}
            onChange={(e) => {
              setCustomerId(e.target.value);
              setJobId("");
            }}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
          >
            <option value="">Select customer…</option>
            {customersPage?.content?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.firstName} {c.lastName}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">
            Related job (optional — enables job photo library & uploads)
          </label>
          <select
            value={jobId}
            onChange={(e) => setJobId(e.target.value)}
            disabled={!customerId}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm disabled:bg-slate-100"
          >
            <option value="">No specific job</option>
            {jobsPage?.content?.map((j) => (
              <option key={j.id} value={j.id}>
                {(formatAddress(j.propertyAddress) !== "—" ? formatAddress(j.propertyAddress) : "Job") +
                  ((j.statusLabel ?? j.statusKey) ? ` — ${j.statusLabel ?? j.statusKey}` : "")}
              </option>
            ))}
          </select>
          {jobId && relatedJobLabel && <p className="mt-1 text-xs text-slate-500">{relatedJobLabel}</p>}
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Title</label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            placeholder="e.g. Roof inspection — 123 Oak St"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Report type</label>
          <input
            value={reportType}
            onChange={(e) => setReportType(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            placeholder="Inspection, Before / After, Scope…"
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">Summary / introduction</label>
          <textarea
            value={summary}
            onChange={(e) => setSummary(e.target.value)}
            rows={4}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            placeholder="Short overview for the customer…"
          />
        </div>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-slate-800">Sections</h2>
          <button
            type="button"
            onClick={() => setSections((s) => [...s, newSection()])}
            className="text-sm text-sky-600 hover:text-sky-700 font-medium"
          >
            + Add section
          </button>
        </div>

        {sections.map((sec, idx) => (
          <div
            key={sec.localKey}
            data-testid={`report-section-${idx + 1}`}
            className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3"
          >
            {(() => {
              const pickerOpen = openPhotoPickerKeys.includes(sec.localKey);
              return (
                <>
            <div className="flex flex-wrap items-center gap-2 justify-between">
              <span className="text-xs font-medium text-slate-500">Section {idx + 1}</span>
              <div className="flex gap-1">
                <button
                  type="button"
                  onClick={() => moveSection(idx, -1)}
                  disabled={idx === 0}
                  className="px-2 py-0.5 text-xs border rounded border-slate-200 disabled:opacity-40"
                >
                  Up
                </button>
                <button
                  type="button"
                  onClick={() => moveSection(idx, 1)}
                  disabled={idx === sections.length - 1}
                  className="px-2 py-0.5 text-xs border rounded border-slate-200 disabled:opacity-40"
                >
                  Down
                </button>
                <button
                  type="button"
                  onClick={() => setSections((prev) => prev.filter((_, i) => i !== idx))}
                  disabled={sections.length <= 1}
                  className="px-2 py-0.5 text-xs text-red-700 border border-red-200 rounded disabled:opacity-40"
                >
                  Remove
                </button>
              </div>
            </div>
            <input
              value={sec.title}
              onChange={(e) =>
                setSections((prev) =>
                  prev.map((s) => (s.localKey === sec.localKey ? { ...s, title: e.target.value } : s))
                )
              }
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm font-medium"
              placeholder="Section title"
            />
            <textarea
              value={sec.body}
              onChange={(e) =>
                updateSection(sec.localKey, (section) => ({ ...section, body: e.target.value }))
              }
              rows={5}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
              placeholder="Details, findings, recommendations…"
            />
            <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-4 space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Section photos</p>
                  <p className="text-xs text-slate-500">
                    Choose existing {jobId ? "job" : "customer"} photos or upload new ones for this section only.
                  </p>
                </div>
                <span className="rounded-full bg-white px-2.5 py-1 text-xs font-medium text-slate-600 border border-slate-200">
                  {sec.attachmentIds.length} selected
                </span>
              </div>

              <div className="space-y-2">
                <p className="text-xs font-medium text-slate-600">Included in this section</p>
                {sec.attachmentIds.length === 0 ? (
                  <p className="text-xs text-slate-500">No photos selected yet.</p>
                ) : (
                  <ul className="space-y-2">
                    {sec.attachmentIds.map((attachmentId, photoIndex) => {
                      const attachment = imageOptionById.get(attachmentId);
                      return (
                        <li
                          key={`${sec.localKey}-${attachmentId}`}
                          className="flex flex-wrap items-center gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2"
                        >
                          {renderPhotoThumbnail(
                            attachmentId,
                            attachment?.fileName,
                            `selected-photo-thumbnail-${attachmentId}`,
                            `Selected ${attachment?.fileName ?? attachmentId}`
                          )}
                          <span className="text-xs font-medium text-slate-500">{photoIndex + 1}.</span>
                          <span className="min-w-0 flex-1">
                            <span className="block truncate text-sm text-slate-800">
                              {attachment?.fileName ?? attachmentId}
                            </span>
                            <span className="block text-xs text-slate-500">
                              {(attachment?.description ?? "").trim() ||
                                attachment?.contentType ||
                                "Selected image"}
                            </span>
                          </span>
                          <button
                            type="button"
                            onClick={() => movePhotoInSection(sec.localKey, photoIndex, -1)}
                            disabled={photoIndex === 0}
                            aria-label={`Move ${attachment?.fileName ?? attachmentId} up in section ${idx + 1}`}
                            className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-600 disabled:opacity-40"
                          >
                            Up
                          </button>
                          <button
                            type="button"
                            onClick={() => movePhotoInSection(sec.localKey, photoIndex, 1)}
                            disabled={photoIndex === sec.attachmentIds.length - 1}
                            aria-label={`Move ${attachment?.fileName ?? attachmentId} down in section ${idx + 1}`}
                            className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-600 disabled:opacity-40"
                          >
                            Down
                          </button>
                          <button
                            type="button"
                            onClick={() => removePhotoFromSection(sec.localKey, attachmentId)}
                            aria-label={`Remove ${attachment?.fileName ?? attachmentId} from section ${idx + 1}`}
                            className="rounded border border-red-200 px-2 py-1 text-xs text-red-700 hover:bg-red-50"
                          >
                            Remove
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>

              <div className="space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs font-medium text-slate-600">
                    {jobId ? "Existing job photos" : "Existing customer photos"}
                  </p>
                  {!pickerOpen ? (
                    <button
                      type="button"
                      onClick={() => openPhotoPicker(sec.localKey)}
                      disabled={!customerId || imageCandidates.length === 0}
                      className="rounded border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {jobId ? "Select Existing Photos" : "Select Existing Customer Photos"}
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() => closePhotoPicker(sec.localKey)}
                      className="rounded bg-sky-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-sky-700"
                    >
                      Confirm Selection
                    </button>
                  )}
                </div>
                {!customerId ? (
                  <p className="text-xs text-slate-500">Select a customer to load photos.</p>
                ) : imageCandidates.length === 0 ? (
                  <p className="text-xs text-slate-500">
                    No image attachments are available yet for this {jobId ? "job" : "customer"}.
                  </p>
                ) : pickerOpen ? (
                  <ul
                    data-testid={`photo-picker-list-${idx + 1}`}
                    className="max-h-64 overflow-y-auto rounded-lg border border-slate-200 divide-y divide-slate-100 bg-white text-sm"
                  >
                    {imageCandidates.map((c) => {
                      const selected = sec.attachmentIds.includes(c.id);
                      return (
                        <li
                          key={c.id}
                          className={`px-3 py-2 ${selected ? "bg-sky-50/70" : ""}`}
                        >
                          <label
                            htmlFor={`${sec.localKey}-${c.id}`}
                            className="flex cursor-pointer items-center gap-3"
                          >
                            <input
                              type="checkbox"
                              checked={selected}
                              onChange={() => togglePhoto(sec.localKey, c.id)}
                              id={`${sec.localKey}-${c.id}`}
                            />
                            {renderPhotoThumbnail(
                              c.id,
                              c.fileName,
                              `photo-thumbnail-${c.id}`,
                              c.fileName ?? "Attachment preview"
                            )}
                            <span className="min-w-0 flex-1">
                              <span className="block truncate text-slate-800">{c.fileName ?? c.id}</span>
                              <span className="block text-xs text-slate-500">
                                {(c.description ?? "").trim() || c.contentType || "Image attachment"}
                              </span>
                            </span>
                          </label>
                        </li>
                      );
                    })}
                  </ul>
                ) : null}
              </div>

              <div className="space-y-2">
                <label className="block text-xs font-medium text-slate-600">
                  {jobId ? "Upload new photos to this section" : "Upload new photos"}
                </label>
                {jobId ? (
                  <input
                    type="file"
                    accept="image/*"
                    multiple
                    onChange={(e) => void onUploadToSection(sec.localKey, e)}
                    className="text-sm"
                    aria-label={`Upload photos to section ${idx + 1}`}
                  />
                ) : (
                  <p className="text-xs text-slate-500">
                    Choose a related job to upload new photos. Uploaded files are stored on that job and selected for
                    this section only.
                  </p>
                )}
                {uploadingSectionKey === sec.localKey && (
                  <p className="text-xs text-slate-500">Uploading photos…</p>
                )}
              </div>
            </div>
                </>
              );
            })()}
          </div>
        ))}
      </div>
      {showEmailModal && reportId && (
        <SendEmailModal
          title="Send report by email"
          isSubmitting={sendEmailMutation.isPending}
          error={
            sendEmailMutation.isError
              ? getApiErrorMessage(sendEmailMutation.error, "Failed to send report email.")
              : null
          }
          initialRecipientEmail={report?.customerEmail ?? selectedCustomer?.email ?? ""}
          initialRecipientName={
            report?.customerName ?? (selectedCustomer ? `${selectedCustomer.firstName} ${selectedCustomer.lastName}` : "")
          }
          initialSubject={defaultEmailSubject}
          initialMessage={defaultEmailMessage}
          showSubjectField
          showExpiresField={false}
          onClose={() => setShowEmailModal(false)}
          onSubmit={(values) => sendEmailMutation.mutate(values)}
        />
      )}
    </div>
  );
}
