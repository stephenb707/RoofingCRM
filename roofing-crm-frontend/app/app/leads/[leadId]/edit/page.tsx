"use client";

import { FormEvent, useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { getLead, updateLead } from "@/lib/leadsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { LEAD_SOURCES, SOURCE_LABELS } from "@/lib/leadsConstants";
import type { LeadSource, AddressDto } from "@/lib/types";

export default function EditLeadPage() {
  const params = useParams();
  const router = useRouter();
  const leadId = params.leadId as string;
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [source, setSource] = useState<LeadSource | "">("");
  const [leadNotes, setLeadNotes] = useState("");
  const [preferredContactMethod, setPreferredContactMethod] = useState("");
  const [line1, setLine1] = useState("");
  const [line2, setLine2] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [zip, setZip] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: lead, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.lead(auth.selectedTenantId, leadId),
    queryFn: () => getLead(api, leadId),
    enabled: !!auth.selectedTenantId && !!leadId,
  });

  useEffect(() => {
    if (lead) {
      setSource((lead.source as LeadSource) ?? "");
      setLeadNotes(lead.leadNotes ?? "");
      setPreferredContactMethod(lead.preferredContactMethod ?? "");
      const addr = lead.propertyAddress;
      setLine1(addr?.line1 ?? "");
      setLine2(addr?.line2 ?? "");
      setCity(addr?.city ?? "");
      setState(addr?.state ?? "");
      setZip(addr?.zip ?? "");
    }
  }, [lead]);

  const mutation = useMutation({
    mutationFn: (payload: {
      source?: LeadSource | null;
      leadNotes?: string | null;
      preferredContactMethod?: string | null;
      propertyAddress?: AddressDto | null;
    }) => updateLead(api, leadId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.lead(auth.selectedTenantId, leadId) });
      queryClient.invalidateQueries({ queryKey: ["leads", auth.selectedTenantId] });
      router.push(`/app/leads/${leadId}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to update lead:", err);
      setFormError(getApiErrorMessage(err, "Failed to update lead. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (!line1.trim()) {
      setFormError("Property address line 1 is required.");
      return;
    }
    const propertyAddress: AddressDto = {
      line1: line1.trim(),
      line2: line2.trim() || undefined,
      city: city.trim() || undefined,
      state: state.trim() || undefined,
      zip: zip.trim() || undefined,
    };
    mutation.mutate({
      source: source || null,
      leadNotes: leadNotes.trim() || null,
      preferredContactMethod: preferredContactMethod.trim() || null,
      propertyAddress,
    });
  };

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading lead…</p>
        </div>
      </div>
    );
  }

  if (isError || !lead) {
    return (
      <div className="max-w-2xl mx-auto">
        <Link href={`/app/leads/${leadId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
          Back to Lead
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load lead</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The lead could not be found.")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link href={`/app/leads/${leadId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
          Back to Lead
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">Edit Lead</h1>
        <p className="text-sm text-slate-500 mt-1">Update lead details and property address</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {formError && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">{formError}</div>
        )}

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Lead details</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Source</label>
              <select
                value={source}
                onChange={(e) => setSource(e.target.value as LeadSource | "")}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                <option value="">Select source</option>
                {LEAD_SOURCES.map((s) => (
                  <option key={s} value={s}>{SOURCE_LABELS[s]}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Preferred contact method</label>
              <input
                type="text"
                value={preferredContactMethod}
                onChange={(e) => setPreferredContactMethod(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                placeholder="e.g. Phone, Email"
              />
            </div>
            <div>
              <label htmlFor="notes" className="block text-sm font-medium text-slate-700 mb-1.5">Notes</label>
              <textarea
                id="notes"
                name="notes"
                value={leadNotes}
                onChange={(e) => setLeadNotes(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                placeholder="Lead notes…"
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Property address</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Address line 1 <span className="text-red-500">*</span></label>
              <input
                type="text"
                value={line1}
                onChange={(e) => setLine1(e.target.value)}
                required
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                placeholder="123 Main St"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Address line 2</label>
              <input
                type="text"
                value={line2}
                onChange={(e) => setLine2(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                placeholder="Apt 4"
              />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">City</label>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  placeholder="Chicago"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">State</label>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  placeholder="IL"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">ZIP</label>
                <input
                  type="text"
                  value={zip}
                  onChange={(e) => setZip(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                  placeholder="60601"
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
            {mutation.isPending ? "Saving…" : "Save changes"}
          </button>
          <Link
            href={`/app/leads/${leadId}`}
            className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
