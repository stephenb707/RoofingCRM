import React from "react";
import { render, screen, mockAuthValue } from "./test-utils";
import AppLayout from "@/app/app/layout";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ replace: jest.fn(), push: jest.fn(), back: jest.fn(), refresh: jest.fn(), prefetch: jest.fn() }),
  usePathname: () => "/app",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

describe("AppLayout navigation", () => {
  const defaultTenant = {
    tenantId: "tenant-123",
    tenantName: "Test Company",
    tenantSlug: "test-company",
    role: "ADMIN" as const,
  };

  beforeEach(() => {
    mockAuthValue.auth.tenants = [{ ...defaultTenant }];
    mockAuthValue.auth.selectedTenantId = "tenant-123";
    mockAuthValue.auth.token = "t";
  });

  it("includes Dashboard as first nav link to /app", () => {
    render(
      <AppLayout>
        <div>Content</div>
      </AppLayout>
    );

    const dash = screen.getByRole("link", { name: /^Dashboard$/i });
    expect(dash).toHaveAttribute("href", "/app");
    expect(dash).toHaveClass("text-sky-600");
  });

  it("shows Pipeline settings for OWNER or ADMIN", () => {
    render(
      <AppLayout>
        <div>Content</div>
      </AppLayout>
    );

    const link = screen.getByRole("link", { name: /^Pipeline settings$/i });
    expect(link).toHaveAttribute("href", "/app/settings/pipeline-statuses");
  });

  it("hides Pipeline settings for SALES", () => {
    mockAuthValue.auth.tenants[0]!.role = "SALES";

    render(
      <AppLayout>
        <div>Content</div>
      </AppLayout>
    );

    expect(screen.queryByRole("link", { name: /^Pipeline settings$/i })).not.toBeInTheDocument();
  });
});
