import React from "react";
import { render, screen, fireEvent, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import NewLeadPage from "@/app/app/leads/new/page";
import * as leadsApi from "@/lib/leadsApi";
import { LeadDto } from "@/lib/types";

// Mock the leadsApi module
jest.mock("@/lib/leadsApi");
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

// Mock useRouter
const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({
    push: mockPush,
    replace: jest.fn(),
    back: jest.fn(),
  }),
  usePathname: () => "/app/leads/new",
  useParams: () => ({}),
}));

const mockCreatedLead: LeadDto = {
  id: "new-lead-123",
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

describe("NewLeadPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders the form with all sections", () => {
    render(<NewLeadPage />);

    expect(screen.getByText("New Lead")).toBeInTheDocument();
    expect(screen.getByText("Customer Information")).toBeInTheDocument();
    expect(screen.getByText("Property Address")).toBeInTheDocument();
    expect(screen.getByText("Lead Details")).toBeInTheDocument();
  });

  it("renders all required form fields", () => {
    render(<NewLeadPage />);

    // Customer fields
    expect(screen.getByPlaceholderText("John")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Doe")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("(555) 123-4567")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("john@example.com")).toBeInTheDocument();

    // Address fields
    expect(screen.getByPlaceholderText("123 Main Street")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Denver")).toBeInTheDocument();

    // Lead fields
    expect(screen.getByText("Lead Source")).toBeInTheDocument();
  });

  it("renders cancel and submit buttons", () => {
    render(<NewLeadPage />);

    expect(screen.getByRole("link", { name: "Cancel" })).toHaveAttribute(
      "href",
      "/app/leads"
    );
    expect(
      screen.getByRole("button", { name: "Create Lead" })
    ).toBeInTheDocument();
  });

  it("shows back to leads link", () => {
    render(<NewLeadPage />);

    expect(screen.getByRole("link", { name: /Back to Leads/i })).toHaveAttribute(
      "href",
      "/app/leads"
    );
  });

  it("shows validation error when required fields are empty", async () => {
    render(<NewLeadPage />);

    const submitButton = screen.getByRole("button", { name: "Create Lead" });
    fireEvent.click(submitButton);

    // HTML5 validation should prevent submission
    // The form should not be submitted without required fields
    expect(mockedLeadsApi.createLead).not.toHaveBeenCalled();
  });

  it("calls createLead and redirects on successful submission", async () => {
    mockedLeadsApi.createLead.mockResolvedValue(mockCreatedLead);

    render(<NewLeadPage />);

    const user = userEvent.setup();

    // Fill in required fields
    await user.type(screen.getByPlaceholderText("John"), "John");
    await user.type(screen.getByPlaceholderText("Doe"), "Doe");
    await user.type(
      screen.getByPlaceholderText("(555) 123-4567"),
      "555-123-4567"
    );
    await user.type(screen.getByPlaceholderText("123 Main Street"), "123 Main St");

    // Submit the form
    const submitButton = screen.getByRole("button", { name: "Create Lead" });
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockedLeadsApi.createLead).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          newCustomer: expect.objectContaining({
            firstName: "John",
            lastName: "Doe",
            primaryPhone: "555-123-4567",
          }),
          propertyAddress: expect.objectContaining({
            line1: "123 Main St",
          }),
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/app/leads/new-lead-123");
    });
  });

  it("shows error message when createLead fails", async () => {
    mockedLeadsApi.createLead.mockRejectedValue(new Error("Server error"));

    render(<NewLeadPage />);

    const user = userEvent.setup();

    // Fill in required fields
    await user.type(screen.getByPlaceholderText("John"), "John");
    await user.type(screen.getByPlaceholderText("Doe"), "Doe");
    await user.type(
      screen.getByPlaceholderText("(555) 123-4567"),
      "555-123-4567"
    );
    await user.type(screen.getByPlaceholderText("123 Main Street"), "123 Main St");

    // Submit the form
    const submitButton = screen.getByRole("button", { name: "Create Lead" });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText("Server error")).toBeInTheDocument();
    });
  });

  it("renders lead source options", () => {
    render(<NewLeadPage />);

    // Find the select by looking for the one with "Select a source" option
    const sourceSelect = screen.getByDisplayValue("Select a source") as HTMLSelectElement;

    // Verify all source options are present
    const options = Array.from(sourceSelect.options).map((o) => o.value);
    expect(options).toContain("");
    expect(options).toContain("REFERRAL");
    expect(options).toContain("WEBSITE");
    expect(options).toContain("DOOR_TO_DOOR");
    expect(options).toContain("INSURANCE_PARTNER");
    expect(options).toContain("OTHER");
  });
});
