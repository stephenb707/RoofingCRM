"use client";

import type { ReactNode } from "react";
import { useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import {
  confirmReceiptCost,
  createCostFromReceipt,
  createJobCostEntry,
  deleteJobCostEntry,
  deleteJobReceipt,
  extractReceiptDetails,
  getReceiptExtraction,
  getJobAccountingSummary,
  linkReceiptToCost,
  listJobCostEntries,
  listJobReceipts,
  unlinkReceiptFromCost,
  updateJobCostEntry,
  uploadJobReceipt,
} from "@/lib/accountingApi";
import { downloadAttachment } from "@/lib/attachmentsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { JOB_COST_CATEGORIES, JOB_COST_CATEGORY_LABELS } from "@/lib/accountingConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatDate, formatDateOnly, formatFileSize, formatMoney } from "@/lib/format";
import { DatePickerField } from "@/components/DatePickerField";
import type {
  ConfirmReceiptCostRequest,
  CreateCostFromReceiptRequest,
  ExtractReceiptResponseDto,
  CreateJobCostEntryRequest,
  JobCostCategory,
  JobCostEntryDto,
  JobReceiptDto,
  ReceiptExtractionResultDto,
  ReceiptExtractionStatus,
  UpdateJobCostEntryRequest,
} from "@/lib/types";

type CostFilter = "ALL" | JobCostCategory;

interface CostFormState {
  category: JobCostCategory;
  vendorName: string;
  description: string;
  amount: string;
  incurredAt: string;
  notes: string;
}

export interface AccountingSectionProps {
  jobId: string;
}

const EMPTY_CATEGORY_TOTALS: Record<JobCostCategory, number> = {
  MATERIAL: 0,
  TRANSPORTATION: 0,
  LABOR: 0,
  OTHER: 0,
};

function getDefaultFormState(): CostFormState {
  return {
    category: "MATERIAL",
    vendorName: "",
    description: "",
    amount: "",
    incurredAt: formatDateOnly(new Date()),
    notes: "",
  };
}

function toApiDate(dateOnly: string): string {
  return `${dateOnly}T12:00:00Z`;
}

function formatPercent(value?: number | null): string {
  if (value == null || Number.isNaN(value)) return "—";
  return `${value.toFixed(1)}%`;
}

function ReviewCue() {
  return (
    <span
      className="ml-1.5 inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide bg-amber-100 text-amber-800"
      data-testid="review-cue"
    >
      Review
    </span>
  );
}

function fieldRingClass(needs: boolean): string {
  return needs ? "rounded-lg ring-2 ring-amber-200/90 border border-amber-200/80 p-2 -m-px" : "";
}

function extractionWarningsLower(ex: ReceiptExtractionResultDto | null): string {
  return filterUiExtractionWarnings(ex?.extractionWarnings).join(" ").toLowerCase();
}

/** Backend reconciliation notes we do not surface in the simplified receipt review UI. */
function isReconciliationWarningNoise(warning: string): boolean {
  const t = warning.trim().toLowerCase();
  if (!t) return true;
  if (t.includes("tax total was derived from subtotal and tax rate")) return true;
  if (t.includes("total was derived from subtotal and tax")) return true;
  if (t.includes("amount paid was aligned")) return true;
  if (t.includes("tax total was derived from subtotal and total")) return true;
  return false;
}

function filterUiExtractionWarnings(warnings: string[] | null | undefined): string[] {
  if (!warnings?.length) return [];
  return warnings.filter((w) => !isReconciliationWarningNoise(w));
}

function vendorNeedsReview(ex: ReceiptExtractionResultDto | null, vendor: string): boolean {
  if (!ex) return false;
  if (!vendor.trim()) return true;
  return (ex.confidence ?? 100) < 55;
}

function dateNeedsReview(ex: ReceiptExtractionResultDto | null): boolean {
  if (!ex) return false;
  const w = extractionWarningsLower(ex);
  if (w.includes("date") || w.includes("incurred")) return true;
  return !ex.incurredAt;
}

function amountFieldNeedsReview(ex: ReceiptExtractionResultDto | null): boolean {
  if (!ex) return false;
  return ex.amountConfidence === "LOW" || ex.amountConfidence === "MEDIUM";
}

