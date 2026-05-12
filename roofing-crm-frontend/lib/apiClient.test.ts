import axios, { AxiosError, AxiosHeaders, InternalAxiosRequestConfig } from "axios";
import {
  createApiClient,
  logoutSessionRequest,
  refreshSessionRequest,
} from "./apiClient";
import type { AuthResponse } from "./types";

describe("apiClient", () => {
  it("adds Authorization and X-Tenant-Id headers", async () => {
    expect.assertions(2);
    const api = createApiClient(() => ({
      token: "token-123",
      csrfToken: "csrf-abc",
      userId: "user-1",
      email: "user@example.com",
      fullName: "Test User",
      selectedTenantId: "tenant-456",
      tenants: [],
    }));

    await api.get("/test", {
      adapter: async (config) => {
        const headers = AxiosHeaders.from(config.headers);
        expect(headers.get("Authorization")).toBe("Bearer token-123");
        expect(headers.get("X-Tenant-Id")).toBe("tenant-456");
        return {
          status: 200,
          statusText: "OK",
          headers: {},
          config,
          data: {},
        };
      },
    });
  });

  it("removes manual Content-Type for FormData payloads", async () => {
    const api = createApiClient(() => ({
      token: "token-123",
      csrfToken: "csrf-abc",
      userId: "user-1",
      email: "user@example.com",
      fullName: "Test User",
      selectedTenantId: "tenant-456",
      tenants: [],
    }));

    const formData = new FormData();
    formData.append("file", new File(["x"], "upload.png", { type: "image/png" }));
    await api.post("/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      adapter: async (config) => {
        const headers = AxiosHeaders.from(config.headers);
        expect(headers.get("Authorization")).toBe("Bearer token-123");
        expect(headers.get("X-Tenant-Id")).toBe("tenant-456");
        expect(headers.get("Content-Type")).not.toBe("multipart/form-data");
        return {
          status: 200,
          statusText: "OK",
          headers: {},
          config,
          data: {},
        };
      },
    });
  });

  it("refreshes once and retries concurrent 401 responses with the new token", async () => {
    let token = "old-token";
    const refreshed: AuthResponse = {
      token: "new-token",
      csrfToken: "next-csrf",
      userId: "user-1",
      email: "user@example.com",
      fullName: "Test User",
      tenants: [],
    };
    const postSpy = jest.spyOn(axios, "post").mockResolvedValue({ data: refreshed });
    const onAuthRefreshed = jest.fn((response: AuthResponse) => {
      token = response.token;
    });
    const api = createApiClient(
      () => ({
        token,
        csrfToken: "current-csrf",
        userId: "user-1",
        email: "user@example.com",
        fullName: "Test User",
        selectedTenantId: "tenant-456",
        tenants: [],
      }),
      { onAuthRefreshed }
    );

    const seenAuthHeaders: string[] = [];
    const adapter = jest.fn(async (config) => {
      const auth = AxiosHeaders.from(config.headers).get("Authorization") as string;
      seenAuthHeaders.push(auth);
      if (auth === "Bearer old-token") {
        const response = {
          status: 401,
          statusText: "Unauthorized",
          headers: {},
          config,
          data: {},
        };
        throw new AxiosError("Unauthorized", "ERR_BAD_REQUEST", config as InternalAxiosRequestConfig, null, response);
      }
      return {
        status: 200,
        statusText: "OK",
        headers: {},
        config,
        data: { ok: true },
      };
    });

    await Promise.all([
      api.get("/needs-auth-a", { adapter }),
      api.get("/needs-auth-b", { adapter }),
    ]);

    expect(postSpy).toHaveBeenCalledTimes(1);
    // The single-flight refresh must include the current CSRF token from auth state.
    const refreshCall = postSpy.mock.calls[0];
    expect(refreshCall[0]).toContain("/api/v1/auth/refresh");
    expect((refreshCall[2] as { headers: Record<string, string> }).headers["X-CSRF-Refresh"]).toBe("current-csrf");
    expect(onAuthRefreshed).toHaveBeenCalledWith(refreshed);
    expect(seenAuthHeaders.filter((h) => h === "Bearer old-token")).toHaveLength(2);
    expect(seenAuthHeaders.filter((h) => h === "Bearer new-token")).toHaveLength(2);

    postSpy.mockRestore();
  });
});

describe("refreshSessionRequest", () => {
  it("sends the supplied CSRF token in X-CSRF-Refresh header and credentials", async () => {
    const refreshed: AuthResponse = {
      token: "new-token",
      csrfToken: "rotated-csrf",
      userId: "user-1",
      email: "user@example.com",
      fullName: "Test User",
      tenants: [],
    };
    const postSpy = jest.spyOn(axios, "post").mockResolvedValue({ data: refreshed });

    const result = await refreshSessionRequest("client-csrf-value");

    expect(result).toEqual(refreshed);
    expect(postSpy).toHaveBeenCalledTimes(1);
    const [, , config] = postSpy.mock.calls[0];
    const cfg = config as { withCredentials: boolean; headers: Record<string, string> };
    expect(cfg.withCredentials).toBe(true);
    expect(cfg.headers["X-CSRF-Refresh"]).toBe("client-csrf-value");

    postSpy.mockRestore();
  });

  it("sends an empty header when no CSRF token is available", async () => {
    const postSpy = jest.spyOn(axios, "post").mockResolvedValue({ data: {} as AuthResponse });

    await refreshSessionRequest(null);

    const [, , config] = postSpy.mock.calls[0];
    const cfg = config as { headers: Record<string, string> };
    expect(cfg.headers["X-CSRF-Refresh"]).toBe("");

    postSpy.mockRestore();
  });
});

describe("logoutSessionRequest", () => {
  it("sends the supplied CSRF token and credentials", async () => {
    const postSpy = jest.spyOn(axios, "post").mockResolvedValue({ data: undefined });

    await logoutSessionRequest("logout-csrf-value");

    expect(postSpy).toHaveBeenCalledTimes(1);
    const [url, , config] = postSpy.mock.calls[0];
    expect(url).toContain("/api/v1/auth/logout");
    const cfg = config as { withCredentials: boolean; headers: Record<string, string> };
    expect(cfg.withCredentials).toBe(true);
    expect(cfg.headers["X-CSRF-Refresh"]).toBe("logout-csrf-value");

    postSpy.mockRestore();
  });
});
