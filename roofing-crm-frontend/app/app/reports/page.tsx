"use client";

import { useState, useCallback } from "react";
import { useAuthReady } from "@/lib/AuthContext";
import {
  downloadLeadsCsv,
  downloadJobsCsv,
  triggerBrowserDownload,
  LIMIT_OPTIONS,
} from "@/lib/reportsApi";
import {
  LEAD_STATUSES,
  STATUS_LABELS,
  LEAD_SOURCES,
  SOURCE_LABELS,
} from "@/lib/leadsConstants";
import {
  JOB_STATUSES,
  JOB_STATUS_LABELS,
} from "@/lib/jobsConstants";
import type { LeadStatus, LeadSource, JobStatus } from "@/lib/types";

export default function ReportsPage() {
  const { api, ready } = useAuthReady();
  const [leadsLoading, setLeadsLoading] = useState(false);
  const [jobsLoading, setJobsLoading] = useState(false);
  const [leadsError, setLeadsError] = useState<string | null>(null);
  const [jobsError, setJobsError] = useState<string | null>(null);

  const [leadsStatus, setLeadsStatus] = useState<LeadStatus | "">("");
  const [leadsSource, setLeadsSource] = useState<LeadSource | "">("");
  const [leadsLimit, setLeadsLimit] = useState(2000);

  const [jobsStatus, setJobsStatus] = useState<JobStatus | "">("");
  const [jobsLimit, setJobsLimit] = useState(2000);

  const handleDownloadLeads = useCallback(async () => {
    if (!ready) return;
    setLeadsError(null);
    setLeadsLoading(true);
    try {
      const { blob, filename } = await downloadLeadsCsv(api, {
        status: leadsStatus || undefined,
        source: leadsSource || undefined,
        limit: leadsLimit,
      });
      triggerBrowserDownload(blob, filename);
    } catch (err) {
      setLeadsError("Failed to download. Check backend is running.");
    } finally {
      setLeadsLoading(false);
    }
  }, [api, ready, leadsStatus, leadsSource, leadsLimit]);

  const handleDownloadJobs = useCallback(async () => {
    if (!ready) return;
    setJobsError(null);
    setJobsLoading(true);
    try {
      const { blob, filename } = await downloadJobsCsv(api, {
        status: jobsStatus || undefined,
        limit: jobsLimit,
      });
      triggerBrowserDownload(blob, filename);
    } catch (err) {
      setJobsError("Failed to download. Check backend is running.");
    } finally {
      setJobsLoading(false);
    }
  }, [api, ready, jobsStatus, jobsLimit]);

  if (!ready) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Reports</h1>

      {/* Leads Export */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">
          Pipeline / Leads Export
        </h2>
        <div className="flex flex-wrap items-end gap-4 mb-4">
          <div>
            <label htmlFor="leads-status" className="block text-sm font-medium text-slate-700 mb-1">
              Status
            </label>
            <select
              id="leads-status"
              value={leadsStatus}
              onChange={(e) => setLeadsStatus(e.target.value as LeadStatus | "")}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {LEAD_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="leads-source" className="block text-sm font-medium text-slate-700 mb-1">
              Source
            </label>
            <select
              id="leads-source"
              value={leadsSource}
              onChange={(e) => setLeadsSource(e.target.value as LeadSource | "")}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {LEAD_SOURCES.map((s) => (
                <option key={s} value={s}>
                  {SOURCE_LABELS[s]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="leads-limit" className="block text-sm font-medium text-slate-700 mb-1">
              Limit
            </label>
            <select
              id="leads-limit"
              value={leadsLimit}
              onChange={(e) => setLeadsLimit(Number(e.target.value))}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              {LIMIT_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </div>
          <button
            onClick={handleDownloadLeads}
            disabled={leadsLoading}
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 disabled:bg-sky-400 text-white text-sm font-medium rounded-lg transition-colors"
          >
            {leadsLoading ? "Downloading…" : "Download Leads CSV"}
          </button>
        </div>
        {leadsError && (
          <p className="text-sm text-red-600">{leadsError}</p>
        )}
      </div>

      {/* Jobs Export */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">
          Jobs Export
        </h2>
        <div className="flex flex-wrap items-end gap-4 mb-4">
          <div>
            <label htmlFor="jobs-status" className="block text-sm font-medium text-slate-700 mb-1">
              Status
            </label>
            <select
              id="jobs-status"
              value={jobsStatus}
              onChange={(e) => setJobsStatus(e.target.value as JobStatus | "")}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {JOB_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {JOB_STATUS_LABELS[s]}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="jobs-limit" className="block text-sm font-medium text-slate-700 mb-1">
              Limit
            </label>
            <select
              id="jobs-limit"
              value={jobsLimit}
              onChange={(e) => setJobsLimit(Number(e.target.value))}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              {LIMIT_OPTIONS.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </div>
          <button
            onClick={handleDownloadJobs}
            disabled={jobsLoading}
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 disabled:bg-sky-400 text-white text-sm font-medium rounded-lg transition-colors"
          >
            {jobsLoading ? "Downloading…" : "Download Jobs CSV"}
          </button>
        </div>
        {jobsError && (
          <p className="text-sm text-red-600">{jobsError}</p>
        )}
      </div>
    </div>
  );
}
