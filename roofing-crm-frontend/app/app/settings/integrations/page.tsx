"use client";

import { useAuthReady } from "@/lib/AuthContext";
import SettingsBackLink from "@/app/app/settings/SettingsBackLink";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";
import {
  disableIntegration,
  listIntegrationSettings,
  updateIntegrationSettings,
} from "@/lib/integrationsApi";
import type {
  IntegrationConnectionStatusKey,
  IntegrationSettingsDto,
} from "@/lib/types";
import { useMemo, useState } from "react";

function statusLabel(s: IntegrationConnectionStatusKey): string {
  switch (s) {
    case "NOT_CONFIGURED":
      return "Not configured";
    case "CONNECTED":
      return "Connected";
    case "ERROR":
      return "Error";
    case "DISABLED":
      return "Disabled";
    default:
      return s;
  }
}

function statusStyles(s: IntegrationConnectionStatusKey): string {
  switch (s) {
    case "CONNECTED":
      return "bg-emerald-50 text-emerald-800 border-emerald-200";
    case "ERROR":
      return "bg-rose-50 text-rose-800 border-rose-200";
    case "DISABLED":
      return "bg-slate-100 text-slate-600 border-slate-200";
    default:
      return "bg-amber-50 text-amber-900 border-amber-200";
  }
}

export default function IntegrationsSettingsPage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const tenantId = auth.selectedTenantId;
  const [message, setMessage] = useState<string | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: queryKeys.integrationSettings(tenantId),
    queryFn: () => listIntegrationSettings(api),
    enabled: ready && Boolean(tenantId),
  });

  const grouped = useMemo(() => {
    if (!data) return new Map<string, IntegrationSettingsDto[]>();
    const m = new Map<string, IntegrationSettingsDto[]>();
    for (const row of data) {
      const cat = row.category || "Other";
      if (!m.has(cat)) m.set(cat, []);
      m.get(cat)!.push(row);
    }
    return m;
  }, [data]);

  const mutateToggle = useMutation({
    mutationFn: async (row: IntegrationSettingsDto) =>
      updateIntegrationSettings(api, row.provider, { enabled: !row.enabled }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.integrationSettings(tenantId),
      });
      setMessage(null);
    },
  });

  const mutateDisable = useMutation({
    mutationFn: async (row: IntegrationSettingsDto) =>
      disableIntegration(api, row.provider),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: queryKeys.integrationSettings(tenantId),
      });
      setMessage(null);
    },
  });

  if (!ready || isLoading) {
    return (
      <div className="flex items-center justify-center py-32">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="max-w-3xl mx-auto px-4">
        <SettingsBackLink />
        <p className="text-rose-600">
          You may need owner or admin access to view integration settings.
        </p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 pb-12">
      <SettingsBackLink />
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-slate-800">Integrations</h1>
        <p className="text-sm text-slate-500 mt-1">
          Connect accounting, messaging, measurements, e-signature, and
          supplier services. This release prepares secure settings and jobs —
          vendor connections are rolled out next; nothing here calls external
          APIs yet.
        </p>
        {message ? (
          <p className="text-xs text-sky-700 mt-2">{message}</p>
        ) : null}
      </div>

      <div className="space-y-8">
        {[...grouped.entries()].map(([category, rows]) => (
          <section key={category}>
            <h2 className="text-sm font-semibold text-slate-700 uppercase tracking-wide mb-3">
              {category}
            </h2>
            <div className="space-y-3">
              {rows.map((row) => (
                <div
                  key={row.provider}
                  className="bg-white rounded-xl border border-slate-200 shadow-sm p-4 flex flex-col sm:flex-row sm:items-center gap-3"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-medium text-slate-900">
                        {row.humanLabel}
                      </h3>
                      <span
                        className={`text-xs font-medium px-2 py-0.5 rounded border ${statusStyles(
                          row.status
                        )}`}
                      >
                        {statusLabel(row.status)}
                      </span>
                      {row.hasCredentials ? (
                        <span className="text-xs text-slate-500">
                          Credentials stored
                        </span>
                      ) : null}
                    </div>
                    <p className="text-xs text-slate-500 font-mono mt-1">
                      {row.provider}
                    </p>
                    {row.lastError ? (
                      <p className="text-xs text-rose-600 mt-1">
                        {row.lastError}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <label className="inline-flex items-center gap-2 text-sm text-slate-600 cursor-pointer">
                      <input
                        type="checkbox"
                        className="rounded border-slate-300"
                        checked={row.enabled}
                        disabled={mutateToggle.isPending}
                        onChange={() => {
                          mutateToggle.mutate(row, {
                            onError: () =>
                              setMessage(
                                "Could not update integration — check your role or try again."
                              ),
                          });
                        }}
                      />
                      Enabled (placeholder)
                    </label>
                    <button
                      type="button"
                      className="text-xs font-medium px-3 py-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-50 disabled:opacity-50"
                      disabled={mutateDisable.isPending}
                      onClick={() =>
                        mutateDisable.mutate(row, {
                          onError: () =>
                            setMessage(
                              "Could not disable — check your role or try again."
                            ),
                        })
                      }
                    >
                      Disable
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        ))}
      </div>

      <p className="text-xs text-slate-400 mt-10 text-center">
        Need help? See environment variables for storage, background jobs, and{" "}
        <code className="text-slate-500">APP_INTEGRATIONS_ENCRYPTION_KEY</code>{" "}
        in the backend docs.
      </p>
    </div>
  );
}
