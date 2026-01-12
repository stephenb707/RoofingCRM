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
    if (auth.token) {
      config.headers.set("Authorization", `Bearer ${auth.token}`);
    }
    if (auth.selectedTenantId) {
      config.headers.set("X-Tenant-Id", auth.selectedTenantId);
    }
    return config;
  });

  return instance;
}
