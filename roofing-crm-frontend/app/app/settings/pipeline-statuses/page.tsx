"use client";

import { useEffect, useMemo, useState } from "react";
import SettingsBackLink from "../SettingsBackLink";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { invalidatePipelineRelatedQueries } from "@/lib/pipelineQueryInvalidation";
import {
  createSettingsPipelineStatus,
  deactivateSettingsPipelineStatus,
  listSettingsPipelineStatuses,
  reorderSettingsPipelineStatuses,
  restoreDefaultPipelineStatuses,
  updateSettingsPipelineStatus,
  type PipelineStatusDefinitionDto,
  type PipelineTypeApi,
} from "@/lib/pipelineStatusesApi";

function StatusRow({
  def,
  onSave,
  onMoveUp,
  onMoveDown,
  onDeactivate,
  busy,
  canMoveUp,
  canMoveDown,
}: {
  def: PipelineStatusDefinitionDto;
  onSave: (label: string) => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  onDeactivate: () => void;
  busy: boolean;
  canMoveUp: boolean;
  canMoveDown: boolean;
}) {
  const [label, setLabel] = useState(def.label);
  useEffect(() => {
    setLabel(def.label);
  }, [def.id, def.label]);

  const trimmed = label.trim();
  const dirty = trimmed !== def.label.trim();
  const canDeactivate = !def.builtIn && def.active;

  return (
    <div
      className={`flex flex-wrap items-center gap-2 py-3 border-b border-slate-100 ${
        def.active ? "" : "opacity-60"
      }`}
      data-testid={`pipeline-status-row-${def.id}`}
    >
      <div className="flex flex-col gap-1 min-w-[200px] flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <input
            type="text"
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            disabled={busy}
            className="flex-1 min-w-[160px] rounded-md border border-slate-300 px-2 py-1.5 text-sm"
            aria-label={`Label for status ${def.systemKey}`}
          />
          <button
            type="button"
            disabled={busy || !dirty || !trimmed}
            onClick={() => onSave(trimmed)}
            className="px-2.5 py-1 text-xs font-medium rounded-md bg-sky-600 text-white disabled:opacity-40"
          >
            Save
          </button>
        </div>
        <div className="flex gap-2 text-xs text-slate-500">
          <span
            className={`font-medium ${
              def.builtIn ? "text-slate-600" : "text-amber-700"
            }`}
            data-testid={`status-kind-${def.id}`}
          >
            {def.builtIn ? "Built-in" : "Custom"}
          </span>
          {!def.active && (
            <span className="text-slate-400" data-testid={`status-inactive-${def.id}`}>
              Inactive
            </span>
          )}
          <span className="font-mono text-[10px] text-slate-400">{def.systemKey}</span>
        </div>
      </div>
      <div className="flex items-center gap-1">
        <button
          type="button"
          disabled={busy || !canMoveUp}
          onClick={onMoveUp}
          className="px-2 py-1 text-xs border border-slate-200 rounded-md hover:bg-slate-50 disabled:opacity-40"
          aria-label="Move up"
        >
          Up
        </button>
        <button
          type="button"
          disabled={busy || !canMoveDown}
          onClick={onMoveDown}
          className="px-2 py-1 text-xs border border-slate-200 rounded-md hover:bg-slate-50 disabled:opacity-40"
          aria-label="Move down"
        >
          Down
        </button>
        {canDeactivate && (
          <button
            type="button"
            disabled={busy}
            onClick={onDeactivate}
            className="px-2 py-1 text-xs border border-red-200 text-red-700 rounded-md hover:bg-red-50 disabled:opacity-40"
          >
            Deactivate
          </button>
        )}
      </div>
    </div>
  );
}

