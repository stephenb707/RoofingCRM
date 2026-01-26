import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import NewEstimatePage from "@/app/app/jobs/[jobId]/estimates/new/page";
import * as estimatesApi from "@/lib/estimatesApi";
import type { EstimateDto } from "@/lib/types";

const mockPush = jest.fn();

jest.mock("@/lib/estimatesApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/job-1/estimates/new",
  useParams: () => ({ jobId: "job-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;

const createdEstimate: EstimateDto = {
  id: "est-new",
  jobId: "job-1",
  customerId: "cust-1",
  status: "DRAFT",
  title: "New Est",
  notes: null,
  issueDate: null,
  validUntil: null,
  items: [{ id: "i1", name: "Labor", description: null, quantity: 1, unitPrice: 1000, unit: "job" }],
  subtotal: 1000,
  total: 1000,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("NewEstimatePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedEstimatesApi.createEstimateForJob.mockResolvedValue(createdEstimate);
  });

  it("calls createEstimateForJob on submit and navigates to estimate detail", async () => {
    render(<NewEstimatePage />);

    fireEvent.change(screen.getByPlaceholderText("Item name"), { target: { value: "Labor" } });
    const spinbuttons = screen.getAllByRole("spinbutton");
    fireEvent.change(spinbuttons[1], { target: { value: "1000" } }); // unit price

    const submit = screen.getByRole("button", { name: /Create estimate/i });
    fireEvent.click(submit);

    await waitFor(() => {
      expect(mockedEstimatesApi.createEstimateForJob).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        expect.objectContaining({
          items: expect.any(Array),
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/app/estimates/est-new");
    });
  });
});
