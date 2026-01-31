"use client";

import { FormEvent, useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getEstimate, updateEstimate } from "@/lib/estimatesApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import type { EstimateItemRequest } from "@/lib/types";

function toDateInputValue(s: string | null | undefined): string {
  if (!s) return "";
  try {
    return new Date(s).toISOString().slice(0, 10);
  } catch {
    return "";
  }
}

function toItemRequest(item: { name: string; description?: string | null; quantity: number; unitPrice: number; unit?: string | null }): EstimateItemRequest {
  return {
    name: item.name,
    description: item.description ?? null,
    quantity: item.quantity,
    unitPrice: item.unitPrice,
    unit: item.unit ?? null,
  };
}

export default function EditEstimatePage() {
  const params = useParams();
  const router = useRouter();
  const estimateId = params.estimateId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [title, setTitle] = useState("");
  const [notes, setNotes] = useState("");
  const [issueDate, setIssueDate] = useState("");
  const [validUntil, setValidUntil] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: estimate, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId),
    queryFn: () => getEstimate(api, estimateId),
    enabled: ready && !!estimateId,
  });

  useEffect(() => {
    if (estimate) {
      setTitle(estimate.title ?? "");
      setNotes(estimate.notes ?? "");
      setIssueDate(toDateInputValue(estimate.issueDate));
      setValidUntil(toDateInputValue(estimate.validUntil));
    }
  }, [estimate]);

  const mutation = useMutation({
    mutationFn: (payload: {
      title?: string | null;
      notes?: string | null;
      issueDate?: string | null;
      validUntil?: string | null;
      items?: EstimateItemRequest[] | null;
    }) => updateEstimate(api, estimateId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId) });
      if (estimate?.jobId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, estimate.jobId) });
      }
      router.push(`/app/estimates/${estimateId}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to update estimate:", err);
      setFormError(getApiErrorMessage(err, "Failed to update estimate. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);
    
    // Include existing items in the payload (updateEstimate may require items)
    const items = estimate ? (estimate.items ?? []).map(toItemRequest) : null;
    
    mutation.mutate({
      title: title.trim() || null,
      notes: notes.trim() || null,
      issueDate: issueDate || null,
      validUntil: validUntil || null,
      items: items,
    });
  };

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading estimate…</p>
        </div>
      </div>
    );
  }

  if (isError || !estimate) {
    return (
      <div className="max-w-2xl mx-auto">
        <Link href={`/app/estimates/${estimateId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Estimate
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load estimate</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The estimate could not be found.")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link href={`/app/estimates/${estimateId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Estimate
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">Edit Estimate</h1>
        <p className="text-sm text-slate-500 mt-1">Update estimate details</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {formError && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">{formError}</div>
        )}

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Estimate details</h2>
          <div className="space-y-4">
            <div>
              <label htmlFor="title" className="block text-sm font-medium text-slate-700 mb-1.5">Title</label>
              <input
                id="title"
                name="title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="Estimate title"
              />
            </div>
            <div>
              <label htmlFor="notes" className="block text-sm font-medium text-slate-700 mb-1.5">Notes</label>
              <textarea
                id="notes"
                name="notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="Estimate notes…"
              />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="issueDate" className="block text-sm font-medium text-slate-700 mb-1.5">Issue date</label>
                <input
                  id="issueDate"
                  name="issueDate"
                  type="date"
                  value={issueDate}
                  onChange={(e) => setIssueDate(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
              <div>
                <label htmlFor="validUntil" className="block text-sm font-medium text-slate-700 mb-1.5">Valid until</label>
                <input
                  id="validUntil"
                  name="validUntil"
                  type="date"
                  value={validUntil}
                  onChange={(e) => setValidUntil(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60"
          >
            {mutation.isPending ? "Saving…" : "Save Changes"}
          </button>
          <Link
            href={`/app/estimates/${estimateId}`}
            className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
