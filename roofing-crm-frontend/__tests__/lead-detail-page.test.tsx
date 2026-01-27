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

  it("shows Convert to Job and Edit Lead buttons when lead is not LOST", async () => {
    render(<LeadDetailPage />);

    const convertJobLink = await screen.findByRole("link", { name: /convert to job/i });
    expect(convertJobLink).toHaveAttribute("href", "/app/leads/lead-1/convert");

    const editLink = await screen.findByRole("link", { name: /edit lead/i });
    expect(editLink).toHaveAttribute("href", "/app/leads/lead-1/edit");
  });

  it("hides Convert to Job button when lead status is LOST", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      status: "LOST",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.queryByRole("link", { name: /convert to job/i })).not.toBeInTheDocument();
    });

    const editLink = await screen.findByRole("link", { name: /edit lead/i });
    expect(editLink).toBeInTheDocument();
  });

  it("when convertedJobId exists shows converted banner and View Job / Create Estimate, hides Convert to Job", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      convertedJobId: "job-99",
      status: "WON",
    });

    render(<LeadDetailPage />);

    await waitFor(() => {
      expect(screen.getByText(/converted to job/i)).toBeInTheDocument();
    });

    expect(screen.queryByRole("link", { name: /convert to job/i })).not.toBeInTheDocument();

    const viewJobLinks = screen.getAllByRole("link", { name: /view job/i });
    expect(viewJobLinks.length).toBeGreaterThanOrEqual(1);
    expect(viewJobLinks[0]).toHaveAttribute("href", "/app/jobs/job-99");

    const createEstimateLinks = screen.getAllByRole("link", { name: /create estimate/i });
    expect(createEstimateLinks.length).toBeGreaterThanOrEqual(1);
    expect(createEstimateLinks[0]).toHaveAttribute("href", "/app/jobs/job-99/estimates/new");

    const editLink = screen.getByRole("link", { name: /edit lead/i });
    expect(editLink).toBeInTheDocument();
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
