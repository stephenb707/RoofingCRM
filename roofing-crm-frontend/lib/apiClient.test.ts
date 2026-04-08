import { createApiClient } from "./apiClient";

describe("apiClient", () => {
  it("adds Authorization and X-Tenant-Id headers", async () => {
    const api = createApiClient(() => ({
      token: "token-123",
      userId: "user-1",
      email: "user@example.com",
      fullName: "Test User",
      selectedTenantId: "tenant-456",
      tenants: [],
    }));

    const [fulfilled] = api.interceptors.request.handlers;
    const config = await fulfilled!.fulfilled!({
      headers: {},
      data: undefined,
    });

    expect(config.headers.get("Authorization")).toBe("Bearer token-123");
    expect(config.headers.get("X-Tenant-Id")).toBe("tenant-456");
  });

  it("removes manual Content-Type for FormData payloads", async () => {
    const api = createApiClient(() => ({
      token: "token-123",
      userId: "user-1",
      email: "user@example.com",
      fullName: "Test User",
      selectedTenantId: "tenant-456",
      tenants: [],
    }));

    const [fulfilled] = api.interceptors.request.handlers;
    const formData = new FormData();
    formData.append("file", new File(["x"], "upload.png", { type: "image/png" }));

    const config = await fulfilled!.fulfilled!({
      headers: { "Content-Type": "multipart/form-data" },
      data: formData,
    });

    expect(config.headers.get("Authorization")).toBe("Bearer token-123");
    expect(config.headers.get("X-Tenant-Id")).toBe("tenant-456");
    expect(config.headers.has("Content-Type")).toBe(false);
  });
});
