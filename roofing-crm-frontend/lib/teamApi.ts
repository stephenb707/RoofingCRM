import type { AxiosInstance } from "axios";
import type {
  TeamMember,
  TenantInvite,
  UserRole,
  AcceptInviteResponse,
} from "./types";

/**
 * Fetch list of team members for the tenant.
 */
export async function listMembers(
  api: AxiosInstance
): Promise<TeamMember[]> {
  const res = await api.get<TeamMember[]>("/api/v1/team/members");
  return res.data;
}

/**
 * Fetch list of pending invites for the tenant.
 */
export async function listInvites(
  api: AxiosInstance
): Promise<TenantInvite[]> {
  const res = await api.get<TenantInvite[]>("/api/v1/team/invites");
  return res.data;
}

/**
 * Create a new invite.
 */
export async function createInvite(
  api: AxiosInstance,
  payload: { email: string; role: UserRole }
): Promise<TenantInvite> {
  const res = await api.post<TenantInvite>("/api/v1/team/invites", payload);
  return res.data;
}

/**
 * Revoke a pending invite.
 */
export async function revokeInvite(
  api: AxiosInstance,
  inviteId: string
): Promise<void> {
  await api.delete(`/api/v1/team/invites/${inviteId}`);
}

/**
 * Update a member's role.
 */
export async function updateMemberRole(
  api: AxiosInstance,
  userId: string,
  role: UserRole
): Promise<TeamMember> {
  const res = await api.put<TeamMember>(
    `/api/v1/team/members/${userId}/role`,
    { role }
  );
  return res.data;
}

/**
 * Remove a member from the tenant (soft delete).
 */
export async function removeMember(
  api: AxiosInstance,
  userId: string
): Promise<void> {
  await api.delete(`/api/v1/team/members/${userId}`);
}

/**
 * Accept an invite by token. Does not require X-Tenant-Id.
 */
export async function acceptInvite(
  api: AxiosInstance,
  token: string
): Promise<AcceptInviteResponse> {
  const res = await api.post<AcceptInviteResponse>(
    "/api/v1/team/invites/accept",
    { token }
  );
  return res.data;
}
