import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import LeadsPage from "@/app/app/leads/page";
import * as leadsApi from "@/lib/leadsApi";
import { LeadDto, PageResponse } from "@/lib/types";

// Mock the leadsApi module
jest.mock("@/lib/leadsApi");
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockLead: LeadDto = {
  id: "lead-123",
  customerId: "customer-456",
  status: "NEW",
  source: "WEBSITE",
  leadNotes: "Test notes",
  propertyAddress: {
    line1: "123 Main St",
    city: "Denver",
    state: "CO",
    zip: "80202",
  },
  preferredContactMethod: "Phone",
  createdAt: "2024-01-15T10:00:00Z",
  updatedAt: "2024-01-15T10:00:00Z",
  customerFirstName: "John",
  customerLastName: "Doe",
  customerPhone: "555-123-4567",
  customerEmail: "john@example.com",
};

const mockEmptyPage: PageResponse<LeadDto> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

const mockPageWithLeads: PageResponse<LeadDto> = {
  content: [mockLead],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("LeadsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders loading state initially", async () => {
    mockedLeadsApi.listLeads.mockImplementation(
      () => new Promise(() => {}) // Never resolves, keeps loading
    );

    render(<LeadsPage />);

    expect(screen.getByText("Loading leads...")).toBeInTheDocument();
  });

  it("renders empty state when no leads exist", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue(mockEmptyPage);

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("No leads yet")).toBeInTheDocument();
    });

    expect(
      screen.getByText("Get started by adding your first lead.")
    ).toBeInTheDocument();
  });

  it("renders leads in a table when leads exist", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue(mockPageWithLeads);

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("John Doe")).toBeInTheDocument();
    });

    expect(screen.getByText("123 Main St, Denver, CO, 80202")).toBeInTheDocument();
    // Check status badge specifically - there are multiple elements with "New" text
    const statusBadge = screen.getByText("New", {
      selector: "span.inline-flex",
    });
    expect(statusBadge).toBeInTheDocument();
    expect(screen.getByText("Website")).toBeInTheDocument();
  });

  it("renders error state when API fails", async () => {
    mockedLeadsApi.listLeads.mockRejectedValue(new Error("Network error"));

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("Failed to load leads")).toBeInTheDocument();
    });

    expect(screen.getByText("Network error")).toBeInTheDocument();
  });

  it("renders page title and new lead button", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue(mockEmptyPage);

    render(<LeadsPage />);

    expect(screen.getByText("Leads")).toBeInTheDocument();
    expect(screen.getByText("Manage your sales pipeline")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /\+ New Lead/i })).toHaveAttribute(
      "href",
      "/app/leads/new"
    );
  });

  it("filters leads by status when selecting from dropdown", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue(mockPageWithLeads);

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("John Doe")).toBeInTheDocument();
    });

    // Clear the initial call count
    mockedLeadsApi.listLeads.mockClear();

    // Change the filter
    const statusSelect = screen.getByLabelText("Status:");
    fireEvent.change(statusSelect, { target: { value: "CONTACTED" } });

    await waitFor(() => {
      expect(mockedLeadsApi.listLeads).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ status: "CONTACTED" })
      );
    });
  });

  it("shows clear filters button when filter is applied", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue(mockPageWithLeads);

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("John Doe")).toBeInTheDocument();
    });

    // Apply a filter
    const statusSelect = screen.getByLabelText("Status:");
    fireEvent.change(statusSelect, { target: { value: "NEW" } });

    await waitFor(() => {
      expect(screen.getByText("Clear filters")).toBeInTheDocument();
    });
  });

  it("has working view link for each lead", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue(mockPageWithLeads);

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("John Doe")).toBeInTheDocument();
    });

    const viewLink = screen.getByRole("link", { name: "View" });
    expect(viewLink).toHaveAttribute("href", "/app/leads/lead-123");
  });

  it("does not render undefined when customer name fields are missing", async () => {
    const leadNoCustomer: LeadDto = {
      ...mockLead,
      id: "lead-no-cust",
      customerFirstName: undefined,
      customerLastName: undefined,
      customerEmail: undefined,
      customerPhone: undefined,
    };
    mockedLeadsApi.listLeads.mockResolvedValue({
      ...mockPageWithLeads,
      content: [leadNoCustomer],
    });

    render(<LeadsPage />);

    await waitFor(() => {
      expect(screen.getByText("123 Main St, Denver, CO, 80202")).toBeInTheDocument();
    });
    expect(screen.queryByText(/undefined/)).not.toBeInTheDocument();
  });
});
