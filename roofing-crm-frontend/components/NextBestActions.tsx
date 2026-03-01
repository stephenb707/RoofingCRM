"use client";

import Link from "next/link";
import type { EstimateStatus, JobStatus, LeadStatus } from "@/lib/types";

type EntityType = "lead" | "job" | "estimate";

interface NextBestActionsProps {
  entityType: EntityType;
  status: string;
  leadId?: string;
  jobId?: string;
  estimateId?: string;
}

interface ActionLink {
  label: string;
  href: string;
  primary?: boolean;
}

function leadActions(status: LeadStatus, leadId: string): ActionLink[] {
  if (status === "NEW") {
    return [
      { label: "Call customer", href: `/app/leads/${leadId}` },
      { label: "Schedule inspection", href: `/app/tasks/new?leadId=${leadId}` },
      { label: "Convert to job", href: `/app/leads/${leadId}/convert`, primary: true },
    ];
  }
  return [
    { label: "Create task", href: `/app/tasks/new?leadId=${leadId}`, primary: true },
    { label: "Edit lead", href: `/app/leads/${leadId}/edit` },
  ];
}

function estimateActions(status: EstimateStatus, estimateId: string, jobId: string): ActionLink[] {
  if (status === "DRAFT") {
    return [
      { label: "Share estimate", href: `/app/estimates/${estimateId}`, primary: true },
      { label: "Edit estimate", href: `/app/estimates/${estimateId}/edit` },
    ];
  }
  if (status === "SENT") {
    return [
      { label: "Share link", href: `/app/estimates/${estimateId}`, primary: true },
      { label: "Set follow-up", href: `/app/tasks/new?jobId=${jobId}` },
    ];
  }
  return [
    { label: "View estimate", href: `/app/estimates/${estimateId}`, primary: true },
    { label: "View job", href: `/app/jobs/${jobId}` },
  ];
}

function jobActions(status: JobStatus, jobId: string): ActionLink[] {
  if (status === "UNSCHEDULED") {
    return [
      { label: "Schedule", href: `/app/schedule`, primary: true },
      { label: "Create estimate", href: `/app/jobs/${jobId}/estimates/new` },
      { label: "Create task", href: `/app/tasks/new?jobId=${jobId}` },
    ];
  }
  if (status === "SCHEDULED") {
    return [
      { label: "Create task", href: `/app/tasks/new?jobId=${jobId}`, primary: true },
      { label: "Assign crew", href: `/app/jobs/${jobId}/edit` },
      { label: "Upload photos", href: `/app/jobs/${jobId}` },
    ];
  }
  return [
    { label: "Create task", href: `/app/tasks/new?jobId=${jobId}`, primary: true },
    { label: "View estimates", href: `/app/jobs/${jobId}/estimates` },
  ];
}

export function NextBestActions({
  entityType,
  status,
  leadId,
  jobId,
  estimateId,
}: NextBestActionsProps) {
  let actions: ActionLink[] = [];

  if (entityType === "lead" && leadId) {
    actions = leadActions(status as LeadStatus, leadId);
  } else if (entityType === "job" && jobId) {
    actions = jobActions(status as JobStatus, jobId);
  } else if (entityType === "estimate" && estimateId && jobId) {
    actions = estimateActions(status as EstimateStatus, estimateId, jobId);
  }

  if (actions.length === 0) return null;

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">Next Best Actions</h2>
      <div className="space-y-2">
        {actions.map((action) => (
          <Link
            key={action.label}
            href={action.href}
            className={`w-full inline-flex justify-center rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
              action.primary
                ? "bg-sky-600 text-white hover:bg-sky-700"
                : "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
            }`}
          >
            {action.label}
          </Link>
        ))}
      </div>
    </div>
  );
}
