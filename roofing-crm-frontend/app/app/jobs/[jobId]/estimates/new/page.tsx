"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { createEstimateForJob } from "@/lib/estimatesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import type { EstimateItemRequest } from "@/lib/types";

const emptyItem = (): EstimateItemRequest => ({
  name: "",
  description: "",
  quantity: 1,
  unitPrice: 0,
  unit: "ea",
});

export default function NewEstimatePage() {
  const params = useParams();
  const router = useRouter();
  const jobId = params.jobId as string;
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState("");
  const [notes, setNotes] = useState("");
  const [issueDate, setIssueDate] = useState("");
  const [validUntil, setValidUntil] = useState("");
  const [items, setItems] = useState<EstimateItemRequest[]>([emptyItem()]);
  const [formError, setFormError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: (payload: { title?: string; notes?: string; issueDate?: string; validUntil?: string; items: EstimateItemRequest[] }) =>
      createEstimateForJob(api, jobId, {
        title: payload.title || null,
        notes: payload.notes || null,
        issueDate: payload.issueDate || null,
        validUntil: payload.validUntil || null,
        items: payload.items,
      }),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, jobId) });
      router.push(`/app/estimates/${data.id}`);
    },
    onError: (err) => {
      console.error("Create estimate failed:", err);
      setFormError(getApiErrorMessage(err, "Failed to create estimate. Please try again."));
    },
  });

  const updateItem = (index: number, patch: Partial<EstimateItemRequest>) => {
    setItems((prev) =>
      prev.map((it, i) => (i === index ? { ...it, ...patch } : it))
    );
  };

  const addItem = () => setItems((prev) => [...prev, emptyItem()]);

  const removeItem = (index: number) => {
    if (items.length <= 1) return;
    setItems((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const validItems = items.map((it) => ({
      ...it,
      name: it.name.trim() || "Item",
      description: it.description || null,
      unit: it.unit || null,
      quantity: Number(it.quantity) || 0,
      unitPrice: Number(it.unitPrice) || 0,
    }));

    const hasValidQtyPrice = validItems.every(
      (it) => typeof it.quantity === "number" && typeof it.unitPrice === "number"
    );
    if (!hasValidQtyPrice) {
      setFormError("Quantity and unit price must be numbers.");
      return;
    }

    create.mutate({
      title: title.trim() || undefined,
      notes: notes.trim() || undefined,
      issueDate: issueDate || undefined,
      validUntil: validUntil || undefined,
      items: validItems,
    });
  };

  return (
    <div className="max-w-2xl mx-auto">
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
        <h1 className="text-2xl font-bold text-slate-800">New Estimate</h1>
        <p className="text-sm text-slate-500 mt-1">Create an estimate for this job</p>
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-xl border border-slate-200 p-6 space-y-6">
        {formError && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
            {formError}
          </div>
        )}

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Title (optional)</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            placeholder="e.g. Roof Replacement"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-slate-700 mb-1">Notes (optional)</label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Issue date (optional)</label>
            <input
              type="date"
              value={issueDate}
              onChange={(e) => setIssueDate(e.target.value)}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Valid until (optional)</label>
            <input
              type="date"
              value={validUntil}
              onChange={(e) => setValidUntil(e.target.value)}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-slate-700">Line items (at least one)</label>
            <button
              type="button"
              onClick={addItem}
              className="text-sm text-sky-600 hover:text-sky-700 font-medium"
            >
              + Add item
            </button>
          </div>
          <div className="space-y-4">
            {items.map((it, i) => (
              <div
                key={i}
                className="border border-slate-200 rounded-lg p-4 space-y-3 relative"
              >
                {items.length > 1 && (
                  <button
                    type="button"
                    onClick={() => removeItem(i)}
                    className="absolute top-2 right-2 text-slate-400 hover:text-red-600 text-sm"
                  >
                    Remove
                  </button>
                )}
                <div className="grid grid-cols-2 gap-3">
                  <div className="col-span-2">
                    <input
                      required
                      placeholder="Item name"
                      value={it.name}
                      onChange={(e) => updateItem(i, { name: e.target.value })}
                      className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    />
                  </div>
                  <div className="col-span-2">
                    <input
                      placeholder="Description (optional)"
                      value={it.description ?? ""}
                      onChange={(e) => updateItem(i, { description: e.target.value })}
                      className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Quantity</label>
                    <input
                      required
                      type="number"
                      min={0}
                      step="any"
                      value={it.quantity}
                      onChange={(e) => updateItem(i, { quantity: Number(e.target.value) || 0 })}
                      className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Unit price</label>
                    <input
                      required
                      type="number"
                      min={0}
                      step="0.01"
                      value={it.unitPrice}
                      onChange={(e) => updateItem(i, { unitPrice: Number(e.target.value) || 0 })}
                      className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-500 mb-1">Unit (optional)</label>
                    <input
                      placeholder="ea"
                      value={it.unit ?? ""}
                      onChange={(e) => updateItem(i, { unit: e.target.value || null })}
                      className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <button
            type="submit"
            disabled={create.isPending}
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-60"
          >
            {create.isPending ? "Creating…" : "Create estimate"}
          </button>
          <Link
            href={`/app/jobs/${jobId}/estimates`}
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
