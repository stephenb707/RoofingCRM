"use client";

import { useParams } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getPublicEstimate, decidePublicEstimate } from "@/lib/estimatesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { formatDate, formatMoney } from "@/lib/format";
import {
  ESTIMATE_STATUS_LABELS,
  ESTIMATE_STATUS_COLORS,
} from "@/lib/estimatesConstants";
import type {
  PublicEstimateDto,
  PublicEstimateDecisionRequest,
  EstimateStatus,
} from "@/lib/types";
import { useState } from "react";

function lineTotal(item: { quantity: number; unitPrice: number }): number {
  return Number(item.quantity) * Number(item.unitPrice);
}

export default function PublicEstimatePage() {
  const params = useParams();
  const token = params.token as string;
  const queryClient = useQueryClient();
  const [signerName, setSignerName] = useState("");
  const [signerEmail, setSignerEmail] = useState("");

  const {
    data: estimate,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ["publicEstimate", token],
    queryFn: () => getPublicEstimate(token),
    enabled: !!token,
    retry: false,
  });

  const decideMutation = useMutation({
    mutationFn: (decision: EstimateStatus) =>
      decidePublicEstimate(token, {
        decision,
        signerName: signerName.trim(),
        signerEmail: signerEmail.trim() || undefined,
      } as PublicEstimateDecisionRequest),
    onSuccess: (data) => {
      queryClient.setQueryData(["publicEstimate", token], data);
    },
  });

  const canDecide =
    estimate &&
    (estimate.status === "DRAFT" || estimate.status === "SENT") &&
    signerName.trim().length > 0;

  const handleAccept = () => {
    if (!canDecide) return;
    decideMutation.mutate("ACCEPTED");
  };

  const handleReject = () => {
    if (!canDecide) return;
    decideMutation.mutate("REJECTED");
  };

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
        <p className="ml-4 text-slate-600">Loading estimate…</p>
      </div>
    );
  }

  if (isError || !estimate) {
    const msg = getApiErrorMessage(error, "Failed to load estimate");
    const isExpired = msg.toLowerCase().includes("expired") || (error as { response?: { status?: number } })?.response?.status === 410;
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
        <div className="bg-white rounded-xl border border-slate-200 p-8 max-w-md text-center">
          <h1 className="text-xl font-semibold text-slate-800 mb-2">
            {isExpired ? "Link expired" : "Estimate not found"}
          </h1>
          <p className="text-slate-600">
            {isExpired
              ? "This estimate link has expired. Please contact your contractor for a new link."
              : msg}
          </p>
        </div>
      </div>
    );
  }

  const status = estimate.status as EstimateStatus;
  const showDecisionForm =
    (status === "DRAFT" || status === "SENT") && !decideMutation.isSuccess;

  return (
    <div className="min-h-screen bg-slate-50 py-8 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
          <div className="p-6 border-b border-slate-200">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="text-2xl font-bold text-slate-800">
                  {estimate.title || "Estimate"}
                </h1>
                <p className="text-sm text-slate-500 mt-1">
                  Estimate #{estimate.estimateNumber}
                </p>
                {estimate.customerName && (
                  <p className="text-sm text-slate-700 mt-2">{estimate.customerName}</p>
                )}
                {estimate.customerAddress && (
                  <p className="text-sm text-slate-600">{estimate.customerAddress}</p>
                )}
              </div>
              <span
                className={`inline-flex px-3 py-1 rounded-full text-sm font-medium ${ESTIMATE_STATUS_COLORS[status]}`}
              >
                {ESTIMATE_STATUS_LABELS[status]}
              </span>
            </div>
          </div>

          <div className="p-6">
            {(estimate.items ?? []).length > 0 && (
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
                  {estimate.items.map((it, idx) => (
                    <tr key={idx} className="hover:bg-slate-50">
                      <td className="px-4 py-3 text-sm">
                        <div className="font-medium text-slate-800">{it.name}</div>
                        {it.description && (
                          <div className="text-xs text-slate-500">{it.description}</div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-sm text-right text-slate-600">{it.quantity}</td>
                      <td className="px-4 py-3 text-sm text-right text-slate-600">{formatMoney(it.unitPrice)}</td>
                      <td className="px-4 py-3 text-sm text-right font-medium">
                        {formatMoney(it.lineTotal ?? lineTotal(it))}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            <div className="flex justify-end gap-4 mb-6">
              {estimate.subtotal != null && (
                <div className="text-sm text-slate-600">Subtotal: {formatMoney(estimate.subtotal)}</div>
              )}
              {estimate.total != null && (
                <div className="text-lg font-semibold text-slate-800">Total: {formatMoney(estimate.total)}</div>
              )}
            </div>

            {estimate.notes && (
              <div className="mb-6 p-4 bg-slate-50 rounded-lg">
                <p className="text-sm text-slate-700 whitespace-pre-wrap">{estimate.notes}</p>
              </div>
            )}

            {showDecisionForm && (
              <div className="pt-6 border-t border-slate-200 space-y-4">
                <h3 className="text-sm font-semibold text-slate-800">Your decision</h3>
                <div>
                  <label htmlFor="signer-name" className="block text-xs font-medium text-slate-600 mb-1">
                    Your name (required)
                  </label>
                  <input
                    id="signer-name"
                    type="text"
                    value={signerName}
                    onChange={(e) => setSignerName(e.target.value)}
                    placeholder="John Smith"
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                </div>
                <div>
                  <label htmlFor="signer-email" className="block text-xs font-medium text-slate-600 mb-1">
                    Your email (optional)
                  </label>
                  <input
                    id="signer-email"
                    type="email"
                    value={signerEmail}
                    onChange={(e) => setSignerEmail(e.target.value)}
                    placeholder="john@example.com"
                    className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                </div>
                {decideMutation.isError && (
                  <p className="text-sm text-red-600">
                    {getApiErrorMessage(decideMutation.error, "Failed to submit decision")}
                  </p>
                )}
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={handleAccept}
                    disabled={!canDecide || decideMutation.isPending}
                    className="px-6 py-2.5 bg-green-600 text-white text-sm font-medium rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Accept
                  </button>
                  <button
                    type="button"
                    onClick={handleReject}
                    disabled={!canDecide || decideMutation.isPending}
                    className="px-6 py-2.5 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Reject
                  </button>
                </div>
              </div>
            )}

            {decideMutation.isSuccess && (
              <div className="pt-6 border-t border-slate-200">
                <p className="text-green-700 font-medium">
                  Thank you. Your decision has been recorded.
                </p>
                <p className="text-sm text-slate-600 mt-1">
                  Status: {ESTIMATE_STATUS_LABELS[estimate.status as EstimateStatus]}
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
