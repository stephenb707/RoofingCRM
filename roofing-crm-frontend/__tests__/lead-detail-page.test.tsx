import React from "react";
import { render, screen, waitFor } from "./test-utils";
import LeadDetailPage from "@/app/app/leads/[leadId]/page";
import * as leadsApi from "@/lib/leadsApi";
import { LeadDto } from "@/lib/types";

jest.mock("@/lib/leadsApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/leads/lead-1",
  useParams: () => ({ leadId: "lead-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockLead: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  status: "NEW",
  source: "WEBSITE",
  leadNotes: "Notes",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  preferredContactMethod: "Phone",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "John",
  customerLastName: "Doe",
  customerPhone: "555-123-4567",
  customerEmail: "john@example.com",
};

describe("LeadDetailPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedLeadsApi.getLead.mockResolvedValue(mockLead);
  });

  it("renders customer name from customerFirstName and customerLastName", async () => {
    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("John Doe");
    });
  });

  it("shows Create Job and Edit Lead buttons", async () => {
    render(<LeadDetailPage />);

    const createJobLink = await screen.findByRole("link", { name: "Create Job" });
    expect(createJobLink).toHaveAttribute("href", "/app/jobs/new?leadId=lead-1");

    const editLink = await screen.findByRole("link", { name: "Edit Lead" });
    expect(editLink).toHaveAttribute("href", "/app/leads/lead-1/edit");
  });

  it("renders — when customer name fields are missing", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      customerFirstName: undefined,
      customerLastName: undefined,
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("—");
    });
    expect(screen.queryByText(/undefined/)).not.toBeInTheDocument();
  });
});
