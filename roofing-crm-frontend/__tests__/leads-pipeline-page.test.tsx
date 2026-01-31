import React from "react";
import { render, screen, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import LeadsPipelinePage from "@/app/app/leads/pipeline/page";
import * as leadsApi from "@/lib/leadsApi";
import { LeadDto } from "@/lib/types";

jest.mock("@/lib/leadsApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/leads/pipeline",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockLeadNew: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  status: "NEW",
  source: "WEBSITE",
  leadNotes: "",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  preferredContactMethod: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "Alice",
  customerLastName: "Smith",
};

const mockLeadContacted: LeadDto = {
  ...mockLeadNew,
  id: "lead-2",
  status: "CONTACTED",
  customerFirstName: "Bob",
  customerLastName: "Jones",
  propertyAddress: { line1: "456 Oak Ave", city: "Boulder", state: "CO" },
};

describe("LeadsPipelinePage", () => {
  const pipelineResponse = {
    content: [mockLeadNew, mockLeadContacted],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 200,
    first: true,
    last: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedLeadsApi.listLeads.mockImplementation(() => Promise.resolve(pipelineResponse));
  });

  it("renders grouped columns and lead cards when listLeads returns leads across multiple statuses", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Pipeline");
    });

    expect(mockedLeadsApi.listLeads).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ page: 0, size: 200 })
    );

    expect(screen.getByRole("heading", { name: "New" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contacted" })).toBeInTheDocument();
    expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
    expect(screen.getByText("123 Main St, Denver, CO")).toBeInTheDocument();
    expect(screen.getByText("456 Oak Ave, Boulder, CO")).toBeInTheDocument();
  });

  it("changing a lead's status calls updateLeadStatus with correct args", async () => {
    mockedLeadsApi.updateLeadStatus.mockResolvedValue({
      ...mockLeadNew,
      status: "CONTACTED",
    });

    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    const select = screen.getByLabelText(/Status for Alice Smith/i);
    await userEvent.selectOptions(select, "Contacted");

    await waitFor(() => {
      expect(mockedLeadsApi.updateLeadStatus).toHaveBeenCalledWith(
        expect.anything(),
        "lead-1",
        "CONTACTED"
      );
    });
  });

  it("optimistic UI: after change, the lead appears under the new column", async () => {
    mockedLeadsApi.updateLeadStatus.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({
        ...mockLeadNew,
        status: "CONTACTED",
      }), 100))
    );

    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    const newColumn = screen.getByRole("heading", { name: "New" }).closest(".flex-shrink-0");
    expect(newColumn).toHaveTextContent("Alice Smith");

    const select = screen.getByLabelText(/Status for Alice Smith/i);
    await userEvent.selectOptions(select, "Contacted");

    await waitFor(
      () => {
        const contactedColumn = screen.getByRole("heading", { name: "Contacted" }).closest(".flex-shrink-0");
        expect(contactedColumn).toHaveTextContent("Alice Smith");
      },
      { timeout: 500 }
    );
  });
});
