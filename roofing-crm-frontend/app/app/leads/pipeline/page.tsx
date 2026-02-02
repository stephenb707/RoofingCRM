"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  DndContext,
  DragEndEvent,
  DragOverlay,
  DragStartEvent,
  PointerSensor,
  useSensor,
  useSensors,
  useDraggable,
  useDroppable,
} from "@dnd-kit/core";
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

function sortLeadsByPosition(leads: LeadDto[]): LeadDto[] {
  return [...leads].sort((a, b) => {
    const posA = a.pipelinePosition ?? 0;
    const posB = b.pipelinePosition ?? 0;
    if (posA !== posB) return posA - posB;
    return (a.createdAt ?? "").localeCompare(b.createdAt ?? "");
  });
}

export default function LeadsPipelinePage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const [activeLead, setActiveLead] = useState<LeadDto | null>(null);
  const [compact, setCompact] = useState(true);

  const canEditPipeline =
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "OWNER" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "ADMIN" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "SALES";

  const pipelineKey = queryKeys.leadsPipeline(auth.selectedTenantId);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: pipelineKey,
    queryFn: () => listLeads(api, { page: 0, size: 200 }),
    enabled: ready,
  });

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    })
  );

  const updateStatusMutation = useMutation({
    mutationFn: ({
      leadId,
      status,
      position,
    }: {
      leadId: string;
      status: LeadStatus;
      position?: number;
    }) => updateLeadStatus(api, leadId, status, position),
    onMutate: async ({ leadId, status, position }) => {
      await queryClient.cancelQueries({ queryKey: pipelineKey });
      const previous = queryClient.getQueryData<typeof data>(pipelineKey);
      queryClient.setQueryData(pipelineKey, (old: typeof data) => {
        if (!old) return old;
        const lead = old.content.find((l) => l.id === leadId);
        if (!lead) return old;
        const oldStatus = lead.status;
        const withoutMoved = old.content.filter((l) => l.id !== leadId);
        const targetCol = withoutMoved.filter((l) => l.status === status);
        const insertIdx =
          position !== undefined && position >= 0 && position <= targetCol.length
            ? position
            : targetCol.length;
        const moved: LeadDto = { ...lead, status, pipelinePosition: insertIdx };
        const targetWithMoved = [...targetCol];
        targetWithMoved.splice(insertIdx, 0, moved);
        const reseqTarget = targetWithMoved.map((l, i) => ({ ...l, pipelinePosition: i }));
        const isSameColumn = oldStatus === status;
        const oldColReseq = isSameColumn
          ? []
          : withoutMoved
              .filter((l) => l.status === oldStatus)
              .sort((a, b) => (a.pipelinePosition ?? 0) - (b.pipelinePosition ?? 0))
              .map((l, i) => ({ ...l, pipelinePosition: i }));
        const otherCols = withoutMoved.filter((l) => l.status !== oldStatus && l.status !== status);
        const newContent = [...otherCols, ...oldColReseq, ...reseqTarget];
        return { ...old, content: newContent };
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
    for (const s of LEAD_STATUSES) {
      const list = byStatus.get(s) ?? [];
      byStatus.set(s, sortLeadsByPosition(list));
    }
    return byStatus;
  }, [data?.content]);

  const handleStatusChange = (leadId: string, newStatus: LeadStatus, position?: number) => {
    updateStatusMutation.mutate({ leadId, status: newStatus, position });
  };

  const handleDragStart = (event: DragStartEvent) => {
    const leadId = String(event.active.id).replace("lead-", "");
    const lead = (data?.content ?? []).find((l) => l.id === leadId);
    if (lead) setActiveLead(lead);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    setActiveLead(null);
    const { active, over } = event;
    if (!over || !canEditPipeline) return;
    const leadId = String(active.id).replace("lead-", "");
    const lead = (data?.content ?? []).find((l) => l.id === leadId);
    if (!lead) return;

    const overId = String(over.id);
    let newStatus: LeadStatus;
    let newIndex: number;

    if (LEAD_STATUSES.includes(overId as LeadStatus)) {
      newStatus = overId as LeadStatus;
      const targetCol = leadsByStatus.get(newStatus) ?? [];
      newIndex = lead.status === newStatus
        ? Math.max(0, targetCol.findIndex((l) => l.id === leadId))
        : targetCol.length;
    } else if (overId.startsWith("lead-")) {
      const overLeadId = overId.replace("lead-", "");
      const overLead = (data?.content ?? []).find((l) => l.id === overLeadId);
      if (!overLead) return;
      newStatus = overLead.status;
      const targetCol = leadsByStatus.get(newStatus) ?? [];
      const overIdx = targetCol.findIndex((l) => l.id === overLeadId);
      newIndex = overIdx >= 0 ? overIdx : targetCol.length;
    } else {
      return;
    }

    if (lead.status === newStatus) {
      const currentCol = leadsByStatus.get(lead.status) ?? [];
      const fromIdx = currentCol.findIndex((l) => l.id === leadId);
      if (fromIdx === newIndex) return;
    }

    handleStatusChange(leadId, newStatus, newIndex);
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
            {!canEditPipeline && (
              <span className="ml-2 text-amber-600 font-medium">(Read-only)</span>
            )}
          </p>
          <p className="text-xs text-slate-500 mt-2">
            Tip: Drag leads to move them between stages. For precise edits,{" "}
            <Link href="/app/leads" className="text-sky-600 hover:text-sky-700 underline">
              open a lead
            </Link>
            .
          </p>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-700 cursor-pointer">
            <input
              type="checkbox"
              checked={compact}
              onChange={(e) => setCompact(e.target.checked)}
              className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
            />
            Compact view
          </label>
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

      <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div className={`flex overflow-x-auto pb-4 ${compact ? "gap-3" : "gap-4"}`}>
          {LEAD_STATUSES.map((status) => (
            <PipelineColumn
              key={status}
              status={status}
              leads={leadsByStatus.get(status) ?? []}
              canEdit={canEditPipeline}
              compact={compact}
            />
          ))}
        </div>

        <DragOverlay>
          {activeLead ? (
            <div className={`bg-white rounded-lg border-2 border-sky-400 shadow-xl opacity-95 ${compact ? "p-3 w-52" : "p-4 w-64"}`}>
              <div className="font-medium text-slate-800 truncate">{customerName(activeLead)}</div>
              <div className="text-xs text-slate-600 truncate">
                {formatAddress(activeLead.propertyAddress)}
              </div>
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>
    </div>
  );
}

function PipelineColumn({
  status,
  leads,
  canEdit,
  compact,
}: {
  status: LeadStatus;
  leads: LeadDto[];
  canEdit: boolean;
  compact: boolean;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: status as string });

  return (
    <div
      ref={setNodeRef}
      className={`flex-shrink-0 bg-slate-50 rounded-xl border-2 overflow-hidden transition-colors ${
        compact ? "w-56" : "w-72"
      } ${isOver ? "border-sky-400 bg-sky-50/50" : "border-slate-200"}`}
      data-testid={`pipeline-col-${status}`}
    >
      <div className={`border-b border-slate-200 ${STATUS_COLORS[status]} rounded-t-xl ${compact ? "p-3" : "p-4"}`}>
        <h2 className={`font-semibold text-slate-800 ${compact ? "text-sm" : ""}`}>{STATUS_LABELS[status]}</h2>
        <p className="text-xs text-slate-600 mt-0.5">{leads.length} leads</p>
      </div>
      <div className={`max-h-[calc(100vh-280px)] overflow-y-auto ${compact ? "p-2 space-y-2" : "p-3 space-y-3"}`}>
        {leads.map((lead, index) => (
          <PipelineLeadCard
            key={lead.id}
            lead={lead}
            index={index}
            canEdit={canEdit}
            compact={compact}
          />
        ))}
      </div>
    </div>
  );
}

function PipelineLeadCard({
  lead,
  index,
  canEdit,
  compact,
}: {
  lead: LeadDto;
  index: number;
  canEdit: boolean;
  compact: boolean;
}) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `lead-${lead.id}`,
    data: { lead },
    disabled: !canEdit,
  });

  return (
    <div
      ref={setNodeRef}
      className={`bg-white rounded-lg border border-slate-200 shadow-sm ${
        compact ? "p-3" : "p-4"
      } ${isDragging ? "opacity-50" : ""}`}
      data-testid={`pipeline-card-${lead.id}`}
    >
      {(canEdit ? (
        <div
          {...listeners}
          {...attributes}
          className="cursor-grab active:cursor-grabbing mb-1.5 p-1 -m-1 rounded text-slate-400 hover:text-slate-600 hover:bg-slate-100 w-fit"
          aria-label="Drag to reorder"
        >
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 16 16">
            <path d="M4 3a1 1 0 000 2h8a1 1 0 100-2H4zm0 4a1 1 0 000 2h8a1 1 0 100-2H4zm0 4a1 1 0 100 2h8a1 1 0 100-2H4z" />
          </svg>
        </div>
      ) : null)}
      <div className={`font-medium text-slate-800 truncate ${compact ? "text-sm" : ""}`}>{customerName(lead)}</div>
      <div className="text-xs text-slate-600 mt-1 truncate" title={formatAddress(lead.propertyAddress)}>
        {formatAddress(lead.propertyAddress)}
      </div>
      {lead.source && (
        <div className="text-xs text-slate-500 mt-0.5">
          {SOURCE_LABELS[lead.source] ?? lead.source}
        </div>
      )}
      <div className={`flex items-center justify-between gap-2 ${compact ? "mt-2" : "mt-3"}`}>
        <span
          className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${STATUS_COLORS[lead.status]}`}
          data-testid={`pipeline-card-status-${lead.id}`}
        >
          {STATUS_LABELS[lead.status]}
        </span>
        <Link
          href={`/app/leads/${lead.id}`}
          className="text-xs font-medium text-sky-600 hover:text-sky-700 px-2 py-1"
        >
          Open
        </Link>
      </div>
    </div>
  );
}
