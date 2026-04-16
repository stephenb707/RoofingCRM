"use client";

import { useMemo, useState, useEffect } from "react";
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
import { listPipelineStatuses } from "@/lib/pipelineStatusesApi";
import type { PipelineStatusDefinitionDto } from "@/lib/pipelineStatusesApi";
import { leadStatusBadgeClass } from "@/lib/pipelineStatusVisuals";
import { getApiErrorMessage } from "@/lib/apiError";
import { SOURCE_LABELS } from "@/lib/leadsConstants";
import { queryKeys } from "@/lib/queryKeys";
import { formatAddress, formatPhone } from "@/lib/format";
import type { LeadDto } from "@/lib/types";
import PipelineViewSwitcher from "./PipelineViewSwitcher";

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

function sortActiveLeadColumns(defs: PipelineStatusDefinitionDto[]): PipelineStatusDefinitionDto[] {
  return [...defs].filter((d) => d.active).sort((a, b) => a.sortOrder - b.sortOrder);
}

function leadMatchesSearch(lead: LeadDto, q: string): boolean {
  if (!q.trim()) return true;
  const needle = q.trim().toLowerCase();
  const phoneDigits = lead.customerPhone?.replace(/\D/g, "") ?? "";
  const sourceLabel =
    lead.source != null ? (SOURCE_LABELS[lead.source] ?? String(lead.source)) : "";
  const parts = [
    customerName(lead),
    formatAddress(lead.propertyAddress),
    lead.customerPhone ?? "",
    phoneDigits,
    lead.customerEmail ?? "",
    lead.statusLabel ?? "",
    sourceLabel,
    lead.leadNotes?.trim() ?? "",
  ];
  return parts.some((p) => p.toLowerCase().includes(needle));
}

export type LeadsPipelineVariant = "page" | "embedded";

