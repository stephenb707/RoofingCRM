"use client";

import { FormEvent, useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { getJob, updateJob } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { JOB_TYPES, JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import type { JobType, AddressDto, UpdateJobRequest } from "@/lib/types";

function toDateInputValue(s: string | null | undefined): string {
  if (!s) return "";
  try {
    const d = new Date(s);
    return d.toISOString().slice(0, 10);
  } catch {
    return "";
  }
}

export default function EditJobPage() {
  const params = useParams();
  const router = useRouter();
  const jobId = params.jobId as string;
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [type, setType] = useState<JobType>("REPLACEMENT");
  const [line1, setLine1] = useState("");
  const [line2, setLine2] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [zip, setZip] = useState("");
  const [scheduledStartDate, setScheduledStartDate] = useState("");
  const [scheduledEndDate, setScheduledEndDate] = useState("");
  const [internalNotes, setInternalNotes] = useState("");
  const [crewName, setCrewName] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: job, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.job(auth.selectedTenantId, jobId),
    queryFn: () => getJob(api, jobId),
    enabled: !!auth.selectedTenantId && !!jobId,
  });

  useEffect(() => {
    if (job) {
      setType(job.type);
      const addr = job.propertyAddress;
      setLine1(addr?.line1 ?? "");
      setLine2(addr?.line2 ?? "");
      setCity(addr?.city ?? "");
      setState(addr?.state ?? "");
      setZip(addr?.zip ?? "");
      setScheduledStartDate(toDateInputValue(job.scheduledStartDate));
      setScheduledEndDate(toDateInputValue(job.scheduledEndDate));
      setInternalNotes(job.internalNotes ?? "");
      setCrewName(job.crewName ?? "");
    }
  }, [job]);

  const mutation = useMutation({
    mutationFn: (payload: UpdateJobRequest) => updateJob(api, jobId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.job(auth.selectedTenantId, jobId) });
      queryClient.invalidateQueries({ queryKey: ["jobs", auth.selectedTenantId] });
      router.push(`/app/jobs/${jobId}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to update job:", err);
      setFormError(getApiErrorMessage(err, "Failed to update job. Please try again."));
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
      type,
      propertyAddress,
      scheduledStartDate: scheduledStartDate || null,
      scheduledEndDate: scheduledEndDate || null,
      internalNotes: internalNotes.trim() || null,
      crewName: crewName.trim() || null,
    });
  };

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading job…</p>
        </div>
      </div>
    );
  }

  if (isError || !job) {
    return (
      <div className="max-w-2xl mx-auto">
        <Link href={`/app/jobs/${jobId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
          Back to Job
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load job</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The job could not be found.")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link href={`/app/jobs/${jobId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
          Back to Job
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">Edit Job</h1>
        <p className="text-sm text-slate-500 mt-1">Update job details and property address</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {formError && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">{formError}</div>
        )}

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Job details</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Type</label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value as JobType)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                {JOB_TYPES.map((t) => (
                  <option key={t} value={t}>{JOB_TYPE_LABELS[t]}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Scheduled start date</label>
              <input
                type="date"
                value={scheduledStartDate}
                onChange={(e) => setScheduledStartDate(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Scheduled end date</label>
              <input
                type="date"
                value={scheduledEndDate}
                onChange={(e) => setScheduledEndDate(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Crew name</label>
              <input
                type="text"
                value={crewName}
                onChange={(e) => setCrewName(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                placeholder="Crew A"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Internal notes</label>
              <textarea
                value={internalNotes}
                onChange={(e) => setInternalNotes(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
                placeholder="Internal notes…"
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
            href={`/app/jobs/${jobId}`}
            className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
