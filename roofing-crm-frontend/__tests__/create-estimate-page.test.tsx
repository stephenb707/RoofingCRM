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
    fireEvent.change(screen.getByTestId("new-estimate-unitprice-0"), { target: { value: "1000" } });

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

  it("quantity and unit price stay empty while editing then normalize to 0 on blur", async () => {
    render(<NewEstimatePage />);

    const qtyInput = screen.getByTestId("new-estimate-quantity-0") as HTMLInputElement;
    const priceInput = screen.getByTestId("new-estimate-unitprice-0") as HTMLInputElement;

    fireEvent.change(qtyInput, { target: { value: "" } });
    fireEvent.change(priceInput, { target: { value: "" } });
    expect(qtyInput.value).toBe("");
    expect(priceInput.value).toBe("");

    fireEvent.blur(qtyInput);
    fireEvent.blur(priceInput);
    expect(qtyInput.value).toBe("0");
    expect(priceInput.value).toBe("0");
  });

  it("submit parses empty quantity/unit price as 0", async () => {
    render(<NewEstimatePage />);

    fireEvent.change(screen.getByPlaceholderText("Item name"), { target: { value: "Labor" } });
    fireEvent.change(screen.getByTestId("new-estimate-quantity-0"), { target: { value: "" } });
    fireEvent.change(screen.getByTestId("new-estimate-unitprice-0"), { target: { value: "" } });

    fireEvent.click(screen.getByRole("button", { name: /Create estimate/i }));

    await waitFor(() => {
      expect(mockedEstimatesApi.createEstimateForJob).toHaveBeenCalledWith(
        expect.anything(),
        "job-1",
        expect.objectContaining({
          items: expect.arrayContaining([
            expect.objectContaining({
              quantity: 0,
              unitPrice: 0,
            }),
          ]),
        })
      );
    });
  });
});
