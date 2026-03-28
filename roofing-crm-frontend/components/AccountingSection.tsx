"use client";

import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import {
  createJobCostEntry,
  deleteJobCostEntry,
  getJobAccountingSummary,
  listJobCostEntries,
  updateJobCostEntry,
} from "@/lib/accountingApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { JOB_COST_CATEGORIES, JOB_COST_CATEGORY_LABELS } from "@/lib/accountingConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatDate, formatMoney, formatDateOnly } from "@/lib/format";
import { DatePickerField } from "@/components/DatePickerField";
import type {
  CreateJobCostEntryRequest,
  JobCostCategory,
  JobCostEntryDto,
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

export function AccountingSection({ jobId }: AccountingSectionProps) {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<CostFilter>("ALL");
  const [showModal, setShowModal] = useState(false);
  const [editingEntry, setEditingEntry] = useState<JobCostEntryDto | null>(null);
  const [formState, setFormState] = useState<CostFormState>(getDefaultFormState());
  const [formError, setFormError] = useState<string | null>(null);

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

  const invalidateAccountingQueries = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.jobAccountingSummary(auth.selectedTenantId, jobId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.jobCostEntries(auth.selectedTenantId, jobId) }),
      queryClient.invalidateQueries({ queryKey: queryKeys.activityForEntity(auth.selectedTenantId, "JOB", jobId) }),
    ]);
  };

  const createMutation = useMutation({
    mutationFn: (payload: CreateJobCostEntryRequest) => createJobCostEntry(api, jobId, payload),
    onSuccess: async () => {
      setFormError(null);
      setShowModal(false);
      setEditingEntry(null);
      setFormState(getDefaultFormState());
      await invalidateAccountingQueries();
    },
    onError: (error: unknown) => {
      setFormError(getApiErrorMessage(error, "Failed to save cost entry."));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ costEntryId, payload }: { costEntryId: string; payload: UpdateJobCostEntryRequest }) =>
      updateJobCostEntry(api, jobId, costEntryId, payload),
    onSuccess: async () => {
      setFormError(null);
      setShowModal(false);
      setEditingEntry(null);
      setFormState(getDefaultFormState());
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

  const entries = costsQuery.data ?? [];
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

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const isLoading = summaryQuery.isLoading || costsQuery.isLoading;
  const isError = summaryQuery.isError || costsQuery.isError;

  const openCreateModal = () => {
    setEditingEntry(null);
    setFormError(null);
    setFormState(getDefaultFormState());
    setShowModal(true);
  };

  const openEditModal = (entry: JobCostEntryDto) => {
    setEditingEntry(entry);
    setFormError(null);
    setFormState({
      category: entry.category,
      vendorName: entry.vendorName ?? "",
      description: entry.description,
      amount: String(entry.amount),
      incurredAt: entry.incurredAt.slice(0, 10),
      notes: entry.notes ?? "",
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingEntry(null);
    setFormError(null);
    setFormState(getDefaultFormState());
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

    const payload = {
      category: formState.category,
      vendorName: formState.vendorName.trim() || null,
      description: formState.description.trim(),
      amount,
      incurredAt: toApiDate(formState.incurredAt),
      notes: formState.notes.trim() || null,
    };

    if (editingEntry) {
      updateMutation.mutate({ costEntryId: editingEntry.id, payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const handleDelete = (entry: JobCostEntryDto) => {
    if (typeof window !== "undefined" && !window.confirm("Delete this cost entry?")) {
      return;
    }
    deleteMutation.mutate(entry.id);
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
          {getApiErrorMessage(summaryQuery.error ?? costsQuery.error, "Failed to load accounting details.")}
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6" data-testid="accounting-section">
      <div className="flex items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-lg font-semibold text-slate-800">Accounting</h2>
          <p className="text-sm text-slate-500 mt-1">Track job revenue, costs, and profitability.</p>
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
            summary?.projectedProfit != null
              ? `Projected ${formatMoney(summary.projectedProfit)}`
              : undefined
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

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-6">
        {JOB_COST_CATEGORIES.map((category) => (
          <div key={category} className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-medium uppercase tracking-wider text-slate-500">
              {JOB_COST_CATEGORY_LABELS[category]}
            </p>
            <p className="mt-1 text-sm font-semibold text-slate-800">{formatMoney(categoryTotals[category])}</p>
          </div>
        ))}
      </div>

      <div className="flex flex-wrap gap-2 mb-4">
        <FilterButton active={filter === "ALL"} onClick={() => setFilter("ALL")}>
          All
        </FilterButton>
        {JOB_COST_CATEGORIES.map((category) => (
          <FilterButton key={category} active={filter === category} onClick={() => setFilter(category)}>
            {JOB_COST_CATEGORY_LABELS[category]}
          </FilterButton>
        ))}
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
                <span className="text-xs font-medium text-slate-500">
                  {formatMoney(categoryTotals[group.category])}
                </span>
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

      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6">
            <h3 className="text-lg font-semibold text-slate-800 mb-4">
              {editingEntry ? "Edit Cost" : "Add Cost"}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
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
                <div>
                  <DatePickerField
                    id="cost-incurred-at"
                    label="Incurred date"
                    value={formState.incurredAt}
                    onChange={(value) =>
                      setFormState((current) => ({
                        ...current,
                        incurredAt: value,
                      }))
                    }
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="cost-vendor" className="block text-sm font-medium text-slate-700 mb-1">
                    Vendor name
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
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label htmlFor="cost-amount" className="block text-sm font-medium text-slate-700 mb-1">
                    Amount
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
                  onClick={closeModal}
                  className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 text-sm font-medium text-white bg-sky-600 rounded-lg hover:bg-sky-700 disabled:opacity-50"
                >
                  {isSaving ? "Saving…" : editingEntry ? "Save Changes" : "Add Cost"}
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

function FilterButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
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
