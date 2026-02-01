"use client";

import { FormEvent, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getLead, convertLeadToJob } from "@/lib/leadsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { JOB_TYPES, JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { formatAddress } from "@/lib/format";
import { DateRangePicker } from "@/components/DateRangePicker";
import type { JobType, ConvertLeadToJobRequest } from "@/lib/types";

export default function ConvertLeadPage() {
  const params = useParams();
  const router = useRouter();
  const leadId = params.leadId as string;
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const [jobType, setJobType] = useState<JobType | "">("");
  const [scheduledStartDate, setScheduledStartDate] = useState("");
  const [scheduledEndDate, setScheduledEndDate] = useState("");
  const [crewName, setCrewName] = useState("");
  const [internalNotes, setInternalNotes] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: lead, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.lead(auth.selectedTenantId, leadId),
    queryFn: () => getLead(api, leadId),
    enabled: ready && !!leadId,
  });

  const mutation = useMutation({
    mutationFn: (payload: ConvertLeadToJobRequest) => convertLeadToJob(api, leadId, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.lead(auth.selectedTenantId, leadId) });
      queryClient.invalidateQueries({ queryKey: ["jobs", auth.selectedTenantId] });
      router.push(`/app/jobs/${data.id}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to convert lead:", err);
      setFormError(getApiErrorMessage(err, "Failed to convert lead. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!jobType) {
      setFormError("Please select a job type.");
      return;
    }

    const payload: ConvertLeadToJobRequest = {
      type: jobType,
      scheduledStartDate: scheduledStartDate.trim() || null,
      scheduledEndDate: scheduledEndDate.trim() || null,
      crewName: crewName.trim() || null,
      internalNotes: internalNotes.trim() || null,
    };

    mutation.mutate(payload);
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
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Lead
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load lead</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The lead could not be found.")}</p>
        </div>
      </div>
    );
  }

  // Already converted: show message + links, no form (single "Back to Lead" in actions)
  if (lead.convertedJobId) {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="mb-6">
          <Link href={`/app/leads/${leadId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2" aria-label="Back">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back
          </Link>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h1 className="text-xl font-bold text-slate-800 mb-2">Lead already converted</h1>
          <p className="text-sm text-slate-600 mb-6">This lead was already converted to a job.</p>
          <div className="flex flex-wrap gap-3">
            <Link
              href={`/app/jobs/${lead.convertedJobId}`}
              className="inline-flex items-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
            >
              View Job
            </Link>
            <Link
              href={`/app/jobs/${lead.convertedJobId}/estimates/new`}
              className="inline-flex items-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
            >
              Create Estimate
            </Link>
            <Link
              href={`/app/leads/${leadId}`}
              className="inline-flex items-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
            >
              Back to Lead
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // LOST: cannot convert (single "Back to Lead" in card)
  if (lead.status === "LOST") {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="mb-6">
          <Link href={`/app/leads/${leadId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2" aria-label="Back">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back
          </Link>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h1 className="text-xl font-bold text-slate-800 mb-2">Cannot convert this lead</h1>
          <p className="text-sm text-slate-600 mb-6">Leads with status LOST cannot be converted to a job.</p>
          <Link
            href={`/app/leads/${leadId}`}
            className="inline-flex items-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
          >
            Back to Lead
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link href={`/app/leads/${leadId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Lead
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">Convert Lead to Job</h1>
        <p className="text-sm text-slate-500 mt-1">Create a job from this lead</p>
      </div>

      {/* Lead Summary */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Lead Summary</h2>
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">Customer:</dt>
            <dd className="text-slate-800 font-medium">
              {[lead.customerFirstName, lead.customerLastName].filter(Boolean).join(" ") || "—"}
            </dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Property Address:</dt>
            <dd className="text-slate-800">{formatAddress(lead.propertyAddress)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">Status:</dt>
            <dd className="text-slate-800">{lead.status}</dd>
          </div>
        </dl>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6" noValidate>
        {formError && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">{formError}</div>
        )}

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Job Details</h2>
          <div className="space-y-4">
            <div>
              <label htmlFor="jobType" className="block text-sm font-medium text-slate-700 mb-1.5">
                Job Type <span className="text-red-500">*</span>
              </label>
              <select
                id="jobType"
                value={jobType}
                onChange={(e) => setJobType(e.target.value as JobType)}
                required
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              >
                <option value="">Select job type</option>
                {JOB_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {JOB_TYPE_LABELS[type]}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Scheduled dates
              </label>
              <DateRangePicker
                startDate={scheduledStartDate}
                endDate={scheduledEndDate}
                onChange={(start, end) => {
                  setScheduledStartDate(start);
                  setScheduledEndDate(end);
                }}
                placeholder="Select date range…"
              />
            </div>

            <div>
              <label htmlFor="crewName" className="block text-sm font-medium text-slate-700 mb-1.5">Crew Name</label>
              <input
                id="crewName"
                type="text"
                value={crewName}
                onChange={(e) => setCrewName(e.target.value)}
                placeholder="Optional"
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>

            <div>
              <label htmlFor="internalNotes" className="block text-sm font-medium text-slate-700 mb-1.5">
                Internal Notes
              </label>
              <textarea
                id="internalNotes"
                name="internalNotes"
                value={internalNotes}
                onChange={(e) => setInternalNotes(e.target.value)}
                rows={4}
                placeholder="Optional"
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60"
          >
            {mutation.isPending ? "Converting…" : "Convert to Job"}
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