export function AccountingSection({ jobId }: AccountingSectionProps) {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const receiptFileInputRef = useRef<HTMLInputElement>(null);

  const [filter, setFilter] = useState<CostFilter>("ALL");
  const [showCostModal, setShowCostModal] = useState(false);
  const [editingEntry, setEditingEntry] = useState<JobCostEntryDto | null>(null);
  const [manualReceiptTarget, setManualReceiptTarget] = useState<JobReceiptDto | null>(null);
  const [reviewReceiptTarget, setReviewReceiptTarget] = useState<JobReceiptDto | null>(null);
  const [showLinkModal, setShowLinkModal] = useState(false);
  const [linkReceiptTarget, setLinkReceiptTarget] = useState<JobReceiptDto | null>(null);
  const [selectedLinkCostId, setSelectedLinkCostId] = useState("");
  const [formState, setFormState] = useState<CostFormState>(getDefaultFormState());
  const [formError, setFormError] = useState<string | null>(null);
  const [receiptError, setReceiptError] = useState<string | null>(null);
  const [receiptDescription, setReceiptDescription] = useState("");
  const [extractingReceiptId, setExtractingReceiptId] = useState<string | null>(null);
  const [loadingExtractionReceiptId, setLoadingExtractionReceiptId] = useState<string | null>(null);
  const [reviewExtraction, setReviewExtraction] = useState<ReceiptExtractionResultDto | null>(null);

  const selectedTenant = auth.tenants.find((tenant) => tenant.tenantId === auth.selectedTenantId);
  const canEdit =
    selectedTenant?.role === "OWNER" ||
    selectedTenant?.role === "ADMIN" ||
    selectedTenant?.role === "SALES";

  const summaryQuery = useQuery({
    queryKey: queryKeys.jobAccountingSummary(auth.selectedTenantId, jobId),
    queryFn: () => getJobAccountingSummary(api, jobId),
    enabled: ready && !!jobId,
  });

  const costsQuery = useQuery({
    queryKey: queryKeys.jobCostEntries(auth.selectedTenantId, jobId),
    queryFn: () => listJobCostEntries(api, jobId),
    enabled: ready && !!jobId,
  });

  const receiptsQuery = useQuery({
    queryKey: queryKeys.jobReceipts(auth.selectedTenantId, jobId),
    queryFn: () => listJobReceipts(api, jobId),
    enabled: ready && !!jobId,
  });

  const invalidateAccountingQueries = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.jobAccountingSummary(auth.selectedTenantId, jobId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.jobCostEntries(auth.selectedTenantId, jobId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.jobReceipts(auth.selectedTenantId, jobId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.jobAttachments(auth.selectedTenantId, jobId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.activityForEntity(auth.selectedTenantId, "JOB", jobId) }),
    ]);
  };

  const createMutation = useMutation({
    mutationFn: (payload: CreateJobCostEntryRequest) => createJobCostEntry(api, jobId, payload),
    onSuccess: async () => {
      closeCostModal();
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setFormError(getApiErrorMessage(error, "Failed to save cost entry."));
    },
  });

  const createFromReceiptMutation = useMutation({
    mutationFn: ({ receiptId, payload }: { receiptId: string; payload: CreateCostFromReceiptRequest }) =>
      createCostFromReceipt(api, jobId, receiptId, payload),
    onSuccess: async () => {
      closeCostModal();
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setFormError(getApiErrorMessage(error, "Failed to create cost from receipt."));
    },
  });

  const confirmReceiptCostMutation = useMutation({
    mutationFn: ({ receiptId, payload }: { receiptId: string; payload: ConfirmReceiptCostRequest }) =>
      confirmReceiptCost(api, jobId, receiptId, payload),
    onSuccess: async () => {
      closeCostModal();
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setFormError(getApiErrorMessage(error, "Failed to save reviewed receipt cost."));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ costEntryId, payload }: { costEntryId: string; payload: UpdateJobCostEntryRequest }) =>
      updateJobCostEntry(api, jobId, costEntryId, payload),
    onSuccess: async () => {
      closeCostModal();
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setFormError(getApiErrorMessage(error, "Failed to update cost entry."));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (costEntryId: string) => deleteJobCostEntry(api, jobId, costEntryId),
    onSuccess: async () => {
      await invalidateAccountingQueries();
    },
  });

  const uploadReceiptMutation = useMutation({
    mutationFn: ({ file, description }: { file: File; description?: string | null }) =>
      uploadJobReceipt(api, jobId, file, description),
    onSuccess: async () => {
      setReceiptDescription("");
      setReceiptError(null);
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setReceiptError(getApiErrorMessage(error, "Failed to upload receipt."));
    },
  });

  const extractReceiptMutation = useMutation({
    mutationFn: ({ receiptId }: { receiptId: string }) => extractReceiptDetails(api, jobId, receiptId),
    onMutate: ({ receiptId }) => {
      setExtractingReceiptId(receiptId);
      setReceiptError(null);
    },
    onSuccess: async (response, variables) => {
      await invalidateAccountingQueries();
      if (response.status === "COMPLETED" && response.result) {
        const receipt = receipts.find((item) => item.id === variables.receiptId);
        if (receipt) {
          openReviewFromExtraction(receipt, response.result);
        }
        return;
      }
      setReceiptError(
        response.error ??
          "We couldn't reliably extract details from this receipt. You can retry or enter it manually."
      );
    },
    onError: (error: unknown) => {
      setReceiptError(getApiErrorMessage(error, "Failed to extract receipt details."));
    },
    onSettled: () => {
      setExtractingReceiptId(null);
    },
  });

  const loadExtractionMutation = useMutation({
    mutationFn: ({ receiptId }: { receiptId: string }) => getReceiptExtraction(api, jobId, receiptId),
    onMutate: ({ receiptId }) => {
      setLoadingExtractionReceiptId(receiptId);
      setReceiptError(null);
    },
    onSuccess: (response, variables) => {
      if (response.status !== "COMPLETED" || !response.result) {
        setReceiptError(
          response.error ??
            "We couldn't reliably extract details from this receipt. You can retry or enter it manually."
        );
        return;
      }
      const receipt = receipts.find((item) => item.id === variables.receiptId);
      if (!receipt) {
        setReceiptError("Receipt not found.");
        return;
      }
      openReviewFromExtraction(receipt, response.result);
    },
    onError: (error: unknown) => {
      setReceiptError(getApiErrorMessage(error, "Failed to load extracted receipt details."));
    },
    onSettled: () => {
      setLoadingExtractionReceiptId(null);
    },
  });

  const linkReceiptMutation = useMutation({
    mutationFn: ({ receiptId, costEntryId }: { receiptId: string; costEntryId: string }) =>
      linkReceiptToCost(api, jobId, receiptId, costEntryId),
    onSuccess: async () => {
      closeLinkModal();
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setReceiptError(getApiErrorMessage(error, "Failed to link receipt to cost."));
    },
  });

  const unlinkReceiptMutation = useMutation({
    mutationFn: (receiptId: string) => unlinkReceiptFromCost(api, jobId, receiptId),
    onSuccess: async () => {
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setReceiptError(getApiErrorMessage(error, "Failed to unlink receipt."));
    },
  });

  const deleteReceiptMutation = useMutation({
    mutationFn: (receiptId: string) => deleteJobReceipt(api, jobId, receiptId),
    onSuccess: async () => {
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setReceiptError(getApiErrorMessage(error, "Failed to delete receipt."));
    },
  });

  const entries = costsQuery.data ?? [];
  const receipts = receiptsQuery.data ?? [];
  const summary = summaryQuery.data;
  const categoryTotals = summary?.categoryTotals ?? EMPTY_CATEGORY_TOTALS;

  const filteredEntries = useMemo(() => {
    if (filter === "ALL") return entries;
    return entries.filter((entry) => entry.category === filter);
  }, [entries, filter]);

  const groupedEntries = useMemo(() => {
    const groups: Array<{ category: JobCostCategory; entries: JobCostEntryDto[] }> = [];
    for (const category of JOB_COST_CATEGORIES) {
      const categoryEntries = filteredEntries.filter((entry) => entry.category === category);
      if (categoryEntries.length > 0) {
        groups.push({ category, entries: categoryEntries });
      }
    }
    return groups;
  }, [filteredEntries]);

  const isSaving =
    createMutation.isPending ||
    updateMutation.isPending ||
    createFromReceiptMutation.isPending ||
    confirmReceiptCostMutation.isPending;
  const isLoading = summaryQuery.isLoading || costsQuery.isLoading || receiptsQuery.isLoading;
  const isError = summaryQuery.isError || costsQuery.isError || receiptsQuery.isError;

  const openCreateModal = () => {
    setEditingEntry(null);
    setManualReceiptTarget(null);
    setReviewReceiptTarget(null);
    setReviewExtraction(null);
    setFormError(null);
    setFormState(getDefaultFormState());
    setShowCostModal(true);
  };

  const openCreateFromReceiptModal = (receipt: JobReceiptDto) => {
    setEditingEntry(null);
    setManualReceiptTarget(receipt);
    setReviewReceiptTarget(null);
    setReviewExtraction(null);
    setFormError(null);
    setFormState({
      ...getDefaultFormState(),
      description: receipt.description ?? "Receipt expense",
    });
    setShowCostModal(true);
  };

  function openReviewFromExtraction(receipt: JobReceiptDto, extraction: ReceiptExtractionResultDto) {
    setEditingEntry(null);
    setManualReceiptTarget(null);
    setReviewReceiptTarget(receipt);
    setReviewExtraction(extraction);
    setFormError(null);
    setFormState({
      category: extraction.suggestedCategory ?? "MATERIAL",
      vendorName: extraction.vendorName ?? "",
      description: extraction.vendorName ? `Receipt from ${extraction.vendorName}` : "Receipt expense",
      amount: extraction.amount != null ? String(extraction.amount) : "",
      incurredAt: extraction.incurredAt ? extraction.incurredAt.slice(0, 10) : formatDateOnly(new Date()),
      notes: extraction.notes ?? "",
    });
    setShowCostModal(true);
  }

  const openEditModal = (entry: JobCostEntryDto) => {
    setEditingEntry(entry);
    setManualReceiptTarget(null);
    setReviewReceiptTarget(null);
    setReviewExtraction(null);
    setFormError(null);
    setFormState({
      category: entry.category,
      vendorName: entry.vendorName ?? "",
      description: entry.description,
      amount: String(entry.amount),
      incurredAt: entry.incurredAt.slice(0, 10),
      notes: entry.notes ?? "",
    });
    setShowCostModal(true);
  };

  const closeCostModal = () => {
    setShowCostModal(false);
    setEditingEntry(null);
    setManualReceiptTarget(null);
    setReviewReceiptTarget(null);
    setReviewExtraction(null);
    setFormError(null);
    setFormState(getDefaultFormState());
  };

  const openLinkModal = (receipt: JobReceiptDto) => {
    setLinkReceiptTarget(receipt);
    setSelectedLinkCostId("");
    setReceiptError(null);
    setShowLinkModal(true);
  };

  const closeLinkModal = () => {
    setShowLinkModal(false);
    setLinkReceiptTarget(null);
    setSelectedLinkCostId("");
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!formState.description.trim()) {
      setFormError("Description is required.");
      return;
    }
    if (!formState.incurredAt) {
      setFormError("Incurred date is required.");
      return;
    }
    const amount = Number(formState.amount);
    if (!Number.isFinite(amount) || amount < 0) {
      setFormError("Amount must be 0 or greater.");
      return;
    }

    const notesPayload = formState.notes.trim() || null;

    const payload = {
      category: formState.category,
      vendorName: formState.vendorName.trim() || null,
      description: formState.description.trim(),
      amount,
      incurredAt: toApiDate(formState.incurredAt),
      notes: notesPayload,
    };

    if (editingEntry) {
      updateMutation.mutate({ costEntryId: editingEntry.id, payload });
      return;
    }

    if (reviewReceiptTarget) {
      confirmReceiptCostMutation.mutate({ receiptId: reviewReceiptTarget.id, payload });
      return;
    }

    if (manualReceiptTarget) {
      createFromReceiptMutation.mutate({ receiptId: manualReceiptTarget.id, payload });
      return;
    }

    createMutation.mutate(payload);
  };

  const handleDelete = (entry: JobCostEntryDto) => {
    if (typeof window !== "undefined" && !window.confirm("Delete this cost entry?")) {
      return;
    }
    deleteMutation.mutate(entry.id);
  };

  const handleReceiptFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    uploadReceiptMutation.mutate({
      file,
      description: receiptDescription.trim() || null,
    });
    event.target.value = "";
  };

  const handleViewReceipt = async (receipt: JobReceiptDto) => {
    try {
      const blob = await downloadAttachment(api, receipt.id);
      const url = URL.createObjectURL(blob);
      const opened = window.open(url, "_blank", "noopener,noreferrer");
      if (!opened) {
        const link = document.createElement("a");
        link.href = url;
        link.download = receipt.fileName;
        link.click();
      }
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (error) {
      setReceiptError(getApiErrorMessage(error, "Failed to open receipt."));
    }
  };

  const handleUnlinkReceipt = (receiptId: string) => {
    unlinkReceiptMutation.mutate(receiptId);
  };

  const handleDeleteReceipt = (receiptId: string) => {
    if (typeof window !== "undefined" && !window.confirm("Delete this receipt?")) {
      return;
    }
    deleteReceiptMutation.mutate(receiptId);
  };

  const handleLinkSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!linkReceiptTarget || !selectedLinkCostId) return;
    linkReceiptMutation.mutate({
      receiptId: linkReceiptTarget.id,
      costEntryId: selectedLinkCostId,
    });
  };

  const handleReviewExtractedReceipt = (receipt: JobReceiptDto) => {
    if (receipt.extractionResult && receipt.extractionStatus === "COMPLETED") {
      openReviewFromExtraction(receipt, receipt.extractionResult);
      return;
    }
    loadExtractionMutation.mutate({ receiptId: receipt.id });
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Accounting</h2>
        <p className="text-sm text-slate-500">Loading accounting summary…</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Accounting</h2>
        <p className="text-sm text-red-600">
          {getApiErrorMessage(
            summaryQuery.error ?? costsQuery.error ?? receiptsQuery.error,
            "Failed to load accounting details."
          )}
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6" data-testid="accounting-section">
      <div className="flex items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-lg font-semibold text-slate-800">Accounting</h2>
          <p className="text-sm text-slate-500 mt-1">Track job revenue, costs, receipts, and profitability.</p>
        </div>
        {canEdit && (
          <button
            type="button"
            onClick={openCreateModal}
            className="px-3 py-1.5 text-sm font-medium text-sky-600 border border-sky-300 rounded-lg hover:bg-sky-50"
          >
            Add Cost
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-3 mb-6">
        <SummaryCard
          label="Agreed Amount"
          value={formatMoney(summary?.agreedAmount)}
          secondaryText={summary?.hasAcceptedEstimate ? undefined : "No accepted estimate yet"}
        />
        <SummaryCard label="Invoiced Amount" value={formatMoney(summary?.invoicedAmount)} />
        <SummaryCard label="Paid Amount" value={formatMoney(summary?.paidAmount)} />
        <SummaryCard label="Total Costs" value={formatMoney(summary?.totalCosts)} />
        <SummaryCard
          label="Profit"
          value={formatMoney(summary?.grossProfit)}
          secondaryText={
            summary?.projectedProfit != null ? `Projected ${formatMoney(summary.projectedProfit)}` : undefined
          }
        />
        <SummaryCard
          label="Margin"
          value={formatPercent(summary?.marginPercent)}
          secondaryText={
            summary?.projectedMarginPercent != null
              ? `Projected ${formatPercent(summary.projectedMarginPercent)}`
              : undefined
          }
        />
      </div>

      <div
        className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-8 lg:grid-cols-[repeat(auto-fill,minmax(14rem,1fr))]"
        data-testid="category-totals-grid"
      >
        {JOB_COST_CATEGORIES.map((category) => (
          <div key={category} className="min-w-0 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-medium uppercase tracking-wider text-slate-500 [overflow-wrap:anywhere]">
              {JOB_COST_CATEGORY_LABELS[category]}
            </p>
            <p className="mt-1 text-sm font-semibold text-slate-800">{formatMoney(categoryTotals[category])}</p>
          </div>
        ))}
      </div>

      <div className="mb-8" data-testid="receipts-section">
        <div className="flex items-center justify-between gap-4 mb-4">
          <div>
            <h3 className="text-base font-semibold text-slate-800">Receipts</h3>
            <p className="text-sm text-slate-500 mt-1">Upload receipt images or PDFs and link them to job costs.</p>
          </div>
        </div>

        {receiptError && (
          <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
            {receiptError}
          </div>
        )}

        {canEdit && (
          <div className="flex flex-wrap items-end gap-3 mb-4">
            <input
              ref={receiptFileInputRef}
              type="file"
              accept="image/*,.pdf,application/pdf"
              onChange={handleReceiptFileChange}
              className="hidden"
              aria-label="Choose receipt file"
            />
            <div className="flex-1 min-w-[220px]">
              <label htmlFor="receipt-description" className="block text-sm font-medium text-slate-700 mb-1">
                Receipt label
              </label>
              <input
                id="receipt-description"
                type="text"
                value={receiptDescription}
                onChange={(event) => setReceiptDescription(event.target.value)}
                placeholder="Optional note for this receipt"
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
              />
            </div>
            <button
              type="button"
              onClick={() => receiptFileInputRef.current?.click()}
              disabled={uploadReceiptMutation.isPending}
              className="px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg disabled:opacity-60"
            >
              {uploadReceiptMutation.isPending ? "Uploading…" : "Upload Receipt"}
            </button>
          </div>
        )}

        {receipts.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 px-6 py-8 text-center">
            <p className="text-sm text-slate-600">No receipts uploaded yet.</p>
            {canEdit && (
              <button
                type="button"
                onClick={() => receiptFileInputRef.current?.click()}
                className="mt-4 inline-flex rounded-lg bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700"
              >
                Upload Receipt
              </button>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {receipts.map((receipt) => (
              <div
                key={receipt.id}
                className="rounded-lg border border-slate-200 px-4 py-4 flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="text-sm font-medium text-slate-800 truncate">{receipt.fileName}</p>
                    <span className="inline-flex rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
                      Receipt
                    </span>
                    <span
                      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${getExtractionBadgeClasses(
                        receipt.extractionStatus
                      )}`}
                    >
                      {getExtractionBadgeLabel(receipt.extractionStatus)}
                    </span>
                  </div>
                  <p className="text-xs text-slate-500 mt-1">
                    Uploaded {formatDate(receipt.uploadedAt)}
                    {receipt.fileSize ? ` · ${formatFileSize(receipt.fileSize)}` : ""}
                    {receipt.contentType ? ` · ${receipt.contentType}` : ""}
                  </p>
                  {receipt.description && <p className="text-sm text-slate-600 mt-2">{receipt.description}</p>}
                  <p className="text-sm text-slate-600 mt-2">
                    {receipt.linkedCostEntryId
                      ? `Linked to: ${receipt.linkedCostEntryDescription ?? "Cost entry"}${
                          receipt.linkedCostEntryAmount != null
                            ? ` — ${formatMoney(receipt.linkedCostEntryAmount)}`
                            : ""
                        }`
                      : "Unlinked"}
                  </p>
                  {receipt.extractionStatus === "FAILED" && receipt.extractionError && (
                    <p className="text-sm text-red-600 mt-2">{receipt.extractionError}</p>
                  )}
                </div>
                <div className="flex flex-wrap gap-2 lg:justify-end">
                  <ReceiptActionButton onClick={() => handleViewReceipt(receipt)}>View / Download</ReceiptActionButton>
                  {!receipt.linkedCostEntryId && canEdit && (
                    <ReceiptActionButton
                      onClick={() => extractReceiptMutation.mutate({ receiptId: receipt.id })}
                      disabled={extractingReceiptId === receipt.id}
                    >
                      {extractingReceiptId === receipt.id
                        ? "Extracting…"
                        : receipt.extractionStatus === "FAILED"
                          ? "Retry extraction"
                          : "Extract details"}
                    </ReceiptActionButton>
                  )}
                  {!receipt.linkedCostEntryId && canEdit && receipt.extractionStatus === "COMPLETED" && (
                    <ReceiptActionButton
                      onClick={() => handleReviewExtractedReceipt(receipt)}
                      disabled={loadingExtractionReceiptId === receipt.id}
                    >
                      {loadingExtractionReceiptId === receipt.id ? "Loading review…" : "Review & Save Cost"}
                    </ReceiptActionButton>
                  )}
                  {!receipt.linkedCostEntryId && canEdit && (
                    <ReceiptActionButton onClick={() => openCreateFromReceiptModal(receipt)}>
                      Create Cost manually
                    </ReceiptActionButton>
                  )}
                  {!receipt.linkedCostEntryId && canEdit && entries.length > 0 && (
                    <ReceiptActionButton onClick={() => openLinkModal(receipt)}>Link to Cost</ReceiptActionButton>
                  )}
                  {receipt.linkedCostEntryId && canEdit && (
                    <ReceiptActionButton
                      onClick={() => handleUnlinkReceipt(receipt.id)}
                      disabled={unlinkReceiptMutation.isPending}
                    >
                      Unlink
                    </ReceiptActionButton>
                  )}
                  {canEdit && (
                    <button
                      type="button"
                      onClick={() => handleDeleteReceipt(receipt.id)}
                      disabled={deleteReceiptMutation.isPending}
                      className="px-3 py-1.5 text-sm font-medium text-red-600 border border-red-200 rounded-lg hover:bg-red-50 disabled:opacity-60"
                    >
                      Delete
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div className="flex flex-wrap gap-2">
          <FilterButton active={filter === "ALL"} onClick={() => setFilter("ALL")}>
            All
          </FilterButton>
          {JOB_COST_CATEGORIES.map((category) => (
            <FilterButton key={category} active={filter === category} onClick={() => setFilter(category)}>
              {JOB_COST_CATEGORY_LABELS[category]}
            </FilterButton>
          ))}
        </div>
      </div>

      {entries.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center">
          <p className="text-sm text-slate-600">No costs recorded yet.</p>
          {canEdit && (
            <button
              type="button"
              onClick={openCreateModal}
              className="mt-4 inline-flex rounded-lg bg-sky-600 px-4 py-2 text-sm font-medium text-white hover:bg-sky-700"
            >
              Add Cost
            </button>
          )}
        </div>
      ) : filteredEntries.length === 0 ? (
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-600">
          No costs in this category.
        </div>
      ) : (
        <div className="space-y-5">
          {groupedEntries.map((group) => (
            <div key={group.category}>
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-semibold text-slate-800">{JOB_COST_CATEGORY_LABELS[group.category]}</h3>
                <span className="text-xs font-medium text-slate-500">{formatMoney(categoryTotals[group.category])}</span>
              </div>
              <div className="overflow-x-auto rounded-lg border border-slate-200">
                <table className="min-w-full divide-y divide-slate-200">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Date</th>
                      <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Category</th>
                      <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Vendor</th>
                      <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Description</th>
                      <th className="px-4 py-2 text-right text-xs font-semibold text-slate-600">Amount</th>
                      <th className="px-4 py-2 text-right text-xs font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200 bg-white">
                    {group.entries.map((entry) => (
                      <tr key={entry.id} className="hover:bg-slate-50">
                        <td className="px-4 py-3 text-sm text-slate-700">{formatDate(entry.incurredAt)}</td>
                        <td className="px-4 py-3 text-sm text-slate-700">
                          {JOB_COST_CATEGORY_LABELS[entry.category]}
                        </td>
                        <td className="px-4 py-3 text-sm text-slate-700">{entry.vendorName || "—"}</td>
                        <td className="px-4 py-3 text-sm text-slate-700">
                          <div className="font-medium text-slate-800">{entry.description}</div>
                          {entry.notes && <div className="text-xs text-slate-500 mt-1">{entry.notes}</div>}
                        </td>
                        <td className="px-4 py-3 text-sm text-right font-medium text-slate-800">
                          {formatMoney(entry.amount)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          {canEdit ? (
                            <div className="inline-flex gap-3">
                              <button
                                type="button"
                                onClick={() => openEditModal(entry)}
                                className="text-sm font-medium text-sky-600 hover:text-sky-700"
                              >
                                Edit
                              </button>
                              <button
                                type="button"
                                onClick={() => handleDelete(entry)}
                                disabled={deleteMutation.isPending}
                                className="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-60"
                              >
                                Delete
                              </button>
                            </div>
                          ) : (
                            <span className="text-sm text-slate-400">—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}

      {deleteMutation.isError && (
        <p className="mt-4 text-sm text-red-600">
          {getApiErrorMessage(deleteMutation.error, "Failed to delete cost entry.")}
        </p>
      )}

      {showCostModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div
            className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6"
            role="dialog"
            aria-modal="true"
            aria-labelledby="receipt-cost-modal-title"
          >
            <h3 id="receipt-cost-modal-title" className="text-lg font-semibold text-slate-800 mb-2">
              {editingEntry
                ? "Edit Cost"
                : reviewReceiptTarget
                  ? "Review Extracted Receipt"
                  : manualReceiptTarget
                    ? "Create Cost from Receipt"
                    : "Add Cost"}
            </h3>
            {(
              reviewReceiptTarget ?? manualReceiptTarget
            ) && (
              <p className="text-sm text-slate-500 mb-4">
                Receipt: {(reviewReceiptTarget ?? manualReceiptTarget)?.fileName}
              </p>
            )}
            {reviewExtraction && hasReviewWarnings(reviewExtraction) && (
              <p className="text-xs text-amber-900 bg-amber-50 border border-amber-100 rounded px-2 py-1.5 mb-3">
                {(() => {
                  const ui = filterUiExtractionWarnings(reviewExtraction.extractionWarnings);
                  return ui.length > 0
                    ? ui.join(" ")
                    : "Please review the total before saving. We found multiple possible amounts.";
                })()}
              </p>
            )}
            <form onSubmit={handleSubmit} className="space-y-4" data-testid="receipt-review-form">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="cost-category" className="block text-sm font-medium text-slate-700 mb-1">
                    Category
                  </label>
                  <select
                    id="cost-category"
                    value={formState.category}
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        category: event.target.value as JobCostCategory,
                      }))
                    }
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  >
                    {JOB_COST_CATEGORIES.map((category) => (
                      <option key={category} value={category}>
                        {JOB_COST_CATEGORY_LABELS[category]}
                      </option>
                    ))}
                  </select>
                </div>
                <div
                  className={fieldRingClass(
                    Boolean(reviewReceiptTarget && reviewExtraction && dateNeedsReview(reviewExtraction))
                  )}
                >
                  <DatePickerField
                    id="cost-incurred-at"
                    label={
                      <>
                        Incurred date
                        {reviewReceiptTarget &&
                          reviewExtraction &&
                          dateNeedsReview(reviewExtraction) && <ReviewCue />}
                      </>
                    }
                    value={formState.incurredAt}
                    onChange={(value) =>
                      setFormState((current) => ({
                        ...current,
                        incurredAt: value,
                      }))
                    }
                    placeholder="Select date…"
                  />
                  {reviewReceiptTarget && reviewExtraction && dateNeedsReview(reviewExtraction) && (
                    <p className="text-xs text-amber-800 mt-1">Confirm against the printed receipt date.</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div
                  className={fieldRingClass(
                    Boolean(reviewReceiptTarget && reviewExtraction && vendorNeedsReview(reviewExtraction, formState.vendorName))
                  )}
                >
                  <label htmlFor="cost-vendor" className="block text-sm font-medium text-slate-700 mb-1">
                    Vendor name
                    {reviewReceiptTarget &&
                      reviewExtraction &&
                      vendorNeedsReview(reviewExtraction, formState.vendorName) && <ReviewCue />}
                  </label>
                  <input
                    id="cost-vendor"
                    type="text"
                    value={formState.vendorName}
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        vendorName: event.target.value,
                      }))
                    }
                    placeholder={reviewReceiptTarget ? "Enter vendor name" : undefined}
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                </div>
                <div
                  className={fieldRingClass(
                    Boolean(reviewReceiptTarget && reviewExtraction && amountFieldNeedsReview(reviewExtraction))
                  )}
                >
                  <label htmlFor="cost-amount" className="block text-sm font-medium text-slate-700 mb-1">
                    Amount
                    {reviewReceiptTarget &&
                      reviewExtraction &&
                      amountFieldNeedsReview(reviewExtraction) && <ReviewCue />}
                  </label>
                  <input
                    id="cost-amount"
                    type="number"
                    min="0"
                    step="0.01"
                    value={formState.amount}
                    onChange={(event) =>
                      setFormState((current) => ({
                        ...current,
                        amount: event.target.value,
                      }))
                    }
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                    required
                  />
                </div>
              </div>

              <div>
                <label htmlFor="cost-description" className="block text-sm font-medium text-slate-700 mb-1">
                  Description
                </label>
                <input
                  id="cost-description"
                  type="text"
                  value={formState.description}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      description: event.target.value,
                    }))
                  }
                  placeholder={reviewReceiptTarget ? "Short description of this receipt" : undefined}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  required
                />
              </div>

              <div>
                <label htmlFor="cost-notes" className="block text-sm font-medium text-slate-700 mb-1">
                  Notes
                </label>
                <textarea
                  id="cost-notes"
                  value={formState.notes}
                  onChange={(event) =>
                    setFormState((current) => ({
                      ...current,
                      notes: event.target.value,
                    }))
                  }
                  rows={3}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>

              {formError && <p className="text-sm text-red-600">{formError}</p>}

              <div className="flex gap-3 justify-end pt-2">
                <button
                  type="button"
                  onClick={closeCostModal}
                  className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 text-sm font-medium text-white bg-sky-600 rounded-lg hover:bg-sky-700 disabled:opacity-50"
                >
                  {isSaving
                    ? "Saving…"
                    : editingEntry
                      ? "Save Changes"
                      : reviewReceiptTarget
                        ? "Review & Save Cost"
                        : manualReceiptTarget
                          ? "Create Cost"
                        : "Add Cost"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showLinkModal && linkReceiptTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-lg font-semibold text-slate-800 mb-2">Link Receipt to Cost</h3>
            <p className="text-sm text-slate-500 mb-4">{linkReceiptTarget.fileName}</p>
            <form onSubmit={handleLinkSubmit} className="space-y-4">
              <div>
                <label htmlFor="receipt-link-cost" className="block text-sm font-medium text-slate-700 mb-1">
                  Cost entry
                </label>
                <select
                  id="receipt-link-cost"
                  value={selectedLinkCostId}
                  onChange={(event) => setSelectedLinkCostId(event.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  required
                >
                  <option value="">Select cost entry…</option>
                  {entries.map((entry) => (
                    <option key={entry.id} value={entry.id}>
                      {entry.description} — {formatMoney(entry.amount)}
                    </option>
                  ))}
                </select>
              </div>
              <div className="flex gap-3 justify-end pt-2">
                <button
                  type="button"
                  onClick={closeLinkModal}
                  className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!selectedLinkCostId || linkReceiptMutation.isPending}
                  className="px-4 py-2 text-sm font-medium text-white bg-sky-600 rounded-lg hover:bg-sky-700 disabled:opacity-50"
                >
                  {linkReceiptMutation.isPending ? "Linking…" : "Link Receipt"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function SummaryCard({
  label,
  value,
  secondaryText,
}: {
  label: string;
  value: string;
  secondaryText?: string;
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-xs font-medium uppercase tracking-wider text-slate-500">{label}</p>
      <p className="mt-1 text-lg font-semibold text-slate-800">{value}</p>
      {secondaryText && <p className="mt-1 text-xs text-slate-500">{secondaryText}</p>}
    </div>
  );
}

function getExtractionBadgeLabel(status?: ReceiptExtractionStatus | null): string {
  switch (status) {
    case "PROCESSING":
      return "Extracting";
    case "COMPLETED":
      return "Ready for review";
    case "FAILED":
      return "Extraction failed";
    case "NOT_STARTED":
    default:
      return "Not extracted";
  }
}

function getExtractionBadgeClasses(status?: ReceiptExtractionStatus | null): string {
  switch (status) {
    case "PROCESSING":
      return "bg-amber-50 text-amber-700";
    case "COMPLETED":
      return "bg-emerald-50 text-emerald-700";
    case "FAILED":
      return "bg-red-50 text-red-700";
    case "NOT_STARTED":
    default:
      return "bg-slate-100 text-slate-600";
  }
}

function hasReviewWarnings(extraction: ReceiptExtractionResultDto): boolean {
  const amountNeedsReview =
    extraction.amountConfidence === "LOW" || extraction.amountConfidence === "MEDIUM";
  const hasUserFacingWarnings = filterUiExtractionWarnings(extraction.extractionWarnings).length > 0;
  return amountNeedsReview || hasUserFacingWarnings;
}

function FilterButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
        active
          ? "border-sky-300 bg-sky-50 text-sky-700"
          : "border-slate-300 bg-white text-slate-600 hover:bg-slate-50"
      }`}
    >
      {children}
    </button>
  );
}

function ReceiptActionButton({
  children,
  onClick,
  disabled = false,
}: {
  children: ReactNode;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="px-3 py-1.5 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-60"
    >
      {children}
    </button>
  );
}
