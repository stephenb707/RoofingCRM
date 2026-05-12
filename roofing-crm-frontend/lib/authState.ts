import { TenantSummary } from "./types";

export type AuthState = {
  token: string | null;
  /**
   * Per-session CSRF token paired with the HttpOnly refresh cookie. Stored alongside
   * non-sensitive metadata so we can survive a page reload and still send the matching
   * X-CSRF-Refresh header on the bootstrap refresh call.
   *
   * NOT a session secret on its own — the refresh cookie is HttpOnly. This token only
   * proves the request originated from our SPA (CSRF defense), not that it's authenticated.
   */
  csrfToken: string | null;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  tenants: TenantSummary[];
  selectedTenantId: string | null;
};

export const emptyAuthState: AuthState = {
  token: null,
  csrfToken: null,
  userId: null,
  email: null,
  fullName: null,
  tenants: [],
  selectedTenantId: null,
};

const STORAGE_KEY = "roofingcrm_auth";

export function loadAuthStateFromStorage(): AuthState {
  if (typeof window === "undefined") return emptyAuthState;

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return emptyAuthState;

    const parsed = JSON.parse(raw) as Partial<AuthState>;

    return {
      // Access tokens are intentionally memory-only. The HttpOnly refresh cookie is the session.
      token: null,
      csrfToken: parsed.csrfToken ?? null,
      userId: parsed.userId ?? null,
      email: parsed.email ?? null,
      fullName: parsed.fullName ?? null,
      tenants: parsed.tenants ?? [],
      selectedTenantId: parsed.selectedTenantId ?? null,
    };
  } catch {
    return emptyAuthState;
  }
}

export function saveAuthStateToStorage(state: AuthState) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
    ...state,
    token: null,
  }));
}

export function clearAuthStateFromStorage() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(STORAGE_KEY);
}
