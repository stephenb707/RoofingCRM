import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import LoginPage from "@/app/auth/login/page";
import RegisterPage from "@/app/auth/register/page";
import AcceptInvitePage from "@/app/auth/accept-invite/page";
import * as teamApi from "@/lib/teamApi";

const mockPush = jest.fn();
const mockReplace = jest.fn();
const mockPost = jest.fn();
const mockSetAuthFromLogin = jest.fn();
const mockAddTenant = jest.fn();
const mockSelectTenant = jest.fn();

let searchParamsString = "";
let authState = {
  token: null as string | null,
  userId: null as string | null,
  email: null as string | null,
  fullName: null as string | null,
  tenants: [] as Array<{
    tenantId: string;
    tenantName: string;
    tenantSlug: string;
    role: string;
  }>,
  selectedTenantId: null as string | null,
};

jest.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: mockReplace,
    back: jest.fn(),
    forward: jest.fn(),
    refresh: jest.fn(),
    prefetch: jest.fn(),
  }),
  useSearchParams: () => new URLSearchParams(searchParamsString),
  usePathname: () => "/auth/login",
  useParams: () => ({}),
}));

jest.mock("@/lib/AuthContext", () => ({
  useAuth: () => ({
    auth: authState,
    api: {
      get: jest.fn(),
      post: mockPost,
      put: jest.fn(),
      delete: jest.fn(),
      interceptors: {
        request: { use: jest.fn() },
        response: { use: jest.fn() },
      },
    },
    setAuthFromLogin: mockSetAuthFromLogin,
    addTenant: mockAddTenant,
    selectTenant: mockSelectTenant,
    logout: jest.fn(),
  }),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

jest.mock("@/lib/teamApi");
const mockedTeamApi = teamApi as jest.Mocked<typeof teamApi>;

function renderWithQueryClient(ui: React.ReactElement) {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>
  );
}

describe("Invite auth flow", () => {
  beforeEach(() => {
    searchParamsString = "";
    authState = {
      token: null,
      userId: null,
      email: null,
      fullName: null,
      tenants: [],
      selectedTenantId: null,
    };
    mockPush.mockReset();
    mockReplace.mockReset();
    mockPost.mockReset();
    mockSetAuthFromLogin.mockReset();
    mockAddTenant.mockReset();
    mockSelectTenant.mockReset();
    mockedTeamApi.acceptInvite.mockReset();
  });

  it("login preserves next in the create account link", () => {
    searchParamsString = `next=${encodeURIComponent("/auth/accept-invite?token=abc123")}`;

    renderWithQueryClient(<LoginPage />);

    expect(screen.getByRole("link", { name: /Create one/i })).toHaveAttribute(
      "href",
      "/auth/register?next=%2Fauth%2Faccept-invite%3Ftoken%3Dabc123"
    );
  });

  it("register invite mode hides tenant name and uses register-with-invite", async () => {
    searchParamsString = `next=${encodeURIComponent("/auth/accept-invite?token=abc123")}`;
    mockPost.mockResolvedValue({
      data: {
        token: "jwt-token",
        userId: "user-1",
        email: "invitee@example.com",
        fullName: "Invited User",
        tenants: [
          {
            tenantId: "tenant-1",
            tenantName: "Invite Tenant",
            tenantSlug: "invite-tenant",
            role: "SALES",
          },
        ],
      },
    });

    renderWithQueryClient(<RegisterPage />);

    expect(screen.queryByPlaceholderText(/Your Roofing Co\./i)).not.toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText(/John Doe/i), {
      target: { value: "Invited User" },
    });
    fireEvent.change(screen.getByPlaceholderText(/you@company\.com/i), {
      target: { value: "invitee@example.com" },
    });
    fireEvent.change(screen.getByPlaceholderText(/••••••••/i), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Create account/i }));

    await waitFor(() => {
      expect(mockPost).toHaveBeenCalledWith("/api/v1/auth/register-with-invite", {
        fullName: "Invited User",
        email: "invitee@example.com",
        password: "password123",
        token: "abc123",
      });
    });

    expect(mockSetAuthFromLogin).toHaveBeenCalled();
    expect(mockPush).toHaveBeenCalledWith("/app/customers");
  });

  it("accept invite redirects unauthenticated users to login while preserving token", async () => {
    searchParamsString = "token=abc123";

    renderWithQueryClient(<AcceptInvitePage />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith(
        "/auth/login?next=%2Fauth%2Faccept-invite%3Ftoken%3Dabc123"
      );
    });
  });

  it("accept invite still works for authenticated users", async () => {
    searchParamsString = "token=abc123";
    authState = {
      token: "jwt-token",
      userId: "user-1",
      email: "invitee@example.com",
      fullName: "Invited User",
      tenants: [],
      selectedTenantId: null,
    };
    mockedTeamApi.acceptInvite.mockResolvedValue({
      tenantId: "tenant-1",
      tenantName: "Invite Tenant",
      tenantSlug: "invite-tenant",
      role: "SALES",
    });

    renderWithQueryClient(<AcceptInvitePage />);

    await waitFor(() => {
      expect(mockedTeamApi.acceptInvite).toHaveBeenCalledWith(
        expect.anything(),
        "abc123"
      );
    });

    expect(mockAddTenant).toHaveBeenCalledWith({
      tenantId: "tenant-1",
      tenantName: "Invite Tenant",
      tenantSlug: "invite-tenant",
      role: "SALES",
    });
    expect(mockSelectTenant).toHaveBeenCalledWith("tenant-1");
    expect(mockReplace).toHaveBeenCalledWith("/app/customers");
  });
});
