"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getInvoice, updateInvoiceStatus } from "@/lib/invoicesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  INVOICE_STATUS_LABELS,
  INVOICE_STATUS_COLORS,
} from "@/lib/invoiceConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatDateTime, formatMoney } from "@/lib/format";
import { StatusBadge } from "@/components/StatusBadge";
import type { InvoiceStatus } from "@/lib/types";

function getNextStatus(current: InvoiceStatus): InvoiceStatus | null {
  if (current === "DRAFT") return "SENT";
  if (current === "SENT") return "PAID";
  return null;
}

export default function InvoiceDetailPage() {
  const params = useParams();
  const invoiceId = params.invoiceId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const canEdit =
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "OWNER" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "ADMIN" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "SALES";

  const { data: invoice, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.invoice(auth.selectedTenantId, invoiceId),
    queryFn: () => getInvoice(api, invoiceId),
    enabled: ready && !!invoiceId,
  });

  const updateStatusMutation = useMutation({
    mutationFn: (status: InvoiceStatus) => updateInvoiceStatus(api, invoiceId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.invoice(auth.selectedTenantId, invoiceId) });
      if (invoice?.jobId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.invoicesForJob(auth.selectedTenantId, invoice.jobId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.activityForEntity(auth.selectedTenantId, "JOB", invoice.jobId) });
      }
    },
  });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto" />
        <p className="text-sm text-slate-500 mt-4 text-center">Loading invoice…</p>
      </div>
    );
  }

  if (isError || !invoice) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load invoice</h3>
          <p className="text-sm text-red-600 mt-1">
            {getApiErrorMessage(error, "The invoice could not be found.")}
          </p>
        </div>
      </div>
    );
  }

  const status = invoice.status as InvoiceStatus;
  const isTerminal = status === "PAID" || status === "VOID";

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Link
          href={invoice.jobId ? `/app/jobs/${invoice.jobId}` : "/app/jobs"}
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Job
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">
              Invoice {invoice.invoiceNumber}
            </h1>
            {invoice.jobId && (
              <p className="text-sm text-slate-500 mt-1">
                Job: <Link href={`/app/jobs/${invoice.jobId}`} className="text-sky-600 hover:underline">View job</Link>
              </p>
            )}
          </div>
          <StatusBadge
            label={INVOICE_STATUS_LABELS[status]}
            className={INVOICE_STATUS_COLORS[status]}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Overview</h2>
            <dl className="grid grid-cols-2 gap-4">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Status</dt>
                <dd className="mt-1">
                  <StatusBadge
                    label={INVOICE_STATUS_LABELS[status]}
                    className={INVOICE_STATUS_COLORS[status]}
                  />
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Total</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatMoney(invoice.total)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Issued</dt>
                <dd className="mt-1 text-sm text-slate-800">{invoice.issuedAt ? formatDateTime(invoice.issuedAt) : "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Due</dt>
                <dd className="mt-1 text-sm text-slate-800">{invoice.dueAt ? formatDateTime(invoice.dueAt) : "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Sent</dt>
                <dd className="mt-1 text-sm text-slate-800">{invoice.sentAt ? formatDateTime(invoice.sentAt) : "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Paid</dt>
                <dd className="mt-1 text-sm text-slate-800">{invoice.paidAt ? formatDateTime(invoice.paidAt) : "—"}</dd>
              </div>
            </dl>
            {invoice.notes && (
              <div className="mt-4 pt-4 border-t border-slate-100">
                <dt className="text-xs font-medium text-slate-500 uppercase">Notes</dt>
                <dd className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">{invoice.notes}</dd>
              </div>
            )}
          </div>

          {(invoice.items ?? []).length > 0 && (
            <div className="bg-white rounded-xl border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Line items</h2>
              <table className="min-w-full divide-y divide-slate-200">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Description</th>
                    <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Qty</th>
                    <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Unit price</th>
                    <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {(invoice.items ?? []).map((it) => (
                    <tr key={it.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 text-sm">
                        <div className="font-medium text-slate-800">{it.name}</div>
                        {it.description && (
                          <div className="text-xs text-slate-500">{it.description}</div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-slate-600">{it.quantity}</td>
                      <td className="px-4 py-3 text-sm text-right text-slate-600">{formatMoney(it.unitPrice)}</td>
                      <td className="px-4 py-3 text-sm text-right font-medium">{formatMoney(it.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-4 flex justify-end">
                <span className="text-lg font-semibold text-slate-800">Total: {formatMoney(invoice.total)}</span>
              </div>
            </div>
          )}
        </div>

        <div className="space-y-6">
          {canEdit && !isTerminal && (
            <div className="bg-white rounded-xl border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">Update status</h2>
              <div className="space-y-2">
                {getNextStatus(status) && (
                  <button
                    type="button"
                    onClick={() => updateStatusMutation.mutate(getNextStatus(status)!)}
                    disabled={updateStatusMutation.isPending}
                    className="w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200"
                  >
                    Mark {getNextStatus(status)!.toLowerCase()}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => updateStatusMutation.mutate("VOID")}
                  disabled={updateStatusMutation.isPending}
                  className="w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium bg-red-50 text-red-700 hover:bg-red-100 border border-red-200"
                >
                  Void
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
