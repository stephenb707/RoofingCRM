"use client";

import Link from "next/link";
import { useState, useCallback, useEffect, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import {
  downloadAccountingJobsExcel,
  downloadLeadsCsv,
  downloadJobsCsv,
  downloadPaidInvoicesPdf,
  getPaidInvoiceYears,
  triggerBrowserDownload,
  LIMIT_OPTIONS,
} from "@/lib/reportsApi";
import { LEAD_SOURCES, SOURCE_LABELS } from "@/lib/leadsConstants";
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import { queryKeys } from "@/lib/queryKeys";
import type { LeadSource } from "@/lib/types";

export default function ReportsPage() {
  const { api, auth, ready } = useAuthReady();
  const [leadsLoading, setLeadsLoading] = useState(false);
  const [jobsLoading, setJobsLoading] = useState(false);
  const [leadsError, setLeadsError] = useState<string | null>(null);
  const [jobsError, setJobsError] = useState<string | null>(null);

  const [leadsStatusDefinitionId, setLeadsStatusDefinitionId] = useState<string>("");
  const [leadsSource, setLeadsSource] = useState<LeadSource | "">("");
  const [leadsLimit, setLeadsLimit] = useState(2000);

  const [jobsStatusDefinitionId, setJobsStatusDefinitionId] = useState<string>("");
  const [jobsLimit, setJobsLimit] = useState(2000);

  const [invoiceYears, setInvoiceYears] = useState<number[] | null>(null);
  const [invoiceYearLoading, setInvoiceYearLoading] = useState(false);
  const [invoiceYearError, setInvoiceYearError] = useState<string | null>(null);
  const [selectedInvoiceYear, setSelectedInvoiceYear] = useState<number | null>(null);
  const [invoicePdfLoading, setInvoicePdfLoading] = useState(false);
  const [invoicePdfError, setInvoicePdfError] = useState<string | null>(null);

  const [accountingXlsxLoading, setAccountingXlsxLoading] = useState(false);
  const [accountingXlsxError, setAccountingXlsxError] = useState<string | null>(null);

  const { data: leadDefs = [] } = useQuery({
    queryKey: queryKeys.pipelineStatuses(auth.selectedTenantId, "LEAD"),
    queryFn: () => listPipelineStatuses(api, "LEAD"),
    enabled: ready && !!auth.selectedTenantId,
  });

  const { data: jobDefs = [] } = useQuery({
    queryKey: queryKeys.pipelineStatuses(auth.selectedTenantId, "JOB"),
    queryFn: () => listPipelineStatuses(api, "JOB"),
    enabled: ready && !!auth.selectedTenantId,
  });

  const sortedLeadDefs = useMemo(
    () => [...leadDefs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder),
    [leadDefs]
  );

  const sortedJobDefs = useMemo(
    () => [...jobDefs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder),
    [jobDefs]
  );

  const handleDownloadLeads = useCallback(async () => {
    if (!ready) return;
    setLeadsError(null);
    setLeadsLoading(true);
    try {
      const { blob, filename } = await downloadLeadsCsv(api, {
        statusDefinitionId: leadsStatusDefinitionId || undefined,
        source: leadsSource || undefined,
        limit: leadsLimit,
      });
      triggerBrowserDownload(blob, filename);
    } catch (err) {
      setLeadsError("Failed to download. Check backend is running.");
    } finally {
      setLeadsLoading(false);
    }
  }, [api, ready, leadsStatusDefinitionId, leadsSource, leadsLimit]);

  const handleDownloadJobs = useCallback(async () => {
    if (!ready) return;
    setJobsError(null);
    setJobsLoading(true);
    try {
      const { blob, filename } = await downloadJobsCsv(api, {
        statusDefinitionId: jobsStatusDefinitionId || undefined,
        limit: jobsLimit,
      });
      triggerBrowserDownload(blob, filename);
    } catch (err) {
      setJobsError("Failed to download. Check backend is running.");
    } finally {
      setJobsLoading(false);
    }
  }, [api, ready, jobsStatusDefinitionId, jobsLimit]);

  useEffect(() => {
    if (!ready) return;
    let cancelled = false;
    const loadYears = async () => {
      setInvoiceYearError(null);
      setInvoiceYearLoading(true);
      try {
        const years = await getPaidInvoiceYears(api);
        if (cancelled) return;
        setInvoiceYears(years);
        setSelectedInvoiceYear(years.length > 0 ? years[0] : null);
      } catch (err) {
        if (cancelled) return;
        setInvoiceYears([]);
        setSelectedInvoiceYear(null);
        setInvoiceYearError("Failed to load paid invoice years.");
      } finally {
        if (!cancelled) setInvoiceYearLoading(false);
      }
    };
    loadYears();
    return () => {
      cancelled = true;
    };
  }, [api, ready]);

  const handleDownloadPaidInvoicesPdf = useCallback(async () => {
    if (!ready || selectedInvoiceYear == null) return;
    setInvoicePdfError(null);
    setInvoicePdfLoading(true);
    try {
      const { blob, filename } = await downloadPaidInvoicesPdf(api, selectedInvoiceYear);
      triggerBrowserDownload(blob, filename);
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 404) {
        setInvoicePdfError("No paid invoices found for that year.");
      } else if (status === 403) {
        setInvoicePdfError("You do not have permission to generate this report.");
      } else {
        setInvoicePdfError("Failed to download. Check backend is running.");
      }
    } finally {
      setInvoicePdfLoading(false);
    }
  }, [api, ready, selectedInvoiceYear]);

  const handleDownloadAccountingExcel = useCallback(async () => {
    if (!ready) return;
    setAccountingXlsxError(null);
    setAccountingXlsxLoading(true);
    try {
      const { blob, filename } = await downloadAccountingJobsExcel(api);
      triggerBrowserDownload(blob, filename);
    } catch (err) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 403) {
        setAccountingXlsxError("You do not have permission to download this report.");
      } else {
        setAccountingXlsxError("Failed to download. Check backend is running.");
      }
    } finally {
      setAccountingXlsxLoading(false);
    }
  }, [api, ready]);

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

      {/* Customer photo reports */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-2">Customer photo reports</h2>
        <p className="text-sm text-slate-600 mb-4">
          Create customer-facing reports with photos and text, ideal for inspections, before and after
          documentation, scope explanations, and recommendations. Build sections, pull in job or customer
          photos, then download or email a polished PDF.
        </p>
        <Link
          href="/app/reports/customer-reports"
          className="inline-flex items-center px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors shadow-sm"
        >
          Open photo reports
        </Link>
      </div>

      {/* Accounting / job profitability (Excel) */}
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-2">Accounting Report</h2>
        <p className="text-sm text-slate-600 mb-4">
          Export job-level accounting and profitability (agreed, invoiced, paid, costs, margins, and cost
          categories) as an Excel file you can edit or import into Google Sheets.
        </p>
        <button
          type="button"
          onClick={handleDownloadAccountingExcel}
          disabled={accountingXlsxLoading}
          className="px-4 py-2 bg-sky-600 hover:bg-sky-700 disabled:bg-sky-400 text-white text-sm font-medium rounded-lg transition-colors shadow-sm"
        >
          {accountingXlsxLoading ? "Downloading…" : "Download Excel"}
        </button>
        {accountingXlsxError && (
          <p className="text-sm text-red-600 mt-3">{accountingXlsxError}</p>
        )}
      </div>

      {/* Paid Invoices Annual PDF */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mt-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">
          Paid Invoices Annual Report
        </h2>
        {invoiceYearLoading || invoiceYears === null ? (
          <p className="text-sm text-slate-500">Loading available years…</p>
        ) : invoiceYears.length === 0 ? (
          <div className="space-y-3">
            <p className="text-sm text-slate-500">No paid invoices available yet.</p>
            <button
              type="button"
              disabled
              className="px-4 py-2 bg-sky-400 text-white text-sm font-medium rounded-lg shadow-sm"
            >
              Download PDF
            </button>
          </div>
        ) : (
          <div className="flex flex-wrap items-end gap-4">
            <div>
              <label htmlFor="paid-invoices-year" className="block text-sm font-medium text-slate-700 mb-1">
                Year
              </label>
              <select
                id="paid-invoices-year"
                value={selectedInvoiceYear ?? ""}
                onChange={(e) => setSelectedInvoiceYear(Number(e.target.value))}
                className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              >
                {invoiceYears.map((year) => (
                  <option key={year} value={year}>
                    {year}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              onClick={handleDownloadPaidInvoicesPdf}
              disabled={invoicePdfLoading || selectedInvoiceYear == null}
              className="px-4 py-2 bg-sky-600 hover:bg-sky-700 disabled:bg-sky-400 text-white text-sm font-medium rounded-lg transition-colors shadow-sm"
            >
              {invoicePdfLoading ? "Downloading…" : "Download PDF"}
            </button>
          </div>
        )}
        {invoiceYearError && (
          <p className="text-sm text-red-600 mt-3">{invoiceYearError}</p>
        )}
        {invoicePdfError && (
          <p className="text-sm text-red-600 mt-3">{invoicePdfError}</p>
        )}
      </div>

      {/* Jobs Export */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mt-6">
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
              value={jobsStatusDefinitionId}
              onChange={(e) => setJobsStatusDefinitionId(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {sortedJobDefs.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.label}
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

      {/* Leads Export */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mt-6">
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
              value={leadsStatusDefinitionId}
              onChange={(e) => setLeadsStatusDefinitionId(e.target.value)}
              className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">All</option>
              {sortedLeadDefs.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.label}
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
    </div>
  );
}
