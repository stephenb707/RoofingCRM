"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { listCustomerPhotoReports } from "@/lib/customerPhotoReportsApi";
import { formatDate } from "@/lib/format";
import { queryKeys } from "@/lib/queryKeys";

export default function CustomerPhotoReportsListPage() {
  const { api, auth, ready } = useAuthReady();

  const { data: reports = [], isLoading } = useQuery({
    queryKey: queryKeys.customerPhotoReports(auth.selectedTenantId),
    queryFn: () => listCustomerPhotoReports(api),
    enabled: ready && !!auth.selectedTenantId,
  });

  if (!ready) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex flex-wrap items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Customer photo reports</h1>
          <p className="text-sm text-slate-500 mt-1">
            Customer-facing reports with photos and narrative — export to PDF when ready.
          </p>
        </div>
        <Link
          href="/app/reports/customer-reports/new"
          className="inline-flex items-center px-4 py-2 rounded-lg bg-sky-600 text-white text-sm font-medium hover:bg-sky-700 shadow-sm"
        >
          Create report
        </Link>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <p className="p-8 text-sm text-slate-500 text-center">Loading…</p>
        ) : reports.length === 0 ? (
          <p className="p-8 text-sm text-slate-500 text-center">
            No reports yet.{" "}
            <Link href="/app/reports/customer-reports/new" className="text-sky-600 hover:text-sky-700">
              Create your first report
            </Link>
            .
          </p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {reports.map((r) => (
              <li key={r.id}>
                <Link
                  href={`/app/reports/customer-reports/${r.id}`}
                  className="block px-5 py-4 hover:bg-slate-50 transition-colors"
                >
                  <div className="font-medium text-slate-800">{r.title}</div>
                  <div className="text-xs text-slate-500 mt-1">
                    {r.customerName}
                    {r.reportType ? ` · ${r.reportType}` : ""}
                    {r.updatedAt ? ` · Updated ${formatDate(r.updatedAt)}` : ""}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      <p className="text-sm text-slate-500 mt-6">
        <Link href="/app/reports" className="text-sky-600 hover:text-sky-700">
          ← Back to all reports
        </Link>
      </p>
    </div>
  );
}
