import React, { ReactElement } from "react";
import { render, RenderOptions, cleanup } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Create a mock auth context
const mockAuthValue = {
  auth: {
    token: "test-token",
    userId: "user-123",
    email: "test@example.com",
    fullName: "Test User",
    tenants: [
      {
        tenantId: "tenant-123",
        tenantName: "Test Company",
        tenantSlug: "test-company",
        role: "ADMIN",
      },
    ],
    selectedTenantId: "tenant-123",
  },
  api: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
  },
  setAuthFromLogin: jest.fn(),
  selectTenant: jest.fn(),
  logout: jest.fn(),
};

// Mock the AuthContext module
jest.mock("@/lib/AuthContext", () => ({
  useAuth: () => mockAuthValue,
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

// Create a fresh query client for each test
const createTestQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
        staleTime: 0,
      },
      mutations: {
        retry: false,
      },
    },
  });

interface WrapperProps {
  children: React.ReactNode;
}

let testQueryClient: QueryClient;

function createWrapper() {
  testQueryClient = createTestQueryClient();
  return function Wrapper({ children }: WrapperProps) {
    return (
      <QueryClientProvider client={testQueryClient}>
        {children}
      </QueryClientProvider>
    );
  };
}

// Cleanup after each test
afterEach(() => {
  cleanup();
  if (testQueryClient) {
    testQueryClient.clear();
  }
});

const customRender = (
  ui: ReactElement,
  options?: Omit<RenderOptions, "wrapper">
) => {
  const Wrapper = createWrapper();
  return render(ui, { wrapper: Wrapper, ...options });
};

export * from "@testing-library/react";
export { customRender as render, mockAuthValue };
