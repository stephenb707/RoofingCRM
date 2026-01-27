"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { getLead } from "@/lib/leadsApi";
import { listCustomers } from "@/lib/customersApi";
import { createJob } from "@/lib/jobsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { JOB_TYPES, JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { formatAddress } from "@/lib/format";
import type { JobType, AddressDto, CreateJobRequest } from "@/lib/types";

const emptyAddress: AddressDto = {
  line1: "",
  line2: null,
  city: null,
  state: null,
  zip: null,
  countryCode: "US",
};

export default function NewJobPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const leadIdFromQuery = searchParams.get("leadId");
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [jobType, setJobType] = useState<JobType | "">("");
  const [customerId, setCustomerId] = useState<string>("");
  const [customerSearch, setCustomerSearch] = useState("");
  const [debouncedCustomerSearch, setDebouncedCustomerSearch] = useState("");
  const [addressLine1, setAddressLine1] = useState("");
  const [addressLine2, setAddressLine2] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [zip, setZip] = useState("");
  const [scheduledStartDate, setScheduledStartDate] = useState("");
  const [scheduledEndDate, setScheduledEndDate] = useState("");
  const [crewName, setCrewName] = useState("");
  const [internalNotes, setInternalNotes] = useState("");
  const [error, setError] = useState<string | null>(null);

  const { data: lead, isLoading: leadLoading } = useQuery({
    queryKey: ["lead", auth.selectedTenantId, leadIdFromQuery!],
    queryFn: () => getLead(api, leadIdFromQuery!),
    enabled: !!auth.selectedTenantId && !!leadIdFromQuery,
  });

  // Debounce customer search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedCustomerSearch(customerSearch);
    }, 300);
    return () => clearTimeout(timer);
  }, [customerSearch]);

  const { data: customersData } = useQuery({
    queryKey: ["customers", auth.selectedTenantId, debouncedCustomerSearch || null, 0],
    queryFn: () => listCustomers(api, { page: 0, size: 100, q: debouncedCustomerSearch || null }),
    enabled: !!auth.selectedTenantId && !leadIdFromQuery,
  });

  const customers = customersData?.content ?? [];

  useEffect(() => {
    if (lead?.propertyAddress) {
      const a = lead.propertyAddress;
      setAddressLine1(a.line1 ?? "");
      setAddressLine2(a.line2 ?? "");
      setCity(a.city ?? "");
      setState(a.state ?? "");
      setZip(a.zip ?? "");
    }
  }, [lead?.propertyAddress]);

  const createMutation = useMutation({
    mutationFn: (payload: CreateJobRequest) => createJob(api, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["jobs", auth.selectedTenantId] });
      router.push(`/app/jobs/${data.id}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to create job:", err);
      setError(getApiErrorMessage(err, "Failed to create job. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!jobType) {
      setError("Please select a job type.");
      return;
    }
    if (!addressLine1.trim()) {
      setError("Property address (line 1) is required.");
      return;
    }

    const propertyAddress: AddressDto = {
      line1: addressLine1.trim(),
      line2: addressLine2.trim() || null,
      city: city.trim() || null,
      state: state.trim() || null,
      zip: zip.trim() || null,
      countryCode: "US",
    };

    const payload: CreateJobRequest = {
      type: jobType as JobType,
      propertyAddress,
      scheduledStartDate: scheduledStartDate || null,
      scheduledEndDate: scheduledEndDate || null,
      internalNotes: internalNotes.trim() || null,
      crewName: crewName.trim() || null,
    };

    if (leadIdFromQuery) {
      payload.leadId = leadIdFromQuery;
    } else {
      if (!customerId) {
        setError("Please select a customer.");
        return;
      }
      payload.customerId = customerId;
    }

    createMutation.mutate(payload);
  };

  const isLeadMode = !!leadIdFromQuery;

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link
          href="/app/jobs"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Jobs
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">New Job</h1>
        <p className="text-sm text-slate-500 mt-1">
          {isLeadMode
            ? "Create a job from this lead"
            : "Create a job for a customer"}
        </p>
      </div>

      {isLeadMode && leadLoading && (
        <div className="bg-white rounded-xl border border-slate-200 p-8 text-center mb-6">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-sky-600 mx-auto mb-2"></div>
          <p className="text-sm text-slate-500">Loading lead…</p>
        </div>
      )}

      {isLeadMode && !leadLoading && !lead && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6">
          <p className="text-sm text-red-800">Lead not found. Check the lead ID.</p>
        </div>
      )}

      <form onSubmit={handleSubmit}>
        {isLeadMode && lead && (
          <div className="bg-slate-50 rounded-xl border border-slate-200 p-4 mb-6">
            <p className="text-sm font-medium text-slate-700 mb-2">Creating job from lead</p>
            {lead.customerFirstName || lead.customerLastName ? (
              <p className="text-xs text-slate-600 mb-1">
                Customer: {[lead.customerFirstName, lead.customerLastName].filter(Boolean).join(" ") || "—"}
              </p>
            ) : null}
            {lead.propertyAddress ? (
              <p className="text-xs text-slate-600 mb-1">
                Address: {formatAddress(lead.propertyAddress)}
              </p>
            ) : null}
            <p className="text-xs text-slate-500 mt-2">
              Lead: <Link href={`/app/leads/${leadIdFromQuery}`} className="text-sky-600 hover:underline">View lead</Link> (ID: {leadIdFromQuery?.slice(0, 8)}…)
            </p>
          </div>
        )}

        {!isLeadMode && (
          <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Customer</h2>
            <label htmlFor="customer-search" className="block text-sm font-medium text-slate-700 mb-1.5">
              Search customer <span className="text-red-500">*</span>
            </label>
            <input
              id="customer-search"
              type="text"
              value={customerSearch}
              onChange={(e) => setCustomerSearch(e.target.value)}
              placeholder="Search by name, email, or phone..."
              className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent mb-3"
            />
            <label htmlFor="customer-select" className="block text-sm font-medium text-slate-700 mb-1.5">
              Select customer <span className="text-red-500">*</span>
            </label>
            <select
              id="customer-select"
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value)}
              required={!isLeadMode}
              className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            >
              <option value="">Select customer</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.firstName} {c.lastName}
                  {c.primaryPhone ? ` — ${c.primaryPhone}` : ""}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Job details</h2>
          <div className="space-y-4">
            <div>
              <label htmlFor="job-type-select" className="block text-sm font-medium text-slate-700 mb-1.5">
                Job type <span className="text-red-500">*</span>
              </label>
              <select
                id="job-type-select"
                value={jobType}
                onChange={(e) => setJobType(e.target.value as JobType)}
                required
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              >
                <option value="">Select type</option>
                {JOB_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {JOB_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Property address (line 1) <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={addressLine1}
                onChange={(e) => setAddressLine1(e.target.value)}
                required
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="123 Main Street"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Address line 2</label>
              <input
                type="text"
                value={addressLine2}
                onChange={(e) => setAddressLine2(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="Apt 4B"
              />
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700 mb-1.5">City</label>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="Denver"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">State</label>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="CO"
                  maxLength={2}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">ZIP</label>
                <input
                  type="text"
                  value={zip}
                  onChange={(e) => setZip(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="80202"
                  maxLength={10}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Scheduled start</label>
                <input
                  type="date"
                  value={scheduledStartDate}
                  onChange={(e) => setScheduledStartDate(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Scheduled end</label>
                <input
                  type="date"
                  value={scheduledEndDate}
                  onChange={(e) => setScheduledEndDate(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Crew name</label>
              <input
                type="text"
                value={crewName}
                onChange={(e) => setCrewName(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="Crew A"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Internal notes</label>
              <textarea
                value={internalNotes}
                onChange={(e) => setInternalNotes(e.target.value)}
                rows={3}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent resize-none"
                placeholder="Internal notes…"
              />
            </div>
          </div>
        </div>

        {error && (
          <div className="mb-6 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3 flex items-center gap-2">
            {error}
          </div>
        )}

        <div className="flex items-center justify-end gap-3">
          <Link
            href="/app/jobs"
            className="px-4 py-2.5 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Cancel
          </Link>
          <button
            type="submit"
            disabled={createMutation.isPending || (isLeadMode && (leadLoading || !lead))}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60 transition-colors shadow-sm"
          >
            {createMutation.isPending ? "Creating…" : "Create Job"}
          </button>
        </div>
      </form>
    </div>
  );
}