function PipelineStatusSection({ pipelineType }: { pipelineType: PipelineTypeApi }) {
  const { auth, api } = useAuthReady();
  const tenantId = auth.selectedTenantId;
  const queryClient = useQueryClient();
  const [addLabel, setAddLabel] = useState("");
  const [sectionError, setSectionError] = useState<string | null>(null);
  const [restoreOpen, setRestoreOpen] = useState(false);
  const [deactivateUnusedOnRestore, setDeactivateUnusedOnRestore] = useState(false);

  const listQuery = useQuery({
    queryKey: queryKeys.settingsPipelineStatuses(tenantId, pipelineType),
    queryFn: () => listSettingsPipelineStatuses(api, pipelineType),
    enabled: !!tenantId,
  });

  const sorted = useMemo(() => {
    const rows = listQuery.data ?? [];
    return [...rows].sort((a, b) => a.sortOrder - b.sortOrder);
  }, [listQuery.data]);

  const afterMutation = () => {
    invalidatePipelineRelatedQueries(queryClient, tenantId);
  };

  const updateMutation = useMutation({
    mutationFn: ({ id, label }: { id: string; label: string }) =>
      updateSettingsPipelineStatus(api, id, { label }),
    onSuccess: async () => {
      setSectionError(null);
      await listQuery.refetch();
      afterMutation();
    },
    onError: (err) => setSectionError(getApiErrorMessage(err, "Update failed")),
  });

  const createMutation = useMutation({
    mutationFn: (label: string) =>
      createSettingsPipelineStatus(api, { pipelineType, label }),
    onSuccess: async () => {
      setAddLabel("");
      setSectionError(null);
      await listQuery.refetch();
      afterMutation();
    },
    onError: (err) => setSectionError(getApiErrorMessage(err, "Could not add status")),
  });

  const reorderMutation = useMutation({
    mutationFn: (orderedDefinitionIds: string[]) =>
      reorderSettingsPipelineStatuses(api, { pipelineType, orderedDefinitionIds }),
    onSuccess: async () => {
      setSectionError(null);
      await listQuery.refetch();
      afterMutation();
    },
    onError: (err) => setSectionError(getApiErrorMessage(err, "Reorder failed")),
  });

  const restoreMutation = useMutation({
    mutationFn: () =>
      restoreDefaultPipelineStatuses(api, pipelineType, deactivateUnusedOnRestore),
    onSuccess: async () => {
      setSectionError(null);
      setRestoreOpen(false);
      setDeactivateUnusedOnRestore(false);
      await listQuery.refetch();
      afterMutation();
    },
    onError: (err) => setSectionError(getApiErrorMessage(err, "Restore failed")),
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => deactivateSettingsPipelineStatus(api, id),
    onSuccess: async () => {
      setSectionError(null);
      await listQuery.refetch();
      afterMutation();
    },
    onError: (err) =>
      setSectionError(getApiErrorMessage(err, "Could not deactivate status")),
  });

  function move(index: number, delta: number) {
    const nextIndex = index + delta;
    if (nextIndex < 0 || nextIndex >= sorted.length) return;
    const next = [...sorted];
    const a = next[index]!;
    const b = next[nextIndex]!;
    next[index] = b;
    next[nextIndex] = a;
    reorderMutation.mutate(next.map((r) => r.id));
  }

  const busy =
    updateMutation.isPending ||
    createMutation.isPending ||
    reorderMutation.isPending ||
    restoreMutation.isPending ||
    deactivateMutation.isPending;

  const title = pipelineType === "LEAD" ? "Lead statuses" : "Job statuses";
  const testId =
    pipelineType === "LEAD" ? "pipeline-settings-lead-section" : "pipeline-settings-job-section";

  return (
    <section
      className="bg-white border border-slate-200 rounded-lg shadow-sm p-5"
      data-testid={testId}
    >
      <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
        <h2 className="text-lg font-semibold text-slate-800">{title}</h2>
        <button
          type="button"
          disabled={busy}
          onClick={() => setRestoreOpen(true)}
          className="px-3 py-1.5 text-xs font-medium border border-amber-300 text-amber-900 rounded-md hover:bg-amber-50 disabled:opacity-40"
          data-testid={`restore-defaults-open-${pipelineType}`}
        >
          Restore defaults…
        </button>
      </div>

      {listQuery.isLoading && (
        <p className="text-sm text-slate-500">Loading…</p>
      )}
      {listQuery.isError && (
        <p className="text-sm text-red-600">
          {getApiErrorMessage(listQuery.error, "Failed to load statuses")}
        </p>
      )}
      {sectionError && (
        <p className="text-sm text-red-600 mb-2" role="alert">
          {sectionError}
        </p>
      )}

      {!listQuery.isLoading && !listQuery.isError && (
        <>
          {sorted.map((def, index) => (
            <StatusRow
              key={def.id}
              def={def}
              busy={busy}
              canMoveUp={index > 0}
              canMoveDown={index < sorted.length - 1}
              onSave={(label) => updateMutation.mutate({ id: def.id, label })}
              onMoveUp={() => move(index, -1)}
              onMoveDown={() => move(index, 1)}
              onDeactivate={() => {
                if (
                  typeof window !== "undefined" &&
                  !window.confirm(
                    "Deactivate this custom status? It will disappear from the pipeline unless reactivated by support. Leads or jobs still using it cannot be deactivated."
                  )
                ) {
                  return;
                }
                deactivateMutation.mutate(def.id);
              }}
            />
          ))}

          <div className="flex flex-wrap items-end gap-2 mt-4 pt-4 border-t border-slate-100">
            <div className="flex flex-col gap-1 flex-1 min-w-[200px]">
              <label className="text-xs font-medium text-slate-600">New custom status</label>
              <input
                type="text"
                value={addLabel}
                onChange={(e) => setAddLabel(e.target.value)}
                disabled={busy}
                placeholder="Status name"
                className="rounded-md border border-slate-300 px-2 py-1.5 text-sm"
                data-testid={`add-status-input-${pipelineType}`}
              />
            </div>
            <button
              type="button"
              disabled={busy || !addLabel.trim()}
              onClick={() => createMutation.mutate(addLabel.trim())}
              className="px-3 py-2 text-sm font-medium rounded-md bg-slate-800 text-white disabled:opacity-40"
              data-testid={`add-status-submit-${pipelineType}`}
            >
              Add status
            </button>
          </div>
        </>
      )}

      {restoreOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby={`restore-title-${pipelineType}`}
        >
          <div className="bg-white rounded-lg shadow-lg max-w-md w-full p-5">
            <h3
              id={`restore-title-${pipelineType}`}
              className="text-base font-semibold text-slate-900"
            >
              Restore {pipelineType === "LEAD" ? "lead" : "job"} defaults?
            </h3>
            <p className="text-sm text-slate-600 mt-2">
              Built-in statuses will reset to their default labels and order. Optional: deactivate
              custom statuses that are not in use (fails if any custom status is still assigned).
            </p>
            <label className="flex items-center gap-2 mt-4 text-sm text-slate-700">
              <input
                type="checkbox"
                checked={deactivateUnusedOnRestore}
                onChange={(e) => setDeactivateUnusedOnRestore(e.target.checked)}
                data-testid={`restore-deactivate-unused-${pipelineType}`}
              />
              Also deactivate unused custom statuses
            </label>
            <div className="flex justify-end gap-2 mt-6">
              <button
                type="button"
                className="px-3 py-1.5 text-sm border border-slate-300 rounded-md"
                onClick={() => {
                  setRestoreOpen(false);
                  setDeactivateUnusedOnRestore(false);
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={restoreMutation.isPending}
                className="px-3 py-1.5 text-sm font-medium rounded-md bg-amber-600 text-white disabled:opacity-40"
                onClick={() => restoreMutation.mutate()}
                data-testid={`restore-defaults-confirm-${pipelineType}`}
              >
                Restore
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

export default function PipelineStatusSettingsPage() {
  const { auth, ready } = useAuthReady();
  const currentTenant = auth.tenants.find((t) => t.tenantId === auth.selectedTenantId);
  const canManage =
    currentTenant?.role === "OWNER" || currentTenant?.role === "ADMIN";

  if (!ready) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600" />
      </div>
    );
  }

  if (!canManage) {
    return (
      <div className="max-w-3xl mx-auto">
        <SettingsBackLink />
        <h1 className="text-2xl font-bold text-slate-800">Pipeline Statuses</h1>
        <p className="mt-1 text-sm text-slate-500">
          Only owners and admins can manage pipeline statuses. Ask an administrator for access.
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6" data-testid="pipeline-settings-page">
      <div>
        <SettingsBackLink />
        <h1 className="text-2xl font-bold text-slate-800">Pipeline Statuses</h1>
        <p className="mt-1 text-sm text-slate-500">
          Rename, reorder, and add custom statuses for leads and jobs. Changes apply to pipelines,
          lists, and status selectors after they refresh.
        </p>
      </div>

      <PipelineStatusSection pipelineType="LEAD" />
      <PipelineStatusSection pipelineType="JOB" />
    </div>
  );
}
