"use client";

import { useParams } from "next/navigation";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getCustomer } from "@/lib/customersApi";
import { listJobs } from "@/lib/jobsApi";
import { listLeads } from "@/lib/leadsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { formatPhone, formatAddress, formatDate } from "@/lib/format";
import { PREFERRED_CONTACT_LABELS } from "@/lib/preferredContactConstants";
import type { LeadDto } from "@/lib/types";
import { JOB_TYPE_LABELS, JOB_STATUS_LABELS, JOB_STATUS_COLORS } from "@/lib/jobsConstants";
import { STATUS_LABELS, STATUS_COLORS } from "@/lib/leadsConstants";

export default function CustomerDetailPage() {
  const params = useParams();
  const customerId = params.customerId as string;
  const { api, auth, ready } = useAuthReady();

  const { data: customer, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.customer(auth.selectedTenantId, customerId),
    queryFn: () => getCustomer(api, customerId),
    enabled: ready && !!customerId,
  });

  const { data: jobsData } = useQuery({
    queryKey: queryKeys.jobsList(auth.selectedTenantId, null, customerId, 0),
    queryFn: () => listJobs(api, { customerId, page: 0, size: 10 }),
    enabled: ready && !!customerId,
  });

  const { data: leadsData } = useQuery({
    queryKey: queryKeys.leadsList(auth.selectedTenantId, null, customerId, 0),
    queryFn: () => listLeads(api, { customerId, page: 0, size: 10 }),
    enabled: ready && !!customerId,
  });

  const jobs = jobsData?.content ?? [];
  const leads = leadsData?.content ?? [];

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading customer details…</p>
        </div>
      </div>
    );
  }

  if (isError || !customer) {
    return (
      <div className="max-w-4xl mx-auto">
        <Link href="/app/customers" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Customers
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load customer</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The customer could not be found.")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Link href="/app/customers" className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Customers
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">
              {customer.firstName} {customer.lastName}
            </h1>
            <p className="text-sm text-slate-500 mt-1">Customer Details</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          {/* Customer Information */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Customer Information</h2>
            <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Name</dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {customer.firstName} {customer.lastName}
                </dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Phone</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatPhone(customer.primaryPhone)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Email</dt>
                <dd className="mt-1 text-sm text-slate-800">{customer.email || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Preferred contact</dt>
                <dd className="mt-1 text-sm text-slate-800">
                  {customer.preferredContactMethod
                    ? PREFERRED_CONTACT_LABELS[customer.preferredContactMethod]
                    : "—"}
                </dd>
              </div>
              {customer.billingAddress && (
                <div className="sm:col-span-2">
                  <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Billing Address</dt>
                  <dd className="mt-1 text-sm text-slate-800">{formatAddress(customer.billingAddress)}</dd>
                </div>
              )}
              {customer.notes && (
                <div className="sm:col-span-2">
                  <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Notes</dt>
                  <dd className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">{customer.notes}</dd>
                </div>
              )}
            </dl>
          </div>

          {/* Related Jobs */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-slate-800">Related Jobs</h2>
              <Link
                href={`/app/jobs?customerId=${customerId}`}
                className="text-sm text-sky-600 hover:text-sky-700 font-medium"
              >
                View all
              </Link>
            </div>
            {jobs.length === 0 ? (
              <p className="text-sm text-slate-500">No jobs found for this customer.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Type</th>
                      <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Status</th>
                      <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Address</th>
                      <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {jobs.map((job) => (
                      <tr key={job.id} className="hover:bg-slate-50">
                        <td className="px-4 py-2 text-sm text-slate-800">{JOB_TYPE_LABELS[job.type]}</td>
                        <td className="px-4 py-2">
                          <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${JOB_STATUS_COLORS[job.status]}`}>
                            {JOB_STATUS_LABELS[job.status]}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-sm text-slate-600">
                          {job.propertyAddress ? formatAddress(job.propertyAddress) : "—"}
                        </td>
                        <td className="px-4 py-2 text-right">
                          <Link href={`/app/jobs/${job.id}`} className="text-sm text-sky-600 hover:text-sky-700 font-medium">
                            View
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Related Leads */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-slate-800">Related Leads</h2>
              <Link
                href={`/app/leads?customerId=${customerId}`}
                className="text-sm text-sky-600 hover:text-sky-700 font-medium"
              >
                View all
              </Link>
            </div>
            {leads.length === 0 ? (
              <p className="text-sm text-slate-500">No leads found for this customer.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Status</th>
                      <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Address</th>
                      <th className="text-left px-4 py-2 text-xs font-semibold text-slate-600">Updated</th>
                      <th className="text-right px-4 py-2 text-xs font-semibold text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {leads.map((lead) => (
                      <tr key={lead.id} className="hover:bg-slate-50">
                        <td className="px-4 py-2">
                          <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${STATUS_COLORS[lead.status]}`}>
                            {STATUS_LABELS[lead.status]}
                          </span>
                        </td>
                        <td className="px-4 py-2 text-sm text-slate-600">{formatAddress(lead.propertyAddress)}</td>
                        <td className="px-4 py-2 text-sm text-slate-600">{formatDate(lead.updatedAt ?? lead.createdAt)}</td>
                        <td className="px-4 py-2 text-right">
                          <Link href={`/app/leads/${lead.id}`} className="text-sm text-sky-600 hover:text-sky-700 font-medium">
                            View
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        <div className="space-y-6">
          {/* Details */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Details</h2>
            <dl className="space-y-3">
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Created</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDate(customer.createdAt)}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-slate-500 uppercase tracking-wider">Last Updated</dt>
                <dd className="mt-1 text-sm text-slate-800">{formatDate(customer.updatedAt)}</dd>
              </div>
            </dl>
          </div>

          {/* Actions */}
          <div className="bg-white rounded-xl border border-slate-200 p-6">
            <h2 className="text-lg font-semibold text-slate-800 mb-4">Actions</h2>
            <div className="space-y-2">
              <Link
                href={`/app/tasks/new?customerId=${customerId}`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg transition-colors"
              >
                Create Task
              </Link>
              <Link
                href={`/app/customers/${customerId}/edit`}
                className="w-full inline-flex justify-center px-4 py-2.5 text-sm font-medium text-slate-700 bg-white border border-slate-300 hover:bg-slate-50 rounded-lg transition-colors"
              >
                Edit Customer
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
