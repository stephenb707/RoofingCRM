import React from "react";
import { render, screen } from "./test-utils";
import AppLayout from "@/app/app/layout";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ replace: jest.fn(), push: jest.fn(), back: jest.fn(), refresh: jest.fn(), prefetch: jest.fn() }),
  usePathname: () => "/app",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

describe("AppLayout navigation", () => {
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
});
