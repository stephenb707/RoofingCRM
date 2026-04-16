"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
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
import { JOB_TYPE_LABELS } from "@/lib/jobsConstants";
import { jobStatusBadgeClass, leadStatusBadgeClass } from "@/lib/pipelineStatusVisuals";
import { DetailSectionNav, type DetailSectionNavItem } from "@/components/JobDetailSectionNav";

const SECTION_SCROLL_MARGIN_CLASS = "scroll-mt-24";

export default function CustomerDetailPage() {
  const params = useParams();
  const router = useRouter();
  const customerId = params.customerId as string;
  const { api, auth, ready } = useAuthReady();

  const navigateTo = (href: string) => {
    router.push(href);
  };

  const handleRowKeyDown = (e: React.KeyboardEvent<HTMLTableRowElement>, href: string) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      navigateTo(href);
    }
  };

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
  const sectionNavItems = useMemo<DetailSectionNavItem[]>(
    () => [
      { id: "customer-information", label: "Customer", icon: "customer" },
      { id: "related-jobs", label: "Related Jobs", icon: "jobs" },
      { id: "related-leads", label: "Related Leads", icon: "leads" },
    ],
    []
  );
  const sectionIds = useMemo(() => sectionNavItems.map((item) => item.id), [sectionNavItems]);
  const [activeSectionId, setActiveSectionId] = useState<string | null>(sectionNavItems[0]?.id ?? null);

  useEffect(() => {
    if (!activeSectionId || !sectionIds.includes(activeSectionId)) {
      setActiveSectionId(sectionNavItems[0]?.id ?? null);
    }
  }, [activeSectionId, sectionIds, sectionNavItems]);

  const scrollToSection = useCallback((id: string, behavior: ScrollBehavior = "smooth") => {
    if (typeof window === "undefined" || !sectionIds.includes(id)) {
      return;
    }

    document.getElementById(id)?.scrollIntoView({ behavior, block: "start" });
  }, [sectionIds]);

  const handleSectionNavigate = useCallback((id: string) => {
    setActiveSectionId(id);
    if (typeof window !== "undefined") {
      window.history.replaceState(null, "", `#${id}`);
    }
    scrollToSection(id);
  }, [scrollToSection]);

  useEffect(() => {
    const scrollToHashSection = () => {
      if (typeof window === "undefined") return;
      const id = window.location.hash.replace(/^#/, "");
      if (!id || !sectionIds.includes(id)) {
        return;
      }

      setActiveSectionId(id);
      requestAnimationFrame(() => {
        scrollToSection(id, "smooth");
      });
    };

    scrollToHashSection();
    window.addEventListener("hashchange", scrollToHashSection);
    return () => window.removeEventListener("hashchange", scrollToHashSection);
  }, [scrollToSection, sectionIds]);

  useEffect(() => {
    if (typeof window === "undefined" || sectionIds.length === 0) {
      return;
    }

    const elements = sectionIds
      .map((id) => document.getElementById(id))
      .filter((element): element is HTMLElement => Boolean(element));

    if (elements.length === 0) {
      return;
    }

    if (typeof IntersectionObserver === "undefined") {
      setActiveSectionId((current) => current ?? sectionIds[0]);
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const visibleEntries = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

        if (visibleEntries.length > 0) {
          setActiveSectionId(visibleEntries[0].target.id);
        }
      },
      {
        rootMargin: "-20% 0px -65% 0px",
        threshold: [0.1, 0.25, 0.5],
      }
    );

    elements.forEach((element) => observer.observe(element));
    return () => observer.disconnect();
  }, [sectionIds]);

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
    <div className="mx-auto max-w-7xl lg:pl-60">
      <div
        className="hidden lg:block lg:fixed lg:top-[8.5rem] lg:left-6 lg:z-10 lg:w-52"
        data-testid="customer-section-nav-rail-container"
      >
        <DetailSectionNav
          items={sectionNavItems}
          activeSectionId={activeSectionId}
          onNavigate={handleSectionNavigate}
          className="max-h-[calc(100vh-10rem)] overflow-y-auto"
        />
      </div>

      <div className="mx-auto max-w-6xl">
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

        <div className="mb-6 lg:hidden">
          <DetailSectionNav
            items={sectionNavItems}
            activeSectionId={activeSectionId}
            onNavigate={handleSectionNavigate}
            variant="inline"
          />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <section id="customer-information" className={SECTION_SCROLL_MARGIN_CLASS}>
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
            </section>

            <section id="related-jobs" className={SECTION_SCROLL_MARGIN_CLASS}>
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
                          <tr
                            key={job.id}
                            data-testid={`related-job-row-${job.id}`}
                            tabIndex={0}
                            role="link"
                            onClick={() => navigateTo(`/app/jobs/${job.id}`)}
                            onKeyDown={(e) => handleRowKeyDown(e, `/app/jobs/${job.id}`)}
                            className="cursor-pointer hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500"
                          >
                            <td className="px-4 py-2 text-sm text-slate-800">{JOB_TYPE_LABELS[job.type]}</td>
                            <td className="px-4 py-2">
                              <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${jobStatusBadgeClass(job.statusKey)}`}>
                                {job.statusLabel}
                              </span>
                            </td>
                            <td className="px-4 py-2 text-sm text-slate-600">
                              {job.propertyAddress ? formatAddress(job.propertyAddress) : "—"}
                            </td>
                            <td className="px-4 py-2 text-right">
                              <Link
                                href={`/app/jobs/${job.id}`}
                                onClick={(e) => e.stopPropagation()}
                                className="text-sm text-sky-600 hover:text-sky-700 font-medium"
                              >
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
            </section>

            <section id="related-leads" className={SECTION_SCROLL_MARGIN_CLASS}>
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
                          <tr
                            key={lead.id}
                            data-testid={`related-lead-row-${lead.id}`}
                            tabIndex={0}
                            role="link"
                            onClick={() => navigateTo(`/app/leads/${lead.id}`)}
                            onKeyDown={(e) => handleRowKeyDown(e, `/app/leads/${lead.id}`)}
                            className="cursor-pointer hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500"
                          >
                            <td className="px-4 py-2">
                              <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${leadStatusBadgeClass(lead.statusKey)}`}>
                                {lead.statusLabel}
                              </span>
                            </td>
                            <td className="px-4 py-2 text-sm text-slate-600">{formatAddress(lead.propertyAddress)}</td>
                            <td className="px-4 py-2 text-sm text-slate-600">{formatDate(lead.updatedAt ?? lead.createdAt)}</td>
                            <td className="px-4 py-2 text-right">
                              <Link
                                href={`/app/leads/${lead.id}`}
                                onClick={(e) => e.stopPropagation()}
                                className="text-sm text-sky-600 hover:text-sky-700 font-medium"
                              >
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
            </section>
          </div>

          <div className="space-y-6">
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
    </div>
  );
}
