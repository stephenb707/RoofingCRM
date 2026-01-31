"use client";

import { useMemo } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { listLeads, updateLeadStatus } from "@/lib/leadsApi";
import { getApiErrorMessage } from "@/lib/apiError";
import {
  LEAD_STATUSES,
  STATUS_LABELS,
  STATUS_COLORS,
  SOURCE_LABELS,
} from "@/lib/leadsConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatAddress } from "@/lib/format";
import type { LeadDto, LeadStatus } from "@/lib/types";

function customerName(lead: LeadDto): string {
  const parts = [lead.customerFirstName, lead.customerLastName].filter(Boolean);
  return parts.length > 0 ? parts.join(" ") : "—";
}

export default function LeadsPipelinePage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const pipelineKey = queryKeys.leadsPipeline(auth.selectedTenantId);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: pipelineKey,
    queryFn: () => listLeads(api, { page: 0, size: 200 }),
    enabled: ready,
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ leadId, status }: { leadId: string; status: LeadStatus }) =>
      updateLeadStatus(api, leadId, status),
    onMutate: async ({ leadId, status }) => {
      await queryClient.cancelQueries({ queryKey: pipelineKey });
      const previous = queryClient.getQueryData<typeof data>(pipelineKey);
      queryClient.setQueryData(pipelineKey, (old: typeof data) => {
        if (!old) return old;
        return {
          ...old,
          content: old.content.map((l) =>
            l.id === leadId ? { ...l, status } : l
          ),
        };
      });
      return { previous };
    },
    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(pipelineKey, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: pipelineKey });
    },
  });

  const leadsByStatus = useMemo(() => {
    const leads = data?.content ?? [];
    const byStatus = new Map<LeadStatus, LeadDto[]>();
    for (const s of LEAD_STATUSES) {
      byStatus.set(s, []);
    }
    for (const lead of leads) {
      const list = byStatus.get(lead.status) ?? [];
      list.push(lead);
      byStatus.set(lead.status, list);
    }
    return byStatus;
  }, [data?.content]);

  const handleStatusChange = (leadId: string, newStatus: LeadStatus) => {
    updateStatusMutation.mutate({ leadId, status: newStatus });
  };

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading pipeline…</p>
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-7xl mx-auto">
        <Link
          href="/app/leads"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Leads
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load pipeline</h3>
          <p className="text-sm text-red-600 mt-1">
            {getApiErrorMessage(error, "An error occurred. Please try again.")}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <div>
          <Link
            href="/app/leads"
            className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
            Back to Leads
          </Link>
          <h1 className="text-2xl font-bold text-slate-800">Pipeline</h1>
          <p className="text-sm text-slate-500 mt-1">
            Group leads by stage and move them through your funnel
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            href="/app/leads"
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
          >
            Back to Leads
          </Link>
          <Link
            href="/app/leads/new"
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            + New Lead
          </Link>
        </div>
      </div>

      <div className="flex gap-4 overflow-x-auto pb-4">
        {LEAD_STATUSES.map((status) => {
          const columnLeads = leadsByStatus.get(status) ?? [];
          return (
            <div
              key={status}
              className="flex-shrink-0 w-72 bg-slate-50 rounded-xl border border-slate-200 overflow-hidden"
            >
              <div className={`p-4 border-b border-slate-200 ${STATUS_COLORS[status]} rounded-t-xl`}>
                <h2 className="font-semibold text-slate-800">{STATUS_LABELS[status]}</h2>
                <p className="text-xs text-slate-600 mt-0.5">{columnLeads.length} leads</p>
              </div>
              <div className="p-3 space-y-3 max-h-[calc(100vh-280px)] overflow-y-auto">
                {columnLeads.map((lead) => (
                  <div
                    key={lead.id}
                    className="bg-white rounded-lg border border-slate-200 p-4 shadow-sm"
                  >
                    <div className="font-medium text-slate-800 truncate">
                      {customerName(lead)}
                    </div>
                    <div className="text-xs text-slate-600 mt-1 truncate" title={formatAddress(lead.propertyAddress)}>
                      {formatAddress(lead.propertyAddress)}
                    </div>
                    {lead.source && (
                      <div className="text-xs text-slate-500 mt-0.5">
                        {SOURCE_LABELS[lead.source] ?? lead.source}
                      </div>
                    )}
                    <div className="mt-3 flex items-center gap-2">
                      <select
                        value={lead.status}
                        onChange={(e) =>
                          handleStatusChange(lead.id, e.target.value as LeadStatus)
                        }
                        disabled={updateStatusMutation.isPending}
                        className="flex-1 text-xs border border-slate-300 rounded px-2 py-1.5 bg-white focus:outline-none focus:ring-1 focus:ring-sky-500"
                        aria-label={`Status for ${customerName(lead)}`}
                      >
                        {LEAD_STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {STATUS_LABELS[s]}
                          </option>
                        ))}
                      </select>
                      <Link
                        href={`/app/leads/${lead.id}`}
                        className="text-xs font-medium text-sky-600 hover:text-sky-700 px-2 py-1.5"
                      >
                        Open
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
