import axios from "axios";
import { AuthState } from "./authState";

const baseURL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export function createApiClient(getAuthState: () => AuthState) {
  const instance = axios.create({
    baseURL,
  });

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

  return instance;
}
