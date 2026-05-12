import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from "axios";
import { AuthState } from "./authState";
import type { AuthResponse } from "./types";

const baseURL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type RetriableConfig = InternalAxiosRequestConfig & { _retryAuthRefresh?: boolean };

type ApiClientOptions = {
  onAuthRefreshed?: (response: AuthResponse) => void;
  onUnauthenticated?: () => void;
};

const CSRF_HEADER = "X-CSRF-Refresh";

/**
 * Calls /api/v1/auth/refresh. Sends the CSRF token paired with the current refresh
 * session in the X-CSRF-Refresh header; the HttpOnly refresh cookie is sent automatically.
 *
 * `csrfToken` may be null on first load (no prior session) — the server will then return 401
 * and the SPA falls back to the login flow.
 */
export async function refreshSessionRequest(csrfToken: string | null | undefined): Promise<AuthResponse> {
  const { data } = await axios.post<AuthResponse>(
    `${baseURL}/api/v1/auth/refresh`,
    {},
    {
      withCredentials: true,
      headers: { [CSRF_HEADER]: csrfToken ?? "" },
    }
  );
  return data;
}

/**
 * Calls /api/v1/auth/logout to revoke the refresh session. Requires the same per-session
 * CSRF token so a third-party site cannot force-log-out a user with their refresh cookie.
 */
export async function logoutSessionRequest(csrfToken: string | null | undefined): Promise<void> {
  await axios.post(`${baseURL}/api/v1/auth/logout`, {}, {
    withCredentials: true,
    headers: { [CSRF_HEADER]: csrfToken ?? "" },
  });
}

export function createApiClient(getAuthState: () => AuthState, options: ApiClientOptions = {}) {
  const instance = axios.create({
    baseURL,
    withCredentials: true,
  });

  let refreshPromise: Promise<AuthResponse> | null = null;

  instance.interceptors.request.use((config) => {
    const auth = getAuthState();
    const headers = axios.AxiosHeaders.from(config.headers ?? {});

    if (auth.token) {
      headers.set("Authorization", `Bearer ${auth.token}`);
    }
    if (auth.selectedTenantId) {
      headers.set("X-Tenant-Id", auth.selectedTenantId);
    }

    // Let axios/browser compute the multipart boundary automatically.
    if (typeof FormData !== "undefined" && config.data instanceof FormData) {
      headers.delete("Content-Type");
    }

    config.headers = headers;
    return config;
  });

  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const response = error.response;
      const original = error.config as RetriableConfig | undefined;
      const url = original?.url ?? "";
      const isAuthEndpoint = url.includes("/api/v1/auth/login")
        || url.includes("/api/v1/auth/register")
        || url.includes("/api/v1/auth/refresh")
        || url.includes("/api/v1/auth/logout");

      if (response?.status !== 401 || !original || original._retryAuthRefresh || isAuthEndpoint) {
        return Promise.reject(error);
      }

      original._retryAuthRefresh = true;
      try {
        if (!refreshPromise) {
          // Read the latest CSRF token at the moment we kick off the refresh, not at
          // interceptor-creation time, so we always send the freshest value.
          const csrf = getAuthState().csrfToken;
          refreshPromise = refreshSessionRequest(csrf).finally(() => {
            refreshPromise = null;
          });
        }
        const refreshed = await refreshPromise;
        options.onAuthRefreshed?.(refreshed);
        const headers = axios.AxiosHeaders.from(original.headers ?? {});
        headers.set("Authorization", `Bearer ${refreshed.token}`);
        original.headers = headers;
        return instance(original);
      } catch (refreshError) {
        options.onUnauthenticated?.();
        return Promise.reject(refreshError);
      }
    }
  );

  return instance;
}
