import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import EstimateDetailPage from "@/app/app/estimates/[estimateId]/page";
import * as estimatesApi from "@/lib/estimatesApi";
import type { EstimateDto } from "@/lib/types";

jest.mock("@/lib/estimatesApi");
const mockReplace = jest.fn();
const mockPush = jest.fn();
let mockSearchParams = new URLSearchParams();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: mockReplace, back: jest.fn() }),
  usePathname: () => "/app/estimates/est-1",
  useParams: () => ({ estimateId: "est-1" }),
  useSearchParams: () => mockSearchParams,
}));

const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;

const mockEstimate: EstimateDto = {
  id: "est-1",
  jobId: "job-1",
  customerId: "cust-1",
  status: "DRAFT",
  title: "Estimate 1",
  notes: null,
  issueDate: null,
  validUntil: null,
  items: [
    { id: "item-1", name: "Shingles", description: null, quantity: 10, unitPrice: 50, unit: "sq ft" },
  ],
  subtotal: 500,
  total: 500,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("EstimateDetailPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockReplace.mockClear();
    mockPush.mockClear();
    mockSearchParams = new URLSearchParams();
    mockedEstimatesApi.getEstimate.mockResolvedValue(mockEstimate);
  });

  it("renders estimate overview and items", async () => {
    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    expect(screen.getByText("Shingles")).toBeInTheDocument();
    expect(screen.getAllByText("Draft").length).toBeGreaterThan(0);
  });

  it("calls updateEstimateStatus when clicking a status button", async () => {
    mockedEstimatesApi.updateEstimateStatus.mockResolvedValue({ ...mockEstimate, status: "SENT" });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    const sentBtn = screen.getByRole("button", { name: /^Sent$/i });
    fireEvent.click(sentBtn);

    await waitFor(() => {
      expect(mockedEstimatesApi.updateEstimateStatus).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        "SENT"
      );
    });
  });

  it("calls updateEstimate when adding an item", async () => {
    mockedEstimatesApi.updateEstimate.mockResolvedValue({
      ...mockEstimate,
      items: [
        ...(mockEstimate.items ?? []),
        { id: "i2", name: "Labor", description: null, quantity: 1, unitPrice: 1000, unit: "job" },
      ],
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Shingles")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText("Name"), { target: { value: "Labor" } });
    fireEvent.change(screen.getByPlaceholderText("Unit price"), { target: { value: "1000" } });

    const addBtn = screen.getByRole("button", { name: /^Add$/i });
    fireEvent.click(addBtn);

    await waitFor(() => {
      expect(mockedEstimatesApi.updateEstimate).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.objectContaining({
          items: expect.any(Array),
        })
      );
    });
  });

  it("calls updateEstimate when deleting an item", async () => {
    const estimateWithTwoItems: EstimateDto = {
      ...mockEstimate,
      items: [
        { id: "item-1", name: "Shingles", description: null, quantity: 10, unitPrice: 50, unit: "sq ft" },
        { id: "item-2", name: "Labor", description: null, quantity: 1, unitPrice: 1000, unit: "job" },
      ],
    };
    mockedEstimatesApi.getEstimate.mockResolvedValue(estimateWithTwoItems);
    mockedEstimatesApi.updateEstimate.mockResolvedValue({ ...estimateWithTwoItems, items: [estimateWithTwoItems.items![0]] });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Labor")).toBeInTheDocument();
    });

    const deleteBtns = screen.getAllByRole("button", { name: /Delete/i });
    fireEvent.click(deleteBtns[1]); // delete Labor

    await waitFor(() => {
      expect(mockedEstimatesApi.updateEstimate).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.objectContaining({
          items: expect.arrayContaining([expect.objectContaining({ name: "Shingles" })]),
        })
      );
    });
  });

  it("toggles edit mode when clicking Edit Estimate button in Actions section", async () => {
    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    const editBtn = screen.getByRole("button", { name: /Edit Estimate/i });
    fireEvent.click(editBtn);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Estimate title")).toBeInTheDocument();
    });
  });

  it("saves header changes and calls updateEstimate with correct payload", async () => {
    mockedEstimatesApi.updateEstimate.mockResolvedValue({
      ...mockEstimate,
      title: "Updated Title",
      notes: "Updated notes",
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Edit Estimate/i }));

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Estimate title")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText("Estimate title"), { target: { value: "Updated Title" } });
    fireEvent.change(screen.getByPlaceholderText("Notes (optional)"), { target: { value: "Updated notes" } });

    fireEvent.click(screen.getByRole("button", { name: /Save Changes/i }));

    await waitFor(() => {
      expect(mockedEstimatesApi.updateEstimate).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.objectContaining({
          title: "Updated Title",
          notes: "Updated notes",
          items: expect.any(Array),
        })
      );
    });
  });

  it("shows Cancel Edit button in Actions when in edit mode", async () => {
    mockSearchParams = new URLSearchParams("edit=1");

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Estimate title")).toBeInTheDocument();
    });

    const cancelBtn = screen.getByRole("button", { name: /Cancel Edit/i });
    expect(cancelBtn).toBeInTheDocument();

    const saveBtn = screen.getByRole("button", { name: /Save Changes/i });
    expect(saveBtn).toBeInTheDocument();
  });

  it("starts in edit mode when edit=1 query param is present", async () => {
    mockSearchParams = new URLSearchParams("edit=1");

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Estimate title")).toBeInTheDocument();
    });
  });
});
