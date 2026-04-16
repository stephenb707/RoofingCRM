"use client";

import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import {
  PIPELINE_PATH_COMBINED,
  PIPELINE_PATH_JOBS,
  PIPELINE_PATH_LEADS,
  pipelineViewFromPathname,
  type PipelineViewId,
} from "@/lib/pipelineNav";

function jobsPipelineHref(customerId: string | null): string {
  if (customerId) {
    return `${PIPELINE_PATH_JOBS}?customerId=${encodeURIComponent(customerId)}`;
  }
  return PIPELINE_PATH_JOBS;
}

const SEGMENTS: { id: PipelineViewId; label: string }[] = [
  { id: "leads", label: "Leads" },
  { id: "jobs", label: "Jobs" },
  { id: "combined", label: "Combined" },
];

export default function PipelineViewSwitcher() {
  const pathname = usePathname() ?? "";
  const searchParams = useSearchParams();
  const customerId = searchParams.get("customerId");
  const active = pipelineViewFromPathname(pathname);

  return (
    <div
      className="inline-flex rounded-lg border border-slate-200 bg-slate-100 p-0.5 shadow-sm"
      role="group"
      aria-label="Pipeline view"
      data-testid="pipeline-view-switcher"
    >
      {SEGMENTS.map((seg) => {
        const isActive = active === seg.id;
        const href =
          seg.id === "leads"
            ? PIPELINE_PATH_LEADS
            : seg.id === "jobs"
              ? jobsPipelineHref(customerId)
              : PIPELINE_PATH_COMBINED;
        return (
          <Link
            key={seg.id}
            href={href}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors ${
              isActive
                ? "bg-white text-sky-700 shadow-sm border border-slate-200/80"
                : "text-slate-600 hover:text-slate-900"
            }`}
            aria-current={isActive ? "page" : undefined}
            data-testid={`pipeline-view-switch-${seg.id}`}
          >
            {seg.label}
          </Link>
        );
      })}
    </div>
  );
}
