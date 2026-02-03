"use client";

import { useState, useEffect, useRef } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getEstimate, updateEstimate, updateEstimateStatus, shareEstimate } from "@/lib/estimatesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import {
  ESTIMATE_STATUSES,
  ESTIMATE_STATUS_LABELS,
  ESTIMATE_STATUS_COLORS,
} from "@/lib/estimatesConstants";
import type { EstimateDto, EstimateItemDto, EstimateItemRequest, EstimateStatus } from "@/lib/types";
import { formatDate, formatMoney } from "@/lib/format";
import { StatusBadge } from "@/components/StatusBadge";

function toItemRequest(it: EstimateItemDto | EstimateItemRequest): EstimateItemRequest {
  return {
    name: it.name,
    description: it.description ?? null,
    quantity: it.quantity,
    unitPrice: it.unitPrice,
    unit: it.unit ?? null,
  };
}

function lineTotal(it: { quantity: number; unitPrice: number }): number {
  return Number(it.quantity) * Number(it.unitPrice);
}

export default function EstimateDetailPage() {
  const params = useParams();
  const estimateId = params.estimateId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [selectedStatus, setSelectedStatus] = useState<EstimateStatus | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);
  const [itemsError, setItemsError] = useState<string | null>(null);
  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [addForm, setAddForm] = useState<EstimateItemRequest>({
    name: "",
    description: null,
    quantity: 1,
    unitPrice: 0,
    unit: "ea",
  });
  const [editForm, setEditForm] = useState<EstimateItemRequest | null>(null);
  const [shareLink, setShareLink] = useState<string | null>(null);
  const [shareExpiresAt, setShareExpiresAt] = useState<string | null>(null);
  const [copyState, setCopyState] = useState<"idle" | "copied" | "error">("idle");
  const copyTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const canShare =
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "OWNER" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "ADMIN" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "SALES";

  const shareMutation = useMutation({
    mutationFn: () => shareEstimate(api, estimateId, { expiresInDays: 14 }),
    onSuccess: (data) => {
      const url = typeof window !== "undefined"
        ? `${window.location.origin}/estimate/${data.token}`
        : "";
      setShareLink(url);
      setShareExpiresAt(data.expiresAt ?? null);
      if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
        navigator.clipboard.writeText(url).catch(() => {});
      }
    },
  });

  const { data: estimate, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId),
    queryFn: () => getEstimate(api, estimateId),
    enabled: ready && !!estimateId,
  });

  useEffect(() => {
    if (estimate) {
      setSelectedStatus(estimate.status as EstimateStatus);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- sync only when server status changes
  }, [estimate?.status]);

  useEffect(() => {
    return () => {
      if (copyTimerRef.current) clearTimeout(copyTimerRef.current);
    };
  }, []);

  const handleCopyLink = async () => {
    if (!shareLink) return;
    if (copyTimerRef.current) {
      clearTimeout(copyTimerRef.current);
      copyTimerRef.current = null;
    }

    const copyToClipboard = (): Promise<void> => {
      if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
        return navigator.clipboard.writeText(shareLink);
      }
      // Fallback for older browsers
      return new Promise((resolve, reject) => {
        const ta = document.createElement("textarea");
        ta.value = shareLink;
        ta.style.position = "fixed";
        ta.style.opacity = "0";
        document.body.appendChild(ta);
        ta.select();
        try {
          document.execCommand("copy");
          resolve();
        } catch (e) {
          reject(e);
        } finally {
          document.body.removeChild(ta);
        }
      });
    };

    try {
      await copyToClipboard();
      setCopyState("copied");
      copyTimerRef.current = setTimeout(() => {
        copyTimerRef.current = null;
        setCopyState("idle");
      }, 1500);
    } catch {
      setCopyState("error");
      copyTimerRef.current = setTimeout(() => {
        copyTimerRef.current = null;
        setCopyState("idle");
      }, 2000);
    }
  };

  const statusMutation = useMutation({
    mutationFn: (status: EstimateStatus) => updateEstimateStatus(api, estimateId, status),
    onSuccess: () => {
      setStatusError(null);
      queryClient.invalidateQueries({ queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId) });
      if (estimate?.jobId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, estimate.jobId) });
      }
    },
    onError: (err) => {
      console.error("Update estimate status failed:", err);
      setStatusError(getApiErrorMessage(err, "Failed to update status."));
      if (estimate) setSelectedStatus(estimate.status as EstimateStatus);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (payload: { items: EstimateItemRequest[] }) => updateEstimate(api, estimateId, { items: payload.items }),
    onSuccess: () => {
      setItemsError(null);
      setEditingItemId(null);
      setEditForm(null);
      setAddForm({ name: "", description: null, quantity: 1, unitPrice: 0, unit: "ea" });
      queryClient.invalidateQueries({ queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId) });
      if (estimate?.jobId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, estimate.jobId) });
      }
    },
    onError: (err) => {
      console.error("Update estimate items failed:", err);
      setItemsError(getApiErrorMessage(err, "Failed to update items."));
    },
  });

  const handleStatusChange = (s: EstimateStatus) => {
    if (s === estimate?.status) return;
    setSelectedStatus(s);
    statusMutation.mutate(s);
  };

  const startEdit = (item: EstimateItemDto) => {
    setEditingItemId(item.id);
    setEditForm(toItemRequest(item));
  };

  const saveEdit = () => {
    if (!estimate || !editForm || !editingItemId) return;
    const list = estimate.items ?? [];
    const items: EstimateItemRequest[] = list.map((it) =>
      it.id === editingItemId ? editForm : toItemRequest(it)
    );
    updateMutation.mutate({ items });
  };

  const deleteItem = (id: string) => {
    if (!estimate) return;
    const list = estimate.items ?? [];
    const items = list.filter((i) => i.id !== id).map(toItemRequest);
    if (items.length === 0) {
      setItemsError("At least one item is required.");
      return;
    }
    updateMutation.mutate({ items });
  };

  const addItem = () => {
    if (!estimate) return;
    const newItem: EstimateItemRequest = {
      name: addForm.name.trim() || "Item",
      description: addForm.description || null,
      quantity: Number(addForm.quantity) || 0,
      unitPrice: Number(addForm.unitPrice) || 0,
      unit: addForm.unit || null,
    };
    const list = estimate.items ?? [];
    const items = [...list.map(toItemRequest), newItem];
    updateMutation.mutate({ items });
  };

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto" />
        <p className="text-sm text-slate-500 mt-4 text-center">Loading estimate…</p>
      </div>
    );
  }

  if (isError || !estimate) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load estimate</h3>
          <p className="text-sm text-red-600 mt-1">
            {getApiErrorMessage(error, "The estimate could not be found.")}
          </p>
        </div>
      </div>
    );
  }

  const jobId = estimate.jobId;

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Link
          href={`/app/jobs/${jobId}/estimates`}
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Estimates
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">
              {estimate.title || `Estimate`}
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Job: <Link href={`/app/jobs/${jobId}`} className="text-sky-600 hover:underline">View job</Link>
            </p>
          </div>
          <StatusBadge
            label={ESTIMATE_STATUS_LABELS[estimate.status as EstimateStatus]}
            className={ESTIMATE_STATUS_COLORS[estimate.status as EstimateStatus]}
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
                    label={ESTIMATE_STATUS_LABELS[estimate.status as EstimateStatus]}
                    className={ESTIMATE_STATUS_COLORS[estimate.status as EstimateStatus]}
                  />
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Subtotal</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatMoney(estimate.subtotal)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Total</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatMoney(estimate.total)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Created</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDate(estimate.createdAt)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase">Updated</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDate(estimate.updatedAt)}</dd>
              </div>
            </dl>
            {estimate.notes && (
              <div className="mt-4 pt-4 border-t border-slate-100">
                <dt className="text-xs font-medium text-slate-500 uppercase">Notes</dt>
                <dd className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">{estimate.notes}</dd>
              </div>
            )}
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Line items</h2>
            {itemsError && (
              <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {itemsError}
              </div>
            )}

            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Description</th>
                  <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Qty</th>
                  <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Unit price</th>
                  <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Unit</th>
                  <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Total</th>
                  <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {(estimate.items ?? []).map((it) => (
                  <tr key={it.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 text-sm">
                      <div className="font-medium text-slate-800">{it.name}</div>
                      {it.description && (
                        <div className="text-xs text-slate-500">{it.description}</div>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-right text-slate-600">{it.quantity}</td>
                    <td className="px-4 py-3 text-sm text-right text-slate-600">{formatMoney(it.unitPrice)}</td>
                    <td className="px-4 py-3 text-sm text-slate-600">{it.unit ?? "—"}</td>
                    <td className="px-4 py-3 text-sm text-right font-medium">{formatMoney(lineTotal(it))}</td>
                    <td className="px-4 py-3 text-right">
                      <button
                        type="button"
                        onClick={() => startEdit(it)}
                        className="text-sky-600 hover:text-sky-700 text-sm font-medium mr-2"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => deleteItem(it.id)}
                        disabled={(estimate.items ?? []).length <= 1}
                        className="text-red-600 hover:text-red-700 text-sm font-medium disabled:opacity-50"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            {editingItemId && editForm && (
              <div className="mt-6 p-4 border border-sky-200 rounded-lg bg-sky-50/50">
                <h3 className="text-sm font-medium text-slate-800 mb-3">Edit item</h3>
                <div className="grid grid-cols-2 gap-3 mb-3">
                  <input
                    value={editForm.name}
                    onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                    placeholder="Name"
                    className="col-span-2 border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                  <input
                    value={editForm.description ?? ""}
                    onChange={(e) => setEditForm({ ...editForm, description: e.target.value || null })}
                    placeholder="Description"
                    className="col-span-2 border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                  <input
                    type="number"
                    min={0}
                    step="any"
                    value={editForm.quantity}
                    onChange={(e) => setEditForm({ ...editForm, quantity: Number(e.target.value) || 0 })}
                    className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    value={editForm.unitPrice}
                    onChange={(e) => setEditForm({ ...editForm, unitPrice: Number(e.target.value) || 0 })}
                    className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                  <input
                    value={editForm.unit ?? ""}
                    onChange={(e) => setEditForm({ ...editForm, unit: e.target.value || null })}
                    placeholder="Unit"
                    className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={saveEdit}
                    disabled={updateMutation.isPending}
                    className="px-3 py-1.5 bg-sky-600 text-white text-sm font-medium rounded-lg disabled:opacity-60"
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    onClick={() => { setEditingItemId(null); setEditForm(null); }}
                    className="px-3 py-1.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}

            <div className="mt-6 p-4 border border-slate-200 rounded-lg">
              <h3 className="text-sm font-medium text-slate-800 mb-3">Add item</h3>
              <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 mb-3">
                <input
                  value={addForm.name}
                  onChange={(e) => setAddForm({ ...addForm, name: e.target.value })}
                  placeholder="Name"
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
                <input
                  type="number"
                  min={0}
                  step="any"
                  value={addForm.quantity}
                  onChange={(e) => setAddForm({ ...addForm, quantity: Number(e.target.value) || 0 })}
                  placeholder="Qty"
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={addForm.unitPrice}
                  onChange={(e) => setAddForm({ ...addForm, unitPrice: Number(e.target.value) || 0 })}
                  placeholder="Unit price"
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
                <input
                  value={addForm.unit ?? ""}
                  onChange={(e) => setAddForm({ ...addForm, unit: e.target.value || null })}
                  placeholder="Unit"
                  className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
                />
                <button
                  type="button"
                  onClick={addItem}
                  disabled={!addForm.name.trim() || updateMutation.isPending}
                  className="px-3 py-2 bg-sky-600 text-white text-sm font-medium rounded-lg hover:bg-sky-700 disabled:opacity-50"
                >
                  Add
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Update status</h2>
            {statusError && (
              <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
                {statusError}
              </div>
            )}
            <div className="space-y-2">
              {ESTIMATE_STATUSES.map((s) => (
                <button
                  key={s}
                  onClick={() => handleStatusChange(s)}
                  disabled={statusMutation.isPending}
                  className={`w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium transition-colors border ${
                    selectedStatus === s ? ESTIMATE_STATUS_COLORS[s] : "bg-slate-50 text-slate-700 hover:bg-slate-100 border-transparent"
                  } ${statusMutation.isPending ? "opacity-60 cursor-not-allowed" : ""}`}
                >
                  {ESTIMATE_STATUS_LABELS[s]}
                </button>
              ))}
            </div>
          </div>

          {/* Share */}
          {canShare && (
            <div className="bg-white rounded-xl border border-slate-200 p-6">
              <h2 className="text-lg font-semibold text-slate-800 mb-4">
                Share
              </h2>
              <button
                type="button"
                onClick={() => shareMutation.mutate()}
                disabled={shareMutation.isPending}
                className="w-full px-4 py-2.5 text-sm font-medium text-sky-600 border border-sky-300 rounded-lg hover:bg-sky-50 disabled:opacity-60"
              >
                {shareMutation.isPending ? "Generating…" : shareLink ? "Refresh link" : "Generate link"}
              </button>
              {shareLink && (
                <div className="mt-4 space-y-2">
                  <div className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-center">
                    <input
                      readOnly
                      value={shareLink}
                      data-testid="share-link-input"
                      className="w-full min-w-0 truncate border border-slate-300 rounded-lg px-3 py-2 text-sm bg-slate-50"
                    />
                    <button
                      type="button"
                      onClick={handleCopyLink}
                      disabled={copyState === "copied"}
                      data-testid="share-copy-button"
                      className={`min-w-[96px] w-full shrink-0 px-4 py-2 text-sm font-medium rounded-lg sm:w-auto inline-flex items-center justify-center gap-1.5 ${
                        copyState === "copied"
                          ? "border border-green-300 bg-green-50 text-green-700"
                          : copyState === "error"
                            ? "border border-red-300 bg-red-50 text-red-700"
                            : "border border-slate-300 text-slate-700 hover:bg-slate-50"
                      }`}
                    >
                      {copyState === "copied" && (
                        <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                        </svg>
                      )}
                      {copyState === "error" && (
                        <svg className="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      )}
                      {copyState === "idle" && "Copy"}
                      {copyState === "copied" && "Copied!"}
                      {copyState === "error" && "Copy failed"}
                    </button>
                  </div>
                  {shareExpiresAt && (
                    <p className="text-xs text-slate-500">
                      Expires {new Date(shareExpiresAt).toLocaleString()}
                    </p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Actions */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">
              Actions
            </h2>
            <div className="space-y-2">
              <Link
                href={`/app/jobs/${jobId}/estimates`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
              >
                Back to Estimates
              </Link>
              <Link
                href={`/app/estimates/${estimateId}/edit`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                Edit Estimate
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
