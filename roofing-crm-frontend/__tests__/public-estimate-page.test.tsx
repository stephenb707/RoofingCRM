import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import PublicEstimatePage from "@/app/estimate/[token]/page";
import * as estimatesApi from "@/lib/estimatesApi";
import type { PublicEstimateDto } from "@/lib/types";

jest.mock("@/lib/estimatesApi");
jest.mock("next/navigation", () => ({
  useParams: () => ({ token: "abc123token" }),
}));

const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;

const mockPublicEstimate: PublicEstimateDto = {
  estimateNumber: "EST-1001",
  status: "SENT",
  title: "Roof Estimate",
  notes: null,
  issueDate: null,
  validUntil: null,
  subtotal: 5000,
  total: 5000,
  publicExpiresAt: "2026-02-16T00:00:00Z",
  customerName: "Jane Doe",
  customerAddress: "123 Main St, Chicago, IL",
  items: [
    { name: "Shingles", description: null, quantity: 100, unitPrice: 50, unit: "sq ft", lineTotal: 5000 },
  ],
};

describe("PublicEstimatePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedEstimatesApi.getPublicEstimate.mockResolvedValue(mockPublicEstimate);
  });

  it("renders estimate and decision form when status is SENT", async () => {
    render(<PublicEstimatePage />);

    await waitFor(() => {
      expect(screen.getByText("Roof Estimate")).toBeInTheDocument();
    });

    expect(screen.getByText(/EST-1001/)).toBeInTheDocument();
    expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    expect(screen.getByText("123 Main St, Chicago, IL")).toBeInTheDocument();
    expect(screen.getByText("Shingles")).toBeInTheDocument();
    expect(screen.getByLabelText(/Your name/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Accept/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Reject/i })).toBeInTheDocument();
  });

  it("calls decidePublicEstimate when Accept is clicked with signer name", async () => {
    const accepted = { ...mockPublicEstimate, status: "ACCEPTED" as const };
    mockedEstimatesApi.decidePublicEstimate.mockResolvedValue(accepted);

    render(<PublicEstimatePage />);

    await waitFor(() => {
      expect(screen.getByText("Roof Estimate")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Your name/i), { target: { value: "John Homeowner" } });
    fireEvent.click(screen.getByRole("button", { name: /Accept/i }));

    await waitFor(() => {
      expect(mockedEstimatesApi.decidePublicEstimate).toHaveBeenCalledWith(
        "abc123token",
        expect.objectContaining({
          decision: "ACCEPTED",
          signerName: "John Homeowner",
        })
      );
    });
  });

  it("calls decidePublicEstimate when Reject is clicked", async () => {
    const rejected = { ...mockPublicEstimate, status: "REJECTED" as const };
    mockedEstimatesApi.decidePublicEstimate.mockResolvedValue(rejected);

    render(<PublicEstimatePage />);

    await waitFor(() => {
      expect(screen.getByText("Roof Estimate")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/Your name/i), { target: { value: "John Homeowner" } });
    fireEvent.click(screen.getByRole("button", { name: /Reject/i }));

    await waitFor(() => {
      expect(mockedEstimatesApi.decidePublicEstimate).toHaveBeenCalledWith(
        "abc123token",
        expect.objectContaining({
          decision: "REJECTED",
          signerName: "John Homeowner",
        })
      );
    });
  });

  it("shows Link expired when getPublicEstimate fails with expired error", async () => {
    const err = new Error("Link expired");
    (err as { response?: { status?: number } }).response = { status: 410 };
    mockedEstimatesApi.getPublicEstimate.mockRejectedValue(err);

    render(<PublicEstimatePage />);

    await waitFor(() => {
      expect(screen.getByText("Link expired")).toBeInTheDocument();
    });
  });

  it("hides decision form when estimate is already ACCEPTED", async () => {
    mockedEstimatesApi.getPublicEstimate.mockResolvedValue({
      ...mockPublicEstimate,
      status: "ACCEPTED",
    });

    render(<PublicEstimatePage />);

    await waitFor(() => {
      expect(screen.getByText("Roof Estimate")).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Your name/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Accept/i })).not.toBeInTheDocument();
  });
});
