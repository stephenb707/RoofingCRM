"use client";

import Link from "next/link";
import type { EstimateStatus } from "@/lib/types";

type EntityType = "lead" | "job" | "estimate";

/** Wired from Estimate Details so NBA can open the same share flows as the Share card. */
export interface EstimateShareActionsCallbacks {
  onSendEmail: () => void;
  onGenerateLink: () => void;
}

interface NextBestActionsProps {
  entityType: EntityType;
  status: string;
  leadId?: string;
  leadConvertedJobId?: string | null;
  /** For lead "Call customer" (customer profile) and estimate "Set follow-up" task prefill */
  customerId?: string | null;
  jobId?: string;
  estimateId?: string;
  /** When set (e.g. estimate detail + canShare), adds Send Email / Generate Link buttons that call into EstimateSharePanel. */
  estimateShareActions?: EstimateShareActionsCallbacks | null;
}

type LinkAction = {
  kind: "link";
  label: string;
  href: string;
  primary?: boolean;
};

type ButtonAction = {
  kind: "button";
  label: string;
  onClick: () => void;
  primary?: boolean;
  testId?: string;
};

type NextAction = LinkAction | ButtonAction;

function leadActions(
  statusKey: string,
  leadId: string,
  convertedJobId: string | null | undefined,
  customerId: string | null | undefined
): LinkAction[] {
  const canConvert = statusKey !== "LOST" && !convertedJobId;
  const callCustomerHref =
    customerId != null && customerId !== ""
      ? `/app/customers/${customerId}`
      : `/app/leads/${leadId}`;
  if (statusKey === "NEW") {
    const secondary: LinkAction[] = [
      { kind: "link", label: "Call customer", href: callCustomerHref },
      { kind: "link", label: "Schedule inspection", href: `/app/tasks/new?leadId=${leadId}` },
    ];
    if (canConvert) {
      return [
        { kind: "link", label: "Convert to job", href: `/app/leads/${leadId}/convert`, primary: true },
        ...secondary,
      ];
    }
    return secondary;
  }
  return [{ kind: "link", label: "Create task", href: `/app/tasks/new?leadId=${leadId}`, primary: true }];
}

function estimateShareButtons(share: EstimateShareActionsCallbacks): ButtonAction[] {
  return [
    {
      kind: "button",
      label: "Send Email",
      onClick: share.onSendEmail,
      primary: true,
      testId: "nba-estimate-send-email",
    },
    {
      kind: "button",
      label: "Generate Link",
      onClick: share.onGenerateLink,
      testId: "nba-estimate-generate-link",
    },
  ];
}

function estimateActions(
  status: EstimateStatus,
  estimateId: string,
  jobId: string,
  customerId: string | null | undefined,
  share: EstimateShareActionsCallbacks | null | undefined
): NextAction[] {
  const followUpHref =
    customerId != null && customerId !== ""
      ? `/app/tasks/new?jobId=${encodeURIComponent(jobId)}&customerId=${encodeURIComponent(customerId)}`
      : `/app/tasks/new?jobId=${encodeURIComponent(jobId)}`;
  const shareBtns = share ? estimateShareButtons(share) : [];

  if (status === "DRAFT") {
    return [
      ...shareBtns,
      {
        kind: "link",
        label: "Edit estimate",
        href: `/app/estimates/${estimateId}/edit`,
        primary: shareBtns.length === 0,
      },
    ];
  }
  if (status === "SENT") {
    return [
      ...shareBtns,
      {
        kind: "link",
        label: "Set follow-up",
        href: followUpHref,
        primary: shareBtns.length === 0,
      },
    ];
  }
  return [
    ...shareBtns,
    {
      kind: "link",
      label: "View job",
      href: `/app/jobs/${jobId}`,
      primary: shareBtns.length === 0,
    },
  ];
}

function jobActions(statusKey: string, jobId: string): LinkAction[] {
  const taskNew = `/app/tasks/new?jobId=${encodeURIComponent(jobId)}`;
  if (statusKey === "UNSCHEDULED") {
    return [
      {
        kind: "link",
        label: "Schedule",
        href: `/app/schedule?focusJob=${encodeURIComponent(jobId)}`,
        primary: true,
      },
      { kind: "link", label: "Create estimate", href: `/app/jobs/${jobId}/estimates/new` },
      { kind: "link", label: "Create task", href: taskNew },
    ];
  }
  if (statusKey === "SCHEDULED") {
    return [
      { kind: "link", label: "Create task", href: taskNew, primary: true },
      { kind: "link", label: "Assign crew", href: `/app/jobs/${jobId}/edit` },
      { kind: "link", label: "Upload photos", href: `/app/jobs/${jobId}#attachments` },
    ];
  }
  return [
    { kind: "link", label: "Create task", href: taskNew, primary: true },
    { kind: "link", label: "View estimates", href: `/app/jobs/${jobId}/estimates` },
  ];
}

export function NextBestActions({
  entityType,
  status,
  leadId,
  leadConvertedJobId,
  customerId,
  jobId,
  estimateId,
  estimateShareActions,
}: NextBestActionsProps) {
  let actions: NextAction[] = [];

  if (entityType === "lead" && leadId) {
    actions = leadActions(status, leadId, leadConvertedJobId, customerId);
  } else if (entityType === "job" && jobId) {
    actions = jobActions(status, jobId);
  } else if (entityType === "estimate" && estimateId && jobId) {
    actions = estimateActions(
      status as EstimateStatus,
      estimateId,
      jobId,
      customerId,
      estimateShareActions ?? null
    );
  }

  if (actions.length === 0) return null;

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">Next Best Actions</h2>
      <div className="space-y-2">
        {actions.map((action) => {
          const className = `w-full inline-flex justify-center rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
            action.primary
              ? "bg-sky-600 text-white hover:bg-sky-700"
              : "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50"
          }`;
          if (action.kind === "link") {
            return (
              <Link key={`${action.kind}-${action.href}-${action.label}`} href={action.href} className={className}>
                {action.label}
              </Link>
            );
          }
          return (
            <button
              key={`${action.kind}-${action.testId ?? action.label}`}
              type="button"
              data-testid={action.testId}
              onClick={action.onClick}
              className={className}
            >
              {action.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
