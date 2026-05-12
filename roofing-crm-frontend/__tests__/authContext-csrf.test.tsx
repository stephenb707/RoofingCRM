import React from "react";
import { act, render, screen } from "@testing-library/react";

jest.mock("@/lib/apiClient", () => {
  const original = jest.requireActual("@/lib/apiClient");
  return {
    ...original,
    refreshSessionRequest: jest.fn(),
    logoutSessionRequest: jest.fn(),
    createApiClient: jest.fn(() => ({})),
  };
});

import { AuthProvider, useAuth } from "@/lib/AuthContext";
import {
  refreshSessionRequest,
  logoutSessionRequest,
} from "@/lib/apiClient";
import type { AuthResponse } from "@/lib/types";

const mockedRefresh = refreshSessionRequest as jest.MockedFunction<typeof refreshSessionRequest>;
const mockedLogout = logoutSessionRequest as jest.MockedFunction<typeof logoutSessionRequest>;

function TestProbe() {
  const { auth, logout } = useAuth();
  return (
    <div>
      <div data-testid="token">{auth.token ?? "null"}</div>
      <div data-testid="csrf">{auth.csrfToken ?? "null"}</div>
      <div data-testid="email">{auth.email ?? "null"}</div>
      <button data-testid="logout-btn" onClick={() => logout()}>logout</button>
    </div>
  );
}

const STORAGE_KEY = "roofingcrm_auth";

beforeEach(() => {
  window.localStorage.clear();
  mockedRefresh.mockReset();
  mockedLogout.mockReset();
});

afterEach(() => {
  window.localStorage.clear();
});

describe("AuthContext + CSRF token", () => {
  it("stores the CSRF token from the bootstrap refresh response", async () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        csrfToken: "stored-csrf",
        userId: "u-1",
        email: "u@example.com",
        tenants: [{ tenantId: "t-1", tenantName: "T", tenantSlug: "t", role: "OWNER" }],
        selectedTenantId: "t-1",
      }),
    );

    const refreshed: AuthResponse = {
      token: "fresh-jwt",
      csrfToken: "rotated-csrf",
      userId: "u-1",
      email: "u@example.com",
      fullName: "User",
      tenants: [{ tenantId: "t-1", tenantName: "T", tenantSlug: "t", role: "OWNER" }],
    };
    mockedRefresh.mockResolvedValueOnce(refreshed);

    await act(async () => {
      render(
        <AuthProvider>
          <TestProbe />
        </AuthProvider>,
      );
    });

    // Bootstrap refresh must use the CSRF token persisted from the previous session.
    expect(mockedRefresh).toHaveBeenCalledWith("stored-csrf");
    expect(screen.getByTestId("token").textContent).toBe("fresh-jwt");
    expect(screen.getByTestId("csrf").textContent).toBe("rotated-csrf");
    expect(screen.getByTestId("email").textContent).toBe("u@example.com");

    const persisted = JSON.parse(window.localStorage.getItem(STORAGE_KEY) ?? "{}");
    expect(persisted.csrfToken).toBe("rotated-csrf");
    // Access token must NEVER be persisted.
    expect(persisted.token).toBeNull();
  });

  it("clears CSRF and auth state when bootstrap refresh fails", async () => {
    window.localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        csrfToken: "stale-csrf",
        userId: "u-1",
        email: "u@example.com",
        tenants: [],
        selectedTenantId: null,
      }),
    );
    mockedRefresh.mockRejectedValueOnce(new Error("401"));

    await act(async () => {
      render(
        <AuthProvider>
          <TestProbe />
        </AuthProvider>,
      );
    });

    expect(screen.getByTestId("token").textContent).toBe("null");
    expect(screen.getByTestId("csrf").textContent).toBe("null");
    expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it("logout sends the current CSRF token and clears state even if the call fails", async () => {
    const refreshed: AuthResponse = {
      token: "fresh-jwt",
      csrfToken: "current-csrf",
      userId: "u-1",
      email: "u@example.com",
      fullName: "User",
      tenants: [{ tenantId: "t-1", tenantName: "T", tenantSlug: "t", role: "OWNER" }],
    };
    mockedRefresh.mockResolvedValueOnce(refreshed);
    mockedLogout.mockRejectedValueOnce(new Error("network down"));

    await act(async () => {
      render(
        <AuthProvider>
          <TestProbe />
        </AuthProvider>,
      );
    });

    expect(screen.getByTestId("csrf").textContent).toBe("current-csrf");

    await act(async () => {
      screen.getByTestId("logout-btn").click();
      // Allow rejected logout promise to resolve
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(mockedLogout).toHaveBeenCalledWith("current-csrf");
    expect(screen.getByTestId("token").textContent).toBe("null");
    expect(screen.getByTestId("csrf").textContent).toBe("null");
    expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull();
  });
});