export default function LeadsPipelineExperience({ variant }: { variant: LeadsPipelineVariant }) {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const [activeLead, setActiveLead] = useState<LeadDto | null>(null);
  const [searchInput, setSearchInput] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");

  useEffect(() => {
    const t = setTimeout(() => setDebouncedSearch(searchInput), 300);
    return () => clearTimeout(t);
  }, [searchInput]);

  const canEditPipeline =
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "OWNER" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "ADMIN" ||
    auth.tenants.find((t) => t.tenantId === auth.selectedTenantId)?.role === "SALES";

  const pipelineKey = queryKeys.leadsPipeline(auth.selectedTenantId);
  const pipelineDefsKey = queryKeys.pipelineStatuses(auth.selectedTenantId, "LEAD");

  const { data: leadDefs = [], isLoading: defsLoading } = useQuery({
    queryKey: pipelineDefsKey,
    queryFn: () => listPipelineStatuses(api, "LEAD"),
    enabled: ready,
  });

  const columnDefs = useMemo(() => sortActiveLeadColumns(leadDefs), [leadDefs]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: pipelineKey,
    queryFn: () => listLeads(api, { page: 0, size: 200 }),
    enabled: ready,
  });

  const defById = useMemo(() => {
    const m = new Map<string, PipelineStatusDefinitionDto>();
    for (const d of leadDefs) m.set(d.id, d);
    return m;
  }, [leadDefs]);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: { distance: 8 },
    })
  );

  const updateStatusMutation = useMutation({
    mutationFn: ({
      leadId,
      statusDefinitionId,
      position,
    }: {
      leadId: string;
      statusDefinitionId: string;
      position?: number;
    }) => updateLeadStatus(api, leadId, statusDefinitionId, position),
    onMutate: async ({ leadId, statusDefinitionId, position }) => {
      await queryClient.cancelQueries({ queryKey: pipelineKey });
      const previous = queryClient.getQueryData<typeof data>(pipelineKey);
      const targetDef = defById.get(statusDefinitionId);
      queryClient.setQueryData(pipelineKey, (old: typeof data) => {
        if (!old || !targetDef) return old;
        const lead = old.content.find((l) => l.id === leadId);
        if (!lead) return old;
        const oldDefId = lead.statusDefinitionId;
        const withoutMoved = old.content.filter((l) => l.id !== leadId);
        const targetCol = withoutMoved.filter((l) => l.statusDefinitionId === statusDefinitionId);
        const insertIdx =
          position !== undefined && position >= 0 && position <= targetCol.length
            ? position
            : targetCol.length;
        const moved: LeadDto = {
          ...lead,
          statusDefinitionId,
          statusKey: targetDef.systemKey,
          statusLabel: targetDef.label,
          pipelinePosition: insertIdx,
        };
        const targetWithMoved = [...targetCol];
        targetWithMoved.splice(insertIdx, 0, moved);
        const reseqTarget = targetWithMoved.map((l, i) => ({ ...l, pipelinePosition: i }));
        const isSameColumn = oldDefId === statusDefinitionId;
        const oldColReseq = isSameColumn
          ? []
          : withoutMoved
              .filter((l) => l.statusDefinitionId === oldDefId)
              .sort((a, b) => (a.pipelinePosition ?? 0) - (b.pipelinePosition ?? 0))
              .map((l, i) => ({ ...l, pipelinePosition: i }));
        const otherCols = withoutMoved.filter(
          (l) => l.statusDefinitionId !== oldDefId && l.statusDefinitionId !== statusDefinitionId
        );
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

  const fullLeadsByDefinitionId = useMemo(() => {
    const leads = data?.content ?? [];
    const byId = new Map<string, LeadDto[]>();
    for (const d of columnDefs) {
      byId.set(d.id, []);
    }
    for (const lead of leads) {
      const list = byId.get(lead.statusDefinitionId) ?? [];
      list.push(lead);
      byId.set(lead.statusDefinitionId, list);
    }
    for (const d of columnDefs) {
      const list = byId.get(d.id) ?? [];
      byId.set(d.id, sortLeadsByPosition(list));
    }
    return byId;
  }, [data?.content, columnDefs]);

  const filteredContent = useMemo(() => {
    const leads = data?.content ?? [];
    return leads.filter((l) => leadMatchesSearch(l, debouncedSearch));
  }, [data?.content, debouncedSearch]);

  const leadsByDefinitionId = useMemo(() => {
    const byId = new Map<string, LeadDto[]>();
    for (const d of columnDefs) {
      byId.set(d.id, []);
    }
    for (const lead of filteredContent) {
      const list = byId.get(lead.statusDefinitionId) ?? [];
      list.push(lead);
      byId.set(lead.statusDefinitionId, list);
    }
    for (const d of columnDefs) {
      const list = byId.get(d.id) ?? [];
      byId.set(d.id, sortLeadsByPosition(list));
    }
    return byId;
  }, [filteredContent, columnDefs]);

  const handleStatusChange = (leadId: string, statusDefinitionId: string, position?: number) => {
    updateStatusMutation.mutate({ leadId, statusDefinitionId, position });
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
    let newDefId: string;
    let newIndex: number;

    const colIds = new Set(columnDefs.map((c) => c.id));
    if (colIds.has(overId)) {
      newDefId = overId;
      const targetCol = fullLeadsByDefinitionId.get(newDefId) ?? [];
      newIndex =
        lead.statusDefinitionId === newDefId
          ? Math.max(0, targetCol.findIndex((l) => l.id === leadId))
          : targetCol.length;
    } else if (overId.startsWith("lead-")) {
      const overLeadId = overId.replace("lead-", "");
      const overLead = (data?.content ?? []).find((l) => l.id === overLeadId);
      if (!overLead) return;
      newDefId = overLead.statusDefinitionId;
      const targetCol = fullLeadsByDefinitionId.get(newDefId) ?? [];
      const overIdx = targetCol.findIndex((l) => l.id === overLeadId);
      newIndex = overIdx >= 0 ? overIdx : targetCol.length;
    } else {
      return;
    }

    if (lead.statusDefinitionId === newDefId) {
      const currentCol = fullLeadsByDefinitionId.get(lead.statusDefinitionId) ?? [];
      const fromIdx = currentCol.findIndex((l) => l.id === leadId);
      if (fromIdx === newIndex) return;
    }

    handleStatusChange(leadId, newDefId, newIndex);
  };

  const loadingBlock = (
    <div
      className={
        variant === "page"
          ? "bg-white rounded-xl border border-slate-200 p-12 text-center"
          : "bg-white rounded-lg border border-slate-200 p-8 text-center"
      }
    >
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
      <p className="text-sm text-slate-500">Loading lead pipeline…</p>
    </div>
  );

  if (defsLoading || isLoading) {
    if (variant === "page") {
      return <div className="max-w-7xl mx-auto">{loadingBlock}</div>;
    }
    return (
      <section data-testid="combined-pipeline-lead-section">{loadingBlock}</section>
    );
  }

  if (isError) {
    const errBody = (
      <div className="bg-red-50 border border-red-200 rounded-xl p-6">
        <h3 className="text-sm font-medium text-red-800">Failed to load lead pipeline</h3>
        <p className="text-sm text-red-600 mt-1">
          {getApiErrorMessage(error, "An error occurred. Please try again.")}
        </p>
      </div>
    );
    if (variant === "page") {
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
          {errBody}
        </div>
      );
    }
    return (
      <section data-testid="combined-pipeline-lead-section">{errBody}</section>
    );
  }

  const board = (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className="flex overflow-x-auto pb-4 gap-3">
        {columnDefs.map((def) => (
          <PipelineColumn
            key={def.id}
            definition={def}
            leads={leadsByDefinitionId.get(def.id) ?? []}
            canEdit={canEditPipeline}
          />
        ))}
      </div>

      <DragOverlay>
        {activeLead ? (
          <div className="bg-white rounded-lg border-2 border-sky-400 shadow-xl opacity-95 p-3 w-52">
            <div className="font-medium text-slate-800 text-sm truncate">{customerName(activeLead)}</div>
            <div className="text-xs text-slate-600 truncate">
              {formatAddress(activeLead.propertyAddress)}
            </div>
            {activeLead.customerPhone?.trim() ? (
              <div className="text-xs text-slate-500 mt-0.5 truncate">
                {formatPhone(activeLead.customerPhone)}
              </div>
            ) : null}
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );

  if (variant === "embedded") {
    return (
      <section
        className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm"
        data-testid="combined-pipeline-lead-section"
      >
        <h2 className="text-lg font-semibold text-slate-800">Lead pipeline</h2>
        <p className="text-sm text-slate-500 mt-1">
          Group leads by stage and move them through your funnel
          {!canEditPipeline && (
            <span className="ml-2 text-amber-600 font-medium">(Read-only)</span>
          )}
        </p>
        <div className="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-3 mt-4">
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search customer, address…"
            className="border border-slate-300 rounded-lg px-3 py-2 text-sm w-full sm:w-64 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            aria-label="Search leads in pipeline"
          />
          <Link
            href="/app/leads"
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors text-center"
          >
            Back to Leads
          </Link>
          <Link
            href="/app/leads/new"
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors text-center"
          >
            + New Lead
          </Link>
        </div>
        <div className="mt-4">{board}</div>
      </section>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between mb-4">
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
        <div className="flex flex-col sm:flex-row sm:flex-wrap sm:items-center gap-3">
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search customer, address…"
            className="border border-slate-300 rounded-lg px-3 py-2 text-sm w-full sm:w-64 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
            aria-label="Search leads in pipeline"
          />
          <Link
            href="/app/leads"
            className="px-4 py-2 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors text-center"
          >
            Back to Leads
          </Link>
          <Link
            href="/app/leads/new"
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg transition-colors text-center"
          >
            + New Lead
          </Link>
        </div>
      </div>

      <div className="mb-6">
        <PipelineViewSwitcher />
      </div>

      {board}
    </div>
  );
}

function PipelineColumn({
  definition,
  leads,
  canEdit,
}: {
  definition: PipelineStatusDefinitionDto;
  leads: LeadDto[];
  canEdit: boolean;
}) {
  const { setNodeRef, isOver } = useDroppable({ id: definition.id });

  return (
    <div
      ref={setNodeRef}
      className={`flex-shrink-0 w-56 bg-slate-50 rounded-xl border-2 overflow-hidden transition-colors ${
        isOver ? "border-sky-400 bg-sky-50/50" : "border-slate-200"
      }`}
      data-testid={`pipeline-col-${definition.id}`}
    >
      <div className={`border-b border-slate-200 ${leadStatusBadgeClass(definition.systemKey)} rounded-t-xl p-3`}>
        <h2 className="font-semibold text-slate-800 text-sm">{definition.label}</h2>
        <p className="text-xs text-slate-600 mt-0.5">{leads.length} leads</p>
      </div>
      <div className="max-h-[calc(100vh-280px)] overflow-y-auto p-2 space-y-2">
        {leads.length === 0 && (
          <p className="text-xs text-slate-400 text-center py-6 px-2">No leads in this stage</p>
        )}
        {leads.map((lead) => (
          <PipelineLeadCard key={lead.id} lead={lead} canEdit={canEdit} />
        ))}
      </div>
    </div>
  );
}

function PipelineLeadCard({ lead, canEdit }: { lead: LeadDto; canEdit: boolean }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `lead-${lead.id}`,
    data: { lead },
    disabled: !canEdit,
  });

  return (
    <div
      ref={setNodeRef}
      {...(canEdit ? { ...listeners, ...attributes } : {})}
      className={`bg-white rounded-lg border border-slate-200 shadow-sm p-3 ${
        canEdit ? "cursor-grab active:cursor-grabbing touch-manipulation" : ""
      } ${isDragging ? "opacity-50" : ""}`}
      data-testid={`pipeline-card-${lead.id}`}
    >
      <div className="font-medium text-slate-800 truncate text-sm">{customerName(lead)}</div>
      <div className="text-xs text-slate-600 mt-1 truncate" title={formatAddress(lead.propertyAddress)}>
        {formatAddress(lead.propertyAddress)}
      </div>
      {lead.customerPhone?.trim() ? (
        <div className="text-xs text-slate-500 mt-0.5 truncate" title={formatPhone(lead.customerPhone)}>
          {formatPhone(lead.customerPhone)}
        </div>
      ) : null}
      {lead.source && (
        <div className="text-xs text-slate-500 mt-0.5">
          {SOURCE_LABELS[lead.source] ?? lead.source}
        </div>
      )}
      <div className="flex items-center justify-between gap-2 mt-2">
        <span
          className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full border ${leadStatusBadgeClass(lead.statusKey)}`}
          data-testid={`pipeline-card-status-${lead.id}`}
        >
          {lead.statusLabel}
        </span>
        <Link
          href={`/app/leads/${lead.id}`}
          onPointerDown={(e) => e.stopPropagation()}
          className="text-xs font-medium text-sky-600 hover:text-sky-700 px-2 py-1 shrink-0 relative z-10"
        >
          Open
        </Link>
      </div>
    </div>
  );
}
