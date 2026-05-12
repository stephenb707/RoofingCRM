"use client";

import React, { createContext, useCallback, useContext, useMemo, useEffect, useState, useRef } from "react";
import { AuthResponse, TenantSummary } from "./types";
import {
  AuthState,
  loadAuthStateFromStorage,
  saveAuthStateToStorage,
  clearAuthStateFromStorage,
  emptyAuthState,
} from "./authState";
import { createApiClient, logoutSessionRequest, refreshSessionRequest } from "./apiClient";

type AuthContextValue = {
  auth: AuthState;
  api: ReturnType<typeof createApiClient>;
  setAuthFromLogin: (response: AuthResponse) => void;
  selectTenant: (tenantId: string) => void;
  addTenant: (tenantSummary: TenantSummary) => void;
  refreshAccessToken: () => Promise<string | null>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [auth, setAuth] = useState<AuthState>(emptyAuthState);
  const [hydrated, setHydrated] = useState(false);
  const authRef = useRef<AuthState>(auth);

  useEffect(() => {
    authRef.current = auth;
  }, [auth]);

  const applyAuthResponse = useCallback((response: AuthResponse, preferredTenantId?: string | null) => {
    const tenants = response.tenants ?? [];
    const preferredStillAvailable =
      preferredTenantId && tenants.some((t) => t.tenantId === preferredTenantId);
    const next: AuthState = {
      token: response.token,
      csrfToken: response.csrfToken ?? null,
      userId: response.userId,
      email: response.email,
      fullName: response.fullName ?? null,
      tenants,
      selectedTenantId: preferredStillAvailable
        ? preferredTenantId
        : tenants.length === 1
          ? tenants[0].tenantId
          : null,
    };
    setAuth(next);
    authRef.current = next;
    saveAuthStateToStorage(next);
    return next;
  }, []);

  const clearAuth = useCallback(() => {
    setAuth(emptyAuthState);
    authRef.current = emptyAuthState;
    clearAuthStateFromStorage();
  }, []);

  const handleUnauthenticated = useCallback(() => {
    clearAuth();
    if (typeof window !== "undefined" && window.location.pathname.startsWith("/app")) {
      const next = `${window.location.pathname}${window.location.search}`;
      window.location.assign(`/auth/login?next=${encodeURIComponent(next)}`);
    }
  }, [clearAuth]);

  useEffect(() => {
    let cancelled = false;
    const stored = loadAuthStateFromStorage();
    setAuth(stored);
    authRef.current = stored;

    refreshSessionRequest(stored.csrfToken)
      .then((response) => {
        if (!cancelled) {
          applyAuthResponse(response, stored.selectedTenantId);
        }
      })
      .catch(() => {
        if (!cancelled) {
          clearAuth();
        }
      })
      .finally(() => {
        if (!cancelled) {
          setHydrated(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [applyAuthResponse, clearAuth]);

  const api = useMemo(() => createApiClient(() => authRef.current, {
    onAuthRefreshed: (response) => {
      applyAuthResponse(response, authRef.current.selectedTenantId);
    },
    onUnauthenticated: handleUnauthenticated,
  }), [applyAuthResponse, handleUnauthenticated]);

  const setAuthFromLogin = (response: AuthResponse) => {
    applyAuthResponse(response);
  };

  const refreshAccessToken = useCallback(async () => {
    try {
      const response = await refreshSessionRequest(authRef.current.csrfToken);
      applyAuthResponse(response, authRef.current.selectedTenantId);
      return response.token;
    } catch {
      handleUnauthenticated();
      return null;
    }
  }, [applyAuthResponse, handleUnauthenticated]);

  const selectTenant = (tenantId: string) => {
    const next: AuthState = {
      ...auth,
      selectedTenantId: tenantId,
    };
    setAuth(next);
    saveAuthStateToStorage(next);
  };

  const addTenant = (tenantSummary: TenantSummary) => {
    const exists = auth.tenants.some(
      (t) => t.tenantId === tenantSummary.tenantId
    );
    if (exists) return;
    const next: AuthState = {
      ...auth,
      tenants: [...auth.tenants, tenantSummary],
    };
    setAuth(next);
    saveAuthStateToStorage(next);
  };

  const logout = () => {
    const csrf = authRef.current.csrfToken;
    void logoutSessionRequest(csrf).catch(() => {
      // Local logout should still complete even if the server is unreachable
      // or the CSRF token is stale.
    });
    clearAuth();
  };

  const value: AuthContextValue = {
    auth,
    api,
    setAuthFromLogin,
    selectTenant,
    addTenant,
    refreshAccessToken,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {!hydrated ? (
        <div className="p-4 text-sm text-slate-500">Loading…</div>
      ) : (
        children
      )}
    </AuthContext.Provider>
  );
};

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}

export function useAuthReady() {
  const ctx = useAuth();
  const ready = !!(ctx.auth.token && ctx.auth.selectedTenantId);
  return { ...ctx, ready };
}
