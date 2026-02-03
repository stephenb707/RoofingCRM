"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import {
  listInvoicesForJob,
  createInvoiceFromEstimate,
  updateInvoiceStatus,
} from "@/lib/invoicesApi";
import { listEstimatesForJob } from "@/lib/estimatesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  INVOICE_STATUS_LABELS,
  INVOICE_STATUS_COLORS,
} from "@/lib/invoiceConstants";
import { ESTIMATE_STATUS_LABELS } from "@/lib/estimatesConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatDateTime, formatMoney } from "@/lib/format";
import { StatusBadge } from "@/components/StatusBadge";
import type { InvoiceDto, InvoiceStatus, EstimateDto } from "@/lib/types";

export interface InvoicesSectionProps {
  jobId: string;
}

function getNextStatus(current: InvoiceStatus): InvoiceStatus | null {
  if (current === "DRAFT") return "SENT";
  if (current === "SENT") return "PAID";
  return null;
}

export function InvoicesSection({ jobId }: InvoicesSectionProps) {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createEstimateId, setCreateEstimateId] = useState("");
  const [createDueAt, setCreateDueAt] = useState("");
  const [createNotes, setCreateNotes] = useState("");

  const canEdit =
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "OWNER" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "ADMIN" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "SALES";

  const invoicesQuery = useQuery({
    queryKey: queryKeys.invoicesForJob(auth.selectedTenantId, jobId),
    queryFn: () => listInvoicesForJob(api, jobId),
    enabled: ready && !!jobId,
  });

  const estimatesQuery = useQuery({
    queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, jobId),
    queryFn: () => listEstimatesForJob(api, jobId),
    enabled: ready && !!jobId && showCreateModal,
  });

  const acceptedEstimates = (estimatesQuery.data ?? []).filter(
    (e: EstimateDto) => e.status === "ACCEPTED"
  );

  const createMutation = useMutation({
    mutationFn: () =>
      createInvoiceFromEstimate(api, {
        estimateId: createEstimateId,
        dueAt: createDueAt ? `${createDueAt}T12:00:00Z` : undefined,
        notes: createNotes || undefined,
      }),
    onSuccess: () => {
      setShowCreateModal(false);
      setCreateEstimateId("");
      setCreateDueAt("");
      setCreateNotes("");
      queryClient.invalidateQueries({ queryKey: queryKeys.invoicesForJob(auth.selectedTenantId, jobId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.activityForEntity(auth.selectedTenantId, "JOB", jobId) });
    },
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ invoiceId, status }: { invoiceId: string; status: InvoiceStatus }) =>
      updateInvoiceStatus(api, invoiceId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.invoicesForJob(auth.selectedTenantId, jobId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.activityForEntity(auth.selectedTenantId, "JOB", jobId) });
    },
  });

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!createEstimateId) return;
    createMutation.mutate();
  };

  if (invoicesQuery.isLoading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Invoices</h2>
        <p className="text-sm text-slate-500">Loading invoices…</p>
      </div>
    );
  }

  if (invoicesQuery.isError) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Invoices</h2>
        <p className="text-sm text-red-600">
          {getApiErrorMessage(invoicesQuery.error, "Failed to load invoices")}
        </p>
      </div>
    );
  }

  const invoices = invoicesQuery.data ?? [];

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-slate-800">Invoices</h2>
        {canEdit && (
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="px-3 py-1.5 text-sm font-medium text-sky-600 border border-sky-300 rounded-lg hover:bg-sky-50"
          >
            Create Invoice
          </button>
        )}
      </div>

      {invoices.length === 0 ? (
        <p className="text-sm text-slate-500">
          No invoices yet. Create an invoice from an accepted estimate.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-slate-200">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Invoice #</th>
                <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Status</th>
                <th className="px-4 py-2 text-right text-xs font-semibold text-slate-600">Total</th>
                <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Due</th>
                <th className="px-4 py-2 text-left text-xs font-semibold text-slate-600">Sent / Paid</th>
                <th className="px-4 py-2 text-right text-xs font-semibold text-slate-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {invoices.map((inv: InvoiceDto) => (
                <tr key={inv.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 text-sm">
                    <Link
                      href={`/app/invoices/${inv.id}`}
                      className="font-medium text-sky-600 hover:text-sky-700"
                    >
                      {inv.invoiceNumber}
                    </Link>
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge
                      label={INVOICE_STATUS_LABELS[inv.status]}
                      className={INVOICE_STATUS_COLORS[inv.status]}
                    />
                  </td>
                  <td className="px-4 py-3 text-sm text-right text-slate-800">
                    {formatMoney(inv.total)}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600">
                    {inv.dueAt ? formatDateTime(inv.dueAt) : "—"}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-600">
                    {inv.sentAt ? `Sent ${formatDateTime(inv.sentAt)}` : "—"}
                    {inv.paidAt ? ` • Paid ${formatDateTime(inv.paidAt)}` : ""}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {canEdit && inv.status !== "PAID" && inv.status !== "VOID" && (
                      <span className="inline-flex gap-2">
                        {getNextStatus(inv.status) && (
                          <button
                            type="button"
                            onClick={() =>
                              updateStatusMutation.mutate({
                                invoiceId: inv.id,
                                status: getNextStatus(inv.status)!,
                              })
                            }
                            disabled={updateStatusMutation.isPending}
                            className="text-sm font-medium text-sky-600 hover:text-sky-700 disabled:opacity-60"
                          >
                            Mark {getNextStatus(inv.status)!.toLowerCase()}
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => updateStatusMutation.mutate({ invoiceId: inv.id, status: "VOID" })}
                          disabled={updateStatusMutation.isPending}
                          className="text-sm font-medium text-red-600 hover:text-red-700 disabled:opacity-60"
                        >
                          Void
                        </button>
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6">
            <h3 className="text-lg font-semibold text-slate-800 mb-4">Create Invoice</h3>
            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div>
                <label htmlFor="create-estimate" className="block text-sm font-medium text-slate-700 mb-1">
                  Estimate (must be ACCEPTED)
                </label>
                <select
                  id="create-estimate"
                  value={createEstimateId}
                  onChange={(e) => setCreateEstimateId(e.target.value)}
                  required
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                >
                  <option value="">Select estimate…</option>
                  {acceptedEstimates.map((est: EstimateDto) => (
                    <option key={est.id} value={est.id}>
                      {est.title || `Estimate ${est.id.slice(0, 8)}`} — {ESTIMATE_STATUS_LABELS[est.status]}
                    </option>
                  ))}
                  {acceptedEstimates.length === 0 && estimatesQuery.data && (
                    <option value="" disabled>
                      No ACCEPTED estimates
                    </option>
                  )}
                </select>
              </div>
              <div>
                <label htmlFor="create-due" className="block text-sm font-medium text-slate-700 mb-1">
                  Due date (optional)
                </label>
                <input
                  id="create-due"
                  type="date"
                  value={createDueAt}
                  onChange={(e) => setCreateDueAt(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label htmlFor="create-notes" className="block text-sm font-medium text-slate-700 mb-1">
                  Notes (optional)
                </label>
                <textarea
                  id="create-notes"
                  value={createNotes}
                  onChange={(e) => setCreateNotes(e.target.value)}
                  rows={2}
                  className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
              </div>
              {createMutation.isError && (
                <p className="text-sm text-red-600">
                  {getApiErrorMessage(createMutation.error, "Failed to create invoice")}
                </p>
              )}
              <div className="flex gap-3 justify-end pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={!createEstimateId || createMutation.isPending}
                  className="px-4 py-2 text-sm font-medium text-white bg-sky-600 rounded-lg hover:bg-sky-700 disabled:opacity-50"
                >
                  {createMutation.isPending ? "Creating…" : "Create"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
