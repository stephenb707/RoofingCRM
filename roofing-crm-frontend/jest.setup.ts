import "@testing-library/jest-dom";

// Mock STOMP client to avoid real WebSocket/SockJS and TextEncoder in JSDOM
jest.mock("@/lib/stompClient", () => ({
  createStompClient: jest.fn(() => ({
    activate: jest.fn(),
    deactivate: jest.fn(),
    subscribe: jest.fn(() => ({ unsubscribe: jest.fn() })),
  })),
  activityTopic: jest.fn(
    (tenantId: string, entityType: string, entityId: string) =>
      `/topic/tenants/${tenantId}/activity/${entityType}/${entityId}`
  ),
}));

// Suppress console.error for act() warnings during React Query tests
const originalError = console.error;
beforeAll(() => {
  console.error = (...args: unknown[]) => {
    if (
      typeof args[0] === "string" &&
      args[0].includes("Warning: An update to") &&
      args[0].includes("was not wrapped in act")
    ) {
      return;
    }
    originalError.call(console, ...args);
  };
});

afterAll(() => {
  console.error = originalError;
});

// Mock next/navigation (usePathname reads from pathnameState for Jobs tests)
jest.mock("next/navigation", () => {
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const pathnameState = require("@/__tests__/pathnameState");
  return {
    useRouter: () => ({
      push: jest.fn(),
      replace: jest.fn(),
      back: jest.fn(),
      forward: jest.fn(),
      refresh: jest.fn(),
      prefetch: jest.fn(),
    }),
    usePathname: () => pathnameState.__pathname,
    useParams: () => ({}),
    useSearchParams: () => new URLSearchParams(),
  };
});
