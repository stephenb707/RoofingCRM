import React from "react";
import { render, screen, waitFor, fireEvent, within } from "./test-utils";
import LeadsPipelinePage from "@/app/app/leads/pipeline/page";
import * as leadsApi from "@/lib/leadsApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";
import { LeadDto } from "@/lib/types";

jest.mock("@/lib/leadsApi");
jest.mock("@/lib/pipelineStatusesApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/leads/pipeline",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

const defNew = {
  id: "def-new",
  pipelineType: "LEAD" as const,
  systemKey: "NEW",
  label: "New",
  sortOrder: 0,
  builtIn: true,
  active: true,
};

const defContacted = {
  id: "def-contacted",
  pipelineType: "LEAD" as const,
  systemKey: "CONTACTED",
  label: "Contacted",
  sortOrder: 1,
  builtIn: true,
  active: true,
};

const mockLeadNew: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  statusDefinitionId: defNew.id,
  statusKey: "NEW",
  statusLabel: "New",
  source: "WEBSITE",
  leadNotes: "",
  propertyAddress: { line1: "123 Main St", city: "Denver", state: "CO" },
  pipelinePosition: 0,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "Alice",
  customerLastName: "Smith",
  customerPhone: "5551234567",
};

const mockLeadContacted: LeadDto = {
  ...mockLeadNew,
  id: "lead-2",
  statusDefinitionId: defContacted.id,
  statusKey: "CONTACTED",
  statusLabel: "Contacted",
  pipelinePosition: 0,
  customerFirstName: "Bob",
  customerLastName: "Jones",
  propertyAddress: { line1: "456 Oak Ave", city: "Boulder", state: "CO" },
  customerPhone: "3035550100",
  customerEmail: "bob@example.com",
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
    mockedPipelineApi.listPipelineStatuses.mockResolvedValue([defNew, defContacted]);
  });

  it("renders columns from pipeline status definitions and groups leads by statusDefinitionId", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Pipeline");
    });

    expect(mockedPipelineApi.listPipelineStatuses).toHaveBeenCalledWith(expect.anything(), "LEAD");
    expect(mockedLeadsApi.listLeads).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ page: 0, size: 200 })
    );

    expect(screen.getByRole("heading", { name: "New" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contacted" })).toBeInTheDocument();
    expect(screen.getByTestId(`pipeline-col-${defNew.id}`)).toBeInTheDocument();
    expect(screen.getByTestId(`pipeline-col-${defContacted.id}`)).toBeInTheDocument();
    expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
    expect(screen.getByText("123 Main St, Denver, CO")).toBeInTheDocument();
    expect(screen.getByText("456 Oak Ave, Boulder, CO")).toBeInTheDocument();
  });

  it("uses backend column labels when tenant renames a stage", async () => {
    mockedPipelineApi.listPipelineStatuses.mockResolvedValue([
      { ...defNew, label: "Intake (renamed)" },
      defContacted,
    ]);

    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Intake (renamed)" })).toBeInTheDocument();
    });
  });

  it("shows customer phone on cards when present", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.getByText("(555) 123-4567")).toBeInTheDocument();
    expect(screen.getByText("(303) 555-0100")).toBeInTheDocument();
  });

  it("does not render compact view toggle", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Pipeline");
    });

    expect(screen.queryByRole("checkbox", { name: /compact view/i })).not.toBeInTheDocument();
  });

  it("does not render a dedicated drag handle control", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Drag to reorder/i)).not.toBeInTheDocument();
  });

  it("whole card is the drag target when editable (grab cursor)", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByTestId("pipeline-card-lead-1")).toBeInTheDocument();
    });

    expect(screen.getByTestId("pipeline-card-lead-1")).toHaveClass("cursor-grab");
  });

  it("does not have a status dropdown on cards", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    expect(screen.queryByLabelText(/Status for Alice Smith/i)).not.toBeInTheDocument();
  });

  it("shows statusLabel from API on each card", async () => {
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

  it("renders search input for filtering pipeline cards", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    const search = screen.getByLabelText(/search leads in pipeline/i);
    expect(search).toBeInTheDocument();
    expect(search).toHaveAttribute("placeholder", "Search customer, address, phone…");
  });

  it("filters cards by search input (customer name)", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search leads in pipeline/i), {
      target: { value: "Bob" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
  });

  it("filters by address substring", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search leads in pipeline/i), {
      target: { value: "Oak Ave" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
  });

  it("filters by phone digits in search", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Bob Jones")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search leads in pipeline/i), {
      target: { value: "303555" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
  });

  it("filters by email when present", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Bob Jones")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search leads in pipeline/i), {
      target: { value: "bob@example" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });
    expect(screen.getByText("Bob Jones")).toBeInTheDocument();
  });

  it("keeps status columns visible when search narrows results", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "New" })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search leads in pipeline/i), {
      target: { value: "Bob" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });

    expect(screen.getByRole("heading", { name: "New" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contacted" })).toBeInTheDocument();
    expect(screen.getByTestId(`pipeline-col-${defNew.id}`)).toBeInTheDocument();
    expect(screen.getByTestId(`pipeline-col-${defContacted.id}`)).toBeInTheDocument();
  });

  it("shows empty column copy when no leads match search in a column", async () => {
    render(<LeadsPipelinePage />);

    await waitFor(() => {
      expect(screen.getByText("Alice Smith")).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/search leads in pipeline/i), {
      target: { value: "Bob" },
    });

    await waitFor(() => {
      expect(screen.queryByText("Alice Smith")).not.toBeInTheDocument();
    });

    const newCol = screen.getByTestId(`pipeline-col-${defNew.id}`);
    expect(within(newCol).getByText("No leads in this stage")).toBeInTheDocument();
    expect(within(newCol).queryByTestId("pipeline-card-lead-1")).not.toBeInTheDocument();
  });
});
