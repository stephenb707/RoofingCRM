import React from "react";
import { render, screen, waitFor, fireEvent, within } from "./test-utils";
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
  customerName: "Jane Doe",
  customerEmail: "jane@example.com",
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

  it("SENT estimate Next Best Actions has Send Email, Generate Link, and Set follow-up; no Share link", async () => {
    mockedEstimatesApi.getEstimate.mockResolvedValue({ ...mockEstimate, status: "SENT" });
    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    expect(screen.queryByRole("link", { name: /^Share link$/i })).not.toBeInTheDocument();
    expect(screen.getByTestId("nba-estimate-send-email")).toBeInTheDocument();
    expect(screen.getByTestId("nba-estimate-generate-link")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Set follow-up/i })).toHaveAttribute(
      "href",
      "/app/tasks/new?jobId=job-1&customerId=cust-1"
    );
  });

  it("Share section shows Send Email (primary) and Generate Link when no public link; stale Share Link absent", async () => {
    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    expect(screen.queryByText(/send by email for the fastest delivery/i)).not.toBeInTheDocument();
    const sendBtn = screen.getByTestId("estimate-share-send-email");
    const generateBtn = screen.getByTestId("estimate-share-generate-link");
    expect(sendBtn).toHaveTextContent(/^Send Email$/);
    expect(generateBtn).toHaveTextContent(/^Generate Link$/);
    expect(sendBtn.compareDocumentPosition(generateBtn) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(sendBtn.className).toMatch(/bg-sky-600/);
    expect(generateBtn.className).toMatch(/border-sky-300/);
    expect(screen.queryByRole("button", { name: /Share link/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId("estimate-share-refresh-link")).not.toBeInTheDocument();
  });

  it("Next Best Actions Generate Link triggers shareEstimate like the Share section", async () => {
    mockedEstimatesApi.shareEstimate.mockResolvedValue({
      token: "nba-token",
      expiresAt: "2026-02-16T00:00:00Z",
    });
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: jest.fn().mockResolvedValue(undefined) },
      configurable: true,
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("nba-estimate-generate-link"));

    await waitFor(() => {
      expect(mockedEstimatesApi.shareEstimate).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.objectContaining({ expiresInDays: 14 })
      );
    });
  });

  it("calls shareEstimate from Generate Link; then shows Send Email, Refresh Link, Copy Link; stale Share Link absent", async () => {
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

    const generateBtn = screen.getByTestId("estimate-share-generate-link");
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
    expect(screen.getAllByRole("button", { name: /^Send Email$/ })).toHaveLength(2);
    expect(screen.getByTestId("estimate-share-refresh-link")).toHaveTextContent(/^Refresh Link$/);
    expect(screen.getByTestId("estimate-share-copy-link")).toHaveTextContent(/^Copy Link$/);
    expect(screen.queryByRole("button", { name: /Share link/i })).not.toBeInTheDocument();
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

    const generateBtn = screen.getByTestId("estimate-share-generate-link");
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
    fireEvent.click(screen.getByTestId("estimate-share-copy-link"));
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
    fireEvent.click(screen.getByTestId("estimate-share-generate-link"));
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

  it("opens send-email modal and submits estimate email request", async () => {
    mockedEstimatesApi.sendEstimateEmail.mockResolvedValue({
      success: true,
      sentAt: "2026-02-16T00:00:00Z",
      publicUrl: "http://localhost:3000/estimate/abc123",
      reusedExistingToken: false,
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("nba-estimate-send-email"));
    const dialog = screen.getByRole("dialog", { name: /send estimate by email/i });
    fireEvent.change(within(dialog).getByLabelText(/recipient email/i), {
      target: { value: "customer@example.com" },
    });
    fireEvent.change(within(dialog).getByLabelText(/recipient name/i), {
      target: { value: "Jane" },
    });
    fireEvent.change(within(dialog).getByLabelText(/^message \(optional\)$/i), {
      target: { value: "Please review this estimate." },
    });
    fireEvent.click(within(dialog).getByRole("button", { name: /^Send email$/i }));

    await waitFor(() => {
      expect(mockedEstimatesApi.sendEstimateEmail).toHaveBeenCalledWith(
        expect.anything(),
        "est-1",
        expect.objectContaining({
          recipientEmail: "customer@example.com",
          recipientName: "Jane",
          message: "Please review this estimate.",
          expiresInDays: 14,
        })
      );
    });

    expect(screen.getByText("Email sent to customer@example.com.")).toBeInTheDocument();
  });

  it("prefills estimate send-email modal from customer and keeps fields editable", async () => {
    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("nba-estimate-send-email"));
    const dialog = screen.getByRole("dialog", { name: /send estimate by email/i });
    const emailInput = within(dialog).getByLabelText(/recipient email/i) as HTMLInputElement;
    const nameInput = within(dialog).getByLabelText(/recipient name/i) as HTMLInputElement;

    expect(emailInput.value).toBe("jane@example.com");
    expect(nameInput.value).toBe("Jane Doe");

    fireEvent.change(emailInput, { target: { value: "edited@example.com" } });
    fireEvent.change(nameInput, { target: { value: "Edited Name" } });

    expect(emailInput.value).toBe("edited@example.com");
    expect(nameInput.value).toBe("Edited Name");
  });

  it("leaves estimate send-email modal fields blank when customer info is missing", async () => {
    mockedEstimatesApi.getEstimate.mockResolvedValue({
      ...mockEstimate,
      customerName: null,
      customerEmail: null,
    });

    render(<EstimateDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Estimate 1")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId("nba-estimate-send-email"));
    const dialog = screen.getByRole("dialog", { name: /send estimate by email/i });

    expect((within(dialog).getByLabelText(/recipient email/i) as HTMLInputElement).value).toBe("");
    expect((within(dialog).getByLabelText(/recipient name/i) as HTMLInputElement).value).toBe("");
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
