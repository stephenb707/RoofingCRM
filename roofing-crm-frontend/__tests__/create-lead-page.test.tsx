import React from "react";
import { render, screen, fireEvent, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import NewLeadPage from "@/app/app/leads/new/page";
import * as leadsApi from "@/lib/leadsApi";
import * as customersApi from "@/lib/customersApi";
import { LeadDto, CustomerDto } from "@/lib/types";
import { PageResponse } from "@/lib/types";

jest.mock("@/lib/leadsApi");
jest.mock("@/lib/customersApi");
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;

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

const mockCustomer: CustomerDto = {
  id: "cust-456",
  firstName: "Jane",
  lastName: "Doe",
  primaryPhone: "555-999-8888",
  email: "jane@example.com",
  notes: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

// Customer with raw phone digits for formatting assertion
const mockCustomerWithRawPhone: CustomerDto = {
  id: "cust-raw",
  firstName: "John",
  lastName: "Smith",
  primaryPhone: "3121112222",
  email: null,
  notes: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockCustomerWithBillingAddress: CustomerDto = {
  ...mockCustomer,
  billingAddress: {
    line1: "789 Billing Rd",
    line2: null,
    city: "Denver",
    state: "CO",
    zip: "80202",
    countryCode: "US",
  },
};

const customersPage: PageResponse<CustomerDto> = {
  content: [mockCustomer],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 100,
  first: true,
  last: true,
};

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
    mockedCustomersApi.listCustomers.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100, first: true, last: true });
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

  it("calls createLead with newCustomer and redirects when no existing customer selected", async () => {
    mockedLeadsApi.createLead.mockResolvedValue(mockCreatedLead);
    mockedCustomersApi.listCustomers.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100, first: true, last: true });

    render(<NewLeadPage />);

    const user = userEvent.setup();

    // Fill in required fields (manual customer entry)
    await user.type(screen.getByPlaceholderText("John"), "John");
    await user.type(screen.getByPlaceholderText("Doe"), "Doe");
    await user.type(
      screen.getByPlaceholderText("(555) 123-4567"),
      "555-123-4567"
    );
    await user.type(screen.getByPlaceholderText("123 Main Street"), "123 Main St");

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

  it("calls createLead with customerId and newCustomer null when existing customer selected", async () => {
    mockedLeadsApi.createLead.mockResolvedValue({ ...mockCreatedLead, customerId: "cust-456" });
    mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
    mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomer);

    render(<NewLeadPage />);

    const user = userEvent.setup();
    await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

    await waitFor(() => {
      expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("option", { name: /Jane Doe/ }));
    await user.type(screen.getByPlaceholderText("123 Main Street"), "123 Main St");

    await user.click(screen.getByRole("button", { name: "Create Lead" }));

    await waitFor(() => {
      expect(mockedLeadsApi.createLead).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          customerId: "cust-456",
          newCustomer: null,
          propertyAddress: expect.objectContaining({ line1: "123 Main St" }),
        })
      );
    });
    expect(mockPush).toHaveBeenCalledWith("/app/leads/new-lead-123");
  });

  it("when selecting existing customer, property address does NOT auto-fill from billing", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
    mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomerWithBillingAddress);

    render(<NewLeadPage />);

    const user = userEvent.setup();
    await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

    await waitFor(() => {
      expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("option", { name: /Jane Doe/ }));

    await waitFor(() => {
      expect(mockedCustomersApi.getCustomer).toHaveBeenCalledWith(expect.anything(), "cust-456");
    });

    expect(screen.getByPlaceholderText("123 Main Street")).toHaveValue("");
    expect(screen.getByPlaceholderText("Denver")).toHaveValue("");
    expect(screen.getByPlaceholderText("CO")).toHaveValue("");
    expect(screen.getByPlaceholderText("80202")).toHaveValue("");
  });

  it("when selecting existing customer, clicking Use billing address fills property address", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
    mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomerWithBillingAddress);

    render(<NewLeadPage />);

    const user = userEvent.setup();
    await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

    await waitFor(() => {
      expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("option", { name: /Jane Doe/ }));
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Use billing address" })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Use billing address" }));

    expect(screen.getByDisplayValue("789 Billing Rd")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Denver")).toBeInTheDocument();
    expect(screen.getByDisplayValue("CO")).toBeInTheDocument();
    expect(screen.getByDisplayValue("80202")).toBeInTheDocument();
  });

  it("when customer is selected, Change is visible and clicking it shows combobox again", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
    mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomer);

    render(<NewLeadPage />);

    const user = userEvent.setup();
    await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

    await waitFor(() => {
      expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("option", { name: /Jane Doe/ }));

    expect(screen.getByRole("button", { name: "Change" })).toBeInTheDocument();
    expect(screen.queryByText("Customer Information")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Change" }));

    expect(screen.getByText("Customer Information")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("John")).toBeInTheDocument();
  });

  it("when selecting existing customer, clicking Clear address clears property address", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
    mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomerWithBillingAddress);

    render(<NewLeadPage />);

    const user = userEvent.setup();
    await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

    await waitFor(() => {
      expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("option", { name: /Jane Doe/ }));
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Use billing address" })).toBeInTheDocument();
    });
    await user.click(screen.getByRole("button", { name: "Use billing address" }));
    expect(screen.getByDisplayValue("789 Billing Rd")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Clear address" }));

    expect(screen.getByPlaceholderText("123 Main Street")).toHaveValue("");
    expect(screen.getByPlaceholderText("Denver")).toHaveValue("");
    expect(screen.getByPlaceholderText("CO")).toHaveValue("");
    expect(screen.getByPlaceholderText("80202")).toHaveValue("");
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

  it("customer dropdown shows formatted phone in option label", async () => {
    const pageWithRawPhone = {
      content: [mockCustomerWithRawPhone],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
    };
    mockedCustomersApi.listCustomers.mockResolvedValue(pageWithRawPhone);

    render(<NewLeadPage />);

    await waitFor(() => {
      expect(mockedCustomersApi.listCustomers).toHaveBeenCalled();
    });

    const user = userEvent.setup();
    await user.click(await screen.findByPlaceholderText("Search by name, email, or phone…"));

    const option = await screen.findByRole("option", { name: /John Smith/ });
    expect(option).toHaveTextContent(/\(312\)\s*111-2222/);
  });
});
