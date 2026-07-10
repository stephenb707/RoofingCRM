import React from "react";
import { render, screen, mockAuthValue, waitFor } from "./test-utils";
import IntegrationsSettingsPage from "@/app/app/settings/integrations/page";

jest.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: jest.fn(),
    push: jest.fn(),
    back: jest.fn(),
    refresh: jest.fn(),
    prefetch: jest.fn(),
  }),
  usePathname: () => "/app/settings/integrations",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

const mockList = jest.fn();

jest.mock("@/lib/integrationsApi", () => ({
  listIntegrationSettings: (...args: unknown[]) => mockList(...args),
  updateIntegrationSettings: jest.fn(),
  disableIntegration: jest.fn(),
}));

describe("IntegrationsSettingsPage", () => {
  beforeEach(() => {
    mockAuthValue.auth.tenants = [
      {
        tenantId: "tenant-123",
        tenantName: "Test Company",
        tenantSlug: "test-company",
        role: "ADMIN" as const,
      },
    ];
    mockAuthValue.auth.selectedTenantId = "tenant-123";
    mockAuthValue.auth.token = "t";
    mockList.mockResolvedValue([
      {
        provider: "TWILIO",
        humanLabel: "SMS / Twilio",
        category: "Messaging",
        enabled: false,
        status: "NOT_CONFIGURED",
        hasCredentials: false,
        config: {},
      },
    ]);
  });

  it("renders provider cards and status", async () => {
    render(<IntegrationsSettingsPage />);
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /^Integrations$/i })).toBeInTheDocument();
    });
    expect(await screen.findByText("SMS / Twilio")).toBeInTheDocument();
    expect(screen.getByText("Not configured")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Back to Settings/i })).toHaveAttribute(
      "href",
      "/app/settings"
    );
  });
});
