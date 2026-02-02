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
  pipelinePosition: 0,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "Alice",
  customerLastName: "Smith",
};

const mockLeadContacted: LeadDto = {
  ...mockLeadNew,
  id: "lead-2",
  status: "CONTACTED",
  pipelinePosition: 0,
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

  it("does not have a status dropdown on cards", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Status for Alice Smith/i)).not.toBeInTheDocument();
  });

  it("shows status badge on each card", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.getByTestId("pipeline-card-status-lead-1")).toHaveTextContent("New");
    expect(screen.getByTestId("pipeline-card-status-lead-2")).toHaveTextContent("Contacted");
  });

  it("shows tip with link to open leads", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Pipeline");
    });

    expect(screen.getByText(/Drag leads to move them between stages/)).toBeInTheDocument();
    const openLink = screen.getByRole("link", { name: /open a lead/i });
    expect(openLink).toHaveAttribute("href", "/app/leads");
  });
});
