"use client";

import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { getPublicInvoice } from "@/lib/invoicesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { formatDateTime, formatMoney } from "@/lib/format";
import {
  INVOICE_STATUS_COLORS,
  INVOICE_STATUS_LABELS,
} from "@/lib/invoiceConstants";
import type { InvoiceStatus } from "@/lib/types";

export default function PublicInvoicePage() {
  const params = useParams();
  const token = params.token as string;

  const {
    data: invoice,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["publicInvoice", token],
    queryFn: () => getPublicInvoice(token),
    enabled: !!token,
    retry: false,
  });

  if (!token) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
        <p className="text-slate-600">Invalid link</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600" />
        <p className="ml-4 text-slate-600">Loading invoice…</p>
      </div>
    );
  }

  if (isError || !invoice) {
    const msg = getApiErrorMessage(error, "Failed to load invoice");
    const status = (error as { response?: { status?: number } })?.response?.status;
    const isExpired = msg.toLowerCase().includes("expired") || status === 410;
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
        <div className="bg-white rounded-xl border border-slate-200 p-8 max-w-md text-center">
          <h1 className="text-xl font-semibold text-slate-800 mb-2">
            {isExpired ? "Link expired" : "Invoice not found"}
          </h1>
          <p className="text-slate-600">
            {isExpired
              ? "This invoice link has expired. Please contact your contractor for a new link."
              : msg}
          </p>
        </div>
      </div>
    );
  }

  const status = invoice.status as InvoiceStatus;
  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-6 border-b border-slate-200">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold text-slate-800">
                  Invoice #{invoice.invoiceNumber}
                </h1>
                {invoice.customerName && (
                  <p className="text-sm text-slate-700 mt-2">{invoice.customerName}</p>
                )}
                {invoice.customerAddress && (
                  <p className="text-sm text-slate-600">{invoice.customerAddress}</p>
                )}
              </div>
              <span
                className={`inline-flex px-3 py-1 rounded-full text-sm font-medium ${INVOICE_STATUS_COLORS[status]}`}
              >
                {INVOICE_STATUS_LABELS[status]}
              </span>
            </div>
          </div>

          <div className="p-6">
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Issued</dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {invoice.issuedAt ? formatDateTime(invoice.issuedAt) : "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Due</dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {invoice.dueAt ? formatDateTime(invoice.dueAt) : "—"}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Sent</dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {invoice.sentAt ? formatDateTime(invoice.sentAt) : "—"}
                </dd>
              </div>
            </div>

            {(invoice.items ?? []).length > 0 && (
              <table className="min-w-full divide-y divide-slate-200 mb-6">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Description</th>
                    <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Qty</th>
                    <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Unit price</th>
                    <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {invoice.items.map((it, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="px-4 py-3 text-sm">
                        <div className="font-medium text-slate-800">{it.name}</div>
                        {it.description && (
                          <div className="text-xs text-slate-500">{it.description}</div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-slate-600">{it.quantity}</td>
                      <td className="px-4 py-3 text-sm text-right text-slate-600">
                        {formatMoney(it.unitPrice)}
                      </td>
                      <td className="px-4 py-3 text-sm text-right font-medium">
                        {formatMoney(it.lineTotal)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            {invoice.notes && (
              <div className="mb-6 p-4 bg-slate-50 rounded-lg">
                <p className="text-sm text-slate-700 whitespace-pre-wrap">{invoice.notes}</p>
              </div>
            )}

            <div className="flex justify-end">
              <div className="text-lg font-semibold text-slate-800">
                Total: {formatMoney(invoice.total)}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
