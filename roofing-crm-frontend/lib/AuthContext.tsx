"use client";

import React, { createContext, useContext, useMemo, useEffect, useState, useRef } from "react";
import { AuthResponse, TenantSummary } from "./types";
import {
  AuthState,
  loadAuthStateFromStorage,
  saveAuthStateToStorage,
  clearAuthStateFromStorage,
  emptyAuthState,
} from "./authState";
import { createApiClient } from "./apiClient";

type AuthContextValue = {
  auth: AuthState;
  api: ReturnType<typeof createApiClient>;
  setAuthFromLogin: (response: AuthResponse) => void;
  selectTenant: (tenantId: string) => void;
  addTenant: (tenantSummary: TenantSummary) => void;
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
    setAuth(loadAuthStateFromStorage());
    setHydrated(true);
  }, []);

  useEffect(() => {
    authRef.current = auth;
  }, [auth]);

  const api = useMemo(() => createApiClient(() => authRef.current), []);

  const setAuthFromLogin = (response: AuthResponse) => {
    const next: AuthState = {
      token: response.token,
      userId: response.userId,
      email: response.email,
      fullName: response.fullName ?? null,
      tenants: response.tenants ?? [],
      selectedTenantId:
        response.tenants && response.tenants.length === 1
          ? response.tenants[0].tenantId
          : null,
    };
    setAuth(next);
    saveAuthStateToStorage(next);
  };

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
    setAuth({
      token: null,
      userId: null,
      email: null,
      fullName: null,
      tenants: [],
      selectedTenantId: null,
    });
    clearAuthStateFromStorage();
  };

  const value: AuthContextValue = {
    auth,
    api,
    setAuthFromLogin,
    selectTenant,
    addTenant,
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
