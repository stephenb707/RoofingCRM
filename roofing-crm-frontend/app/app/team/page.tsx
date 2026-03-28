"use client";

import { useState, useCallback } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import {
  listMembers,
  listInvites,
  createInvite,
  resendInvite,
  revokeInvite,
  updateMemberRole,
  removeMember,
} from "@/lib/teamApi";
import { queryKeys } from "@/lib/queryKeys";
import { getApiErrorMessage } from "@/lib/apiError";
import type { TeamMember, TenantInvite, UserRole } from "@/lib/types";

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: "OWNER", label: "Owner" },
  { value: "ADMIN", label: "Admin" },
  { value: "SALES", label: "Sales" },
  { value: "FIELD_TECH", label: "Field Tech" },
];

export default function TeamPage() {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const tenantId = auth.selectedTenantId ?? null;

  const currentTenant = auth.tenants.find(
    (t) => t.tenantId === auth.selectedTenantId
  );
  const myRole = (currentTenant?.role ?? "FIELD_TECH") as UserRole;
  const canInvite = myRole === "OWNER" || myRole === "ADMIN";
  const canManageMembers = myRole === "OWNER";

  const [inviteEmail, setInviteEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<UserRole>("SALES");
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [inviteSuccess, setInviteSuccess] = useState<string | null>(null);

  const { data: members = [], isLoading: membersLoading } = useQuery({
    queryKey: queryKeys.teamMembers(tenantId),
    queryFn: () => listMembers(api),
    enabled: ready && !!tenantId,
  });

  const { data: invites = [], isLoading: invitesLoading } = useQuery({
    queryKey: queryKeys.teamInvites(tenantId),
    queryFn: () => listInvites(api),
    enabled: ready && !!tenantId && canInvite,
  });

  const createInviteMutation = useMutation({
    mutationFn: (payload: { email: string; role: UserRole }) =>
      createInvite(api, payload),
    onSuccess: (data) => {
      setInviteEmail("");
      setInviteError(null);
      setInviteSuccess(`Invitation email sent to ${data.email}.`);
      queryClient.invalidateQueries({ queryKey: queryKeys.teamInvites(tenantId) });
    },
    onError: (err) => {
      setInviteSuccess(null);
      setInviteError(getApiErrorMessage(err, "Failed to create invite"));
    },
  });

  const resendInviteMutation = useMutation({
    mutationFn: (inviteId: string) => resendInvite(api, inviteId),
    onSuccess: (data) => {
      setInviteError(null);
      setInviteSuccess(`Invitation email resent to ${data.email}.`);
      queryClient.invalidateQueries({ queryKey: queryKeys.teamInvites(tenantId) });
    },
    onError: (err) => {
      setInviteSuccess(null);
      setInviteError(getApiErrorMessage(err, "Failed to resend invite"));
    },
  });

  const revokeInviteMutation = useMutation({
    mutationFn: (inviteId: string) => revokeInvite(api, inviteId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.teamInvites(tenantId) });
      setInviteSuccess(null);
    },
  });

  const updateRoleMutation = useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: UserRole }) =>
      updateMemberRole(api, userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.teamMembers(tenantId) });
    },
  });

  const removeMemberMutation = useMutation({
    mutationFn: (userId: string) => removeMember(api, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.teamMembers(tenantId) });
    },
  });

  const handleInviteSubmit = useCallback(
    (e: React.FormEvent) => {
      e.preventDefault();
      if (!inviteEmail.trim()) return;
      setInviteError(null);
      setInviteSuccess(null);
      createInviteMutation.mutate({
        email: inviteEmail.trim(),
        role: inviteRole,
      });
    },
    [inviteEmail, inviteRole, createInviteMutation]
  );

  if (!ready) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Team</h1>

      {/* Members section */}
      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">
          Members
        </h2>
        {membersLoading ? (
          <div className="py-8 text-center text-slate-500 text-sm">
            Loading members...
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200">
                  <th className="text-left py-2 font-medium text-slate-700">
                    Name / Email
                  </th>
                  <th className="text-left py-2 font-medium text-slate-700">
                    Role
                  </th>
                  {canManageMembers && (
                    <th className="text-right py-2 font-medium text-slate-700">
                      Actions
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {members.map((member) => (
                  <tr
                    key={member.userId}
                    className="border-b border-slate-100 last:border-0"
                  >
                    <td className="py-3">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-slate-800">
                          {member.fullName || member.email}
                        </span>
                        {member.userId === auth.userId && (
                          <span className="px-2 py-0.5 text-xs font-medium bg-sky-100 text-sky-700 rounded">
                            You
                          </span>
                        )}
                        {member.fullName && (
                          <span className="text-slate-500 text-xs">
                            {member.email}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3">
                      {canManageMembers &&
                      member.userId !== auth.userId ? (
                        <select
                          value={member.role}
                          onChange={(e) =>
                            updateRoleMutation.mutate({
                              userId: member.userId,
                              role: e.target.value as UserRole,
                            })
                          }
                          className="border border-slate-300 rounded px-2 py-1 text-sm"
                        >
                          {ROLE_OPTIONS.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                              {opt.label}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <span className="text-slate-600">{member.role}</span>
                      )}
                    </td>
                    {canManageMembers && (
                      <td className="py-3 text-right">
                        {member.userId !== auth.userId && (
                          <button
                            onClick={() =>
                              removeMemberMutation.mutate(member.userId)
                            }
                            disabled={removeMemberMutation.isPending}
                            className="text-red-600 hover:text-red-700 text-sm font-medium disabled:opacity-50"
                          >
                            Remove
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Invites section (only if canInvite) */}
      {canInvite && (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Invite team member
          </h2>

          <form onSubmit={handleInviteSubmit} className="flex flex-wrap gap-4 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Email
              </label>
              <input
                type="email"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                placeholder="colleague@company.com"
                className="border border-slate-300 rounded-lg px-3 py-2 text-sm w-64"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Role
              </label>
              <select
                value={inviteRole}
                onChange={(e) =>
                  setInviteRole(e.target.value as UserRole)
                }
                className="border border-slate-300 rounded-lg px-3 py-2 text-sm"
              >
                {ROLE_OPTIONS.filter(
                  (o) => o.value !== "OWNER" || myRole === "OWNER"
                ).map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex items-end">
              <button
                type="submit"
                disabled={createInviteMutation.isPending || !inviteEmail.trim()}
                className="px-4 py-2 bg-sky-600 hover:bg-sky-700 disabled:bg-sky-400 text-white text-sm font-medium rounded-lg"
              >
                {createInviteMutation.isPending ? "Inviting…" : "Invite"}
              </button>
            </div>
          </form>

          {inviteError && (
            <p className="text-sm text-red-600 mb-4">{inviteError}</p>
          )}

          {inviteSuccess && (
            <div className="mb-6 p-4 bg-sky-50 border border-sky-200 rounded-lg text-sm font-medium text-sky-800">
              {inviteSuccess}
            </div>
          )}

          <h3 className="text-sm font-semibold text-slate-700 mb-3">Pending invites</h3>

          {invitesLoading ? (
            <p className="text-sm text-slate-500">Loading invites...</p>
          ) : invites.length > 0 ? (
            <ul className="space-y-2">
              {invites.map((inv) => (
                <li
                  key={inv.inviteId}
                  className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0"
                >
                  <div className="text-sm">
                    <div className="font-medium text-slate-800">
                      {inv.email} ({inv.role})
                    </div>
                    <div className="text-slate-500 text-xs">
                      Expires {new Date(inv.expiresAt).toLocaleDateString()}
                      {inv.createdByName ? ` • Invited by ${inv.createdByName}` : ""}
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => resendInviteMutation.mutate(inv.inviteId)}
                      disabled={resendInviteMutation.isPending}
                      className="text-sky-600 hover:text-sky-700 text-sm font-medium disabled:opacity-50"
                    >
                      Resend
                    </button>
                    <button
                      onClick={() => revokeInviteMutation.mutate(inv.inviteId)}
                      disabled={revokeInviteMutation.isPending}
                      className="text-red-600 hover:text-red-700 text-sm font-medium disabled:opacity-50"
                    >
                      Revoke
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-500">No pending invites</p>
          )}
        </div>
      )}
    </div>
  );
}
