import { TenantSummary } from "./types";

export type AuthState = {
  token: string | null;
  userId: string | null;
  email: string | null;
  fullName: string | null;
  tenants: TenantSummary[];
  selectedTenantId: string | null;
};

const STORAGE_KEY = "roofingcrm_auth";

export function loadAuthStateFromStorage(): AuthState {
  if (typeof window === "undefined") {
    return {
      token: null,
      userId: null,
      email: null,
      fullName: null,
      tenants: [],
      selectedTenantId: null,
    };
  }

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return {
        token: null,
        userId: null,
        email: null,
        fullName: null,
        tenants: [],
        selectedTenantId: null,
      };
    }
    const parsed = JSON.parse(raw) as AuthState;
    return {
      token: parsed.token ?? null,
      userId: parsed.userId ?? null,
      email: parsed.email ?? null,
      fullName: parsed.fullName ?? null,
      tenants: parsed.tenants ?? [],
      selectedTenantId: parsed.selectedTenantId ?? null,
    };
  } catch {
    return {
      token: null,
      userId: null,
      email: null,
      fullName: null,
      tenants: [],
      selectedTenantId: null,
    };
  }
}

export function saveAuthStateToStorage(state: AuthState) {
  if (typeof window === "undefined") return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

export function clearAuthStateFromStorage() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem(STORAGE_KEY);
}
