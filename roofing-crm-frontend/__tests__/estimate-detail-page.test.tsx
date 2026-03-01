import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import EstimateDetailPage from "@/app/app/estimates/[estimateId]/page";
import * as estimatesApi from "@/lib/estimatesApi";
import * as tasksApi from "@/lib/tasksApi";
import type { EstimateDto } from "@/lib/types";

jest.mock("@/lib/estimatesApi");
jest.mock("@/lib/tasksApi");
const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/estimates/est-1",
  useParams: () => ({ estimateId: "est-1" }),
}));

const mockedEstimatesApi = estimatesApi as jest.Mocked<typeof estimatesApi>;
const mockedTasksApi = tasksApi as jest.Mocked<typeof tasksApi>;

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
    mockPush.mockClear();
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

  it("shows Share section and calls shareEstimate when Generate link clicked", async () => {
    const shareResponse = { token: "abc123", expiresAt: "2026-02-16T00:00:00Z" };
    mockedEstimatesApi.shareEstimate.mockResolvedValue(shareResponse);

    const writeTextMock = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: writeTextMock },
      configurable: true,
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    const generateBtn = screen.getByRole("button", { name: /Generate link/i });
    fireEvent.click(generateBtn);

    await waitFor(() => {
      expect(mockedEstimatesApi.shareEstimate).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.any(Object)
      );
    });

    await waitFor(() => {
      const linkInput = screen.getByTestId("share-link-input") as HTMLInputElement;
      expect(linkInput.value).toContain("/estimate/abc123");
    });
    expect(screen.getByRole("button", { name: /Copy link/i })).toBeInTheDocument();
    expect(writeTextMock).toHaveBeenCalledWith(expect.stringContaining("/estimate/abc123"));
    expect(screen.getByText("Link copied")).toBeInTheDocument();
  });

  it("modal preview button opens customer view and Done closes modal", async () => {
    const shareResponse = { token: "abc123", expiresAt: "2026-02-16T00:00:00Z" };
    mockedEstimatesApi.shareEstimate.mockResolvedValue(shareResponse);
    const writeTextMock = jest.fn().mockResolvedValue(undefined);
    const openMock = jest.fn();
    Object.defineProperty(window, "open", { value: openMock, configurable: true });
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: writeTextMock },
      configurable: true,
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    const generateBtn = screen.getByRole("button", { name: /Generate link/i });
    fireEvent.click(generateBtn);

    await waitFor(() => {
      expect(screen.getByTestId("share-link-input")).toBeInTheDocument();
    });

    expect(screen.getByRole("button", { name: /set follow-up in 2 days/i })).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /^done$/i })).toHaveLength(1);
    fireEvent.click(screen.getByTestId("share-next-step-preview"));

    await waitFor(() => {
      expect(openMock).toHaveBeenCalledWith(
        expect.stringContaining("/estimate/abc123"),
        "_blank",
        "noopener,noreferrer"
      );
    });

    // Re-open prompt by copying existing link and close with Done.
    fireEvent.click(screen.getByRole("button", { name: /Copy link/i }));
    await waitFor(() => {
      expect(screen.getByText("Link copied")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("share-next-step-done"));
    await waitFor(() => {
      expect(screen.queryByText("Link copied")).not.toBeInTheDocument();
    });
  });

  it("follow-up action creates task and navigates", async () => {
    const shareResponse = { token: "abc123", expiresAt: "2026-02-16T00:00:00Z" };
    mockedEstimatesApi.shareEstimate.mockResolvedValue(shareResponse);
    mockedTasksApi.createTask.mockResolvedValue({
      taskId: "task-1",
      title: "Follow up",
      status: "TODO",
      priority: "MEDIUM",
      createdAt: "2026-01-01T00:00:00Z",
      updatedAt: "2026-01-01T00:00:00Z",
    });
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: jest.fn().mockResolvedValue(undefined) },
      configurable: true,
    });

    render(<EstimateDetailPage />);
    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /generate link/i }));
    await waitFor(() => {
      expect(screen.getByRole("button", { name: /set follow-up in 2 days/i })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole("button", { name: /set follow-up in 2 days/i }));
    await waitFor(() => {
      expect(mockedTasksApi.createTask).toHaveBeenCalled();
    });
    expect(mockPush).toHaveBeenCalledWith("/app/tasks/task-1");
  });

  it("renders Edit Estimate link in Actions section", async () => {
    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    const editLinks = screen.getAllByRole("link", { name: /Edit Estimate/i });
    expect(editLinks[0]).toHaveAttribute("href", "/app/estimates/est-1/edit");
  });

  it("quantity and unit price draft allow empty while editing, then normalize on blur", async () => {
    render(<EstimateDetailPage />);
    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    const addQty = screen.getByTestId("add-quantity-input") as HTMLInputElement;
    const addUnitPrice = screen.getByTestId("add-unitprice-input") as HTMLInputElement;
    expect(addQty.value).toBe("0");
    expect(addUnitPrice.value).toBe("0");

    addQty.focus();
    addUnitPrice.focus();
    fireEvent.change(addQty, { target: { value: "" } });
    fireEvent.change(addUnitPrice, { target: { value: "" } });
    expect(addQty.value).toBe("");
    expect(addUnitPrice.value).toBe("");

    fireEvent.blur(addQty);
    fireEvent.blur(addUnitPrice);
    expect(addQty.value).toBe("0");
    expect(addUnitPrice.value).toBe("0");
  });
});
