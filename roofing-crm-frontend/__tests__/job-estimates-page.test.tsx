import React from "react";
import { render, screen, waitFor } from "./test-utils";
import JobEstimatesPage from "@/app/app/jobs/[jobId]/estimates/page";
import * as estimatesApi from "@/lib/estimatesApi";
import type { EstimateDto } from "@/lib/types";

jest.mock("@/lib/estimatesApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/job-1/estimates",
  useParams: () => ({ jobId: "job-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;

const mockEstimate: EstimateDto = {
  id: "est-1",
  jobId: "job-1",
  customerId: "cust-1",
  status: "DRAFT",
  title: "Roof Replacement",
  notes: null,
  issueDate: null,
  validUntil: null,
  items: [{ id: "i1", name: "Shingles", description: null, quantity: 10, unitPrice: 50, unit: "sq ft" }],
  subtotal: 500,
  total: 500,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-02T00:00:00Z",
};

describe("JobEstimatesPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedEstimatesApi.listEstimatesForJob.mockResolvedValue([mockEstimate]);
  });

  it("renders header and New Estimate button", async () => {
    render(<JobEstimatesPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimates")).toBeInTheDocument();
    });

    expect(screen.getByRole("link", { name: /\+ New Estimate/i })).toBeInTheDocument();
  });

  it("renders estimate rows when list returns data", async () => {
    render(<JobEstimatesPage />);

    await waitFor(() => {
      expect(screen.getByText("Roof Replacement")).toBeInTheDocument();
    });

    expect(screen.getByText("Draft")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /View/i })).toBeInTheDocument();
  });

  it("shows empty state when list is empty", async () => {
    mockedEstimatesApi.listEstimatesForJob.mockResolvedValue([]);

    render(<JobEstimatesPage />);

    await waitFor(() => {
      expect(screen.getByText("No estimates yet")).toBeInTheDocument();
    });
  });

  it("shows error state when listEstimatesForJob fails", async () => {
    mockedEstimatesApi.listEstimatesForJob.mockRejectedValue(new Error("Network error"));

    render(<JobEstimatesPage />);

    await waitFor(() => {
      expect(screen.getByText("Failed to load estimates")).toBeInTheDocument();
    });
  });
});
