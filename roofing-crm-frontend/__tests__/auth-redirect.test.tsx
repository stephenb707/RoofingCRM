import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import LoginPage from "@/app/auth/login/page";

const mockReplace = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: mockReplace, back: jest.fn(), forward: jest.fn(), refresh: jest.fn(), prefetch: jest.fn() }),
  usePathname: () => "/auth/login",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

jest.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    auth: {
      token: "test-token",
      userId: "user-1",
      email: "u@example.com",
      fullName: "User",
      tenants: [{ tenantId: "t1", tenantName: "T1", tenantSlug: "t1", role: "USER" }],
      selectedTenantId: null, // token present but no tenant selected
    },
    api: { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), interceptors: { request: { use: jest.fn() }, response: { use: jest.fn() } } },
    setAuthFromLogin: jest.fn(),
    selectTenant: jest.fn(),
    logout: jest.fn(),
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

function createWrapper() {
  const q = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return <QueryClientProvider client={q}>{children}</QueryClientProvider>;
  };
}

describe("Auth redirect (login)", () => {
  beforeEach(() => {
    mockReplace.mockClear();
  });

  it("redirects to /auth/select-tenant when token exists but selectedTenantId is null", async () => {
    render(<LoginPage />, { wrapper: createWrapper() });

    expect(screen.getByRole("button", { name: /Sign in/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/auth/select-tenant");
    });
  });
});
