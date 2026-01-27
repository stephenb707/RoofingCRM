import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import EditEstimatePage from "@/app/app/estimates/[estimateId]/edit/page";
import * as estimatesApi from "@/lib/estimatesApi";
import type { EstimateDto } from "@/lib/types";

jest.mock("@/lib/estimatesApi");
const mockPush = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/estimates/est-1/edit",
  useParams: () => ({ estimateId: "est-1" }),
}));

const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;

const mockEstimate: EstimateDto = {
  id: "est-1",
  jobId: "job-1",
  customerId: "cust-1",
  status: "DRAFT",
  title: "Estimate 1",
  notes: "Some notes",
  issueDate: "2024-01-15",
  validUntil: "2024-02-15",
  items: [
    { id: "item-1", name: "Shingles", description: null, quantity: 10, unitPrice: 50, unit: "sq ft" },
  ],
  subtotal: 500,
  total: 500,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("EditEstimatePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockedEstimatesApi.getEstimate.mockResolvedValue(mockEstimate);
  });

  it("renders edit form with prefilled fields from estimate", async () => {
    render(<EditEstimatePage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("Estimate 1")).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue("Some notes")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2024-01-15")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2024-02-15")).toBeInTheDocument();
  });

  it("calls updateEstimate with correct payload when saving", async () => {
    mockedEstimatesApi.updateEstimate.mockResolvedValue({
      ...mockEstimate,
      title: "Updated Title",
      notes: "Updated notes",
    });

    render(<EditEstimatePage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("Estimate 1")).toBeInTheDocument();
    });

    const titleInput = screen.getByLabelText(/Title/i);
    fireEvent.change(titleInput, { target: { value: "Updated Title" } });

    const notesInput = screen.getByLabelText(/Notes/i);
    fireEvent.change(notesInput, { target: { value: "Updated notes" } });

    const saveButton = screen.getByRole("button", { name: /Save Changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockedEstimatesApi.updateEstimate).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.objectContaining({
          title: "Updated Title",
          notes: "Updated notes",
          issueDate: "2024-01-15",
          validUntil: "2024-02-15",
          items: expect.arrayContaining([
            expect.objectContaining({ name: "Shingles" }),
          ]),
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/app/estimates/est-1");
    });
  });

  it("shows loading state while fetching estimate", () => {
    mockedEstimatesApi.getEstimate.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<EditEstimatePage />);

    expect(screen.getByText("Loading estimate…")).toBeInTheDocument();
  });

  it("shows error state when estimate fails to load", async () => {
    const error = new Error("Failed to load");
    mockedEstimatesApi.getEstimate.mockRejectedValue(error);

    render(<EditEstimatePage />);

    await waitFor(() => {
      expect(screen.getByText("Failed to load estimate")).toBeInTheDocument();
    });
  });

  it("handles empty/null fields correctly", async () => {
    const estimateWithNulls: EstimateDto = {
      ...mockEstimate,
      title: null,
      notes: null,
      issueDate: null,
      validUntil: null,
    };
    mockedEstimatesApi.getEstimate.mockResolvedValue(estimateWithNulls);

    render(<EditEstimatePage />);

    await waitFor(() => {
      const titleInput = screen.getByLabelText(/Title/i) as HTMLInputElement;
      expect(titleInput.value).toBe("");
    });

    const notesInput = screen.getByLabelText(/Notes/i) as HTMLTextAreaElement;
    expect(notesInput.value).toBe("");
  });

  it("disables save button while saving", async () => {
    mockedEstimatesApi.updateEstimate.mockImplementation(() => new Promise(() => {})); // Never resolves

    render(<EditEstimatePage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("Estimate 1")).toBeInTheDocument();
    });

    const saveButton = screen.getByRole("button", { name: /Save Changes/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(saveButton).toBeDisabled();
      expect(screen.getByText("Saving…")).toBeInTheDocument();
    });
  });
});
