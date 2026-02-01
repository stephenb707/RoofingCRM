import React from "react";
import { render, screen, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import NewJobPage from "@/app/app/jobs/new/page";
import * as jobsApi from "@/lib/jobsApi";
import * as customersApi from "@/lib/customersApi";
import * as leadsApi from "@/lib/leadsApi";
import { JobDto } from "@/lib/types";
import { PageResponse } from "@/lib/types";
import { CustomerDto } from "@/lib/types";
import { LeadDto } from "@/lib/types";

jest.mock("@/lib/jobsApi");
jest.mock("@/lib/customersApi");
jest.mock("@/lib/leadsApi");
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockPush = jest.fn();
let searchParamsStr = "";
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/jobs/new",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(searchParamsStr),
}));

const mockCustomer: CustomerDto = {
  id: "cust-1",
  firstName: "Jane",
  lastName: "Doe",
  primaryPhone: "555-111-2222",
  email: "jane@example.com",
  notes: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockCustomerRawPhone: CustomerDto = {
  id: "cust-raw",
  firstName: "John",
  lastName: "Smith",
  primaryPhone: "3121112222",
  email: null,
  notes: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockCustomerWithAddress: CustomerDto = {
  ...mockCustomer,
  billingAddress: {
    line1: "456 Billing Ave",
    line2: "Suite 2",
    city: "Chicago",
    state: "IL",
    zip: "60601",
    countryCode: "US",
  },
};

const mockLead: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  status: "NEW",
  source: "WEBSITE",
  leadNotes: null,
  propertyAddress: {
    line1: "999 Lead St",
    city: "Denver",
    state: "CO",
    zip: "80202",
  },
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockCreatedJob: JobDto = {
  id: "job-new-1",
  customerId: "cust-1",
  leadId: null,
  status: "UNSCHEDULED",
  type: "REPAIR",
  propertyAddress: { line1: "123 Test Ave", city: "Denver", state: "CO" },
  scheduledStartDate: null,
  scheduledEndDate: null,
  internalNotes: null,
  crewName: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
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

describe("NewJobPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    searchParamsStr = "";
  });

  describe("Customer mode (no leadId param)", () => {
    it("loads customers and creates job with customerId and propertyAddress and type", async () => {
      mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
      mockedJobsApi.createJob.mockResolvedValue(mockCreatedJob);

      render(<NewJobPage />);

      await waitFor(() => {
        expect(mockedCustomersApi.listCustomers).toHaveBeenCalled();
      });

      const user = userEvent.setup();
      await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

      await waitFor(() => {
        expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
      });

      await user.click(screen.getByRole("option", { name: /Jane Doe/ }));
      await user.selectOptions(screen.getByLabelText(/Job type/i), "REPAIR");
      await user.type(screen.getByPlaceholderText("123 Main Street"), "123 Test Ave");
      await user.type(screen.getByPlaceholderText("Denver"), "Denver");
      await user.type(screen.getByPlaceholderText("CO"), "CO");

      await user.click(screen.getByRole("button", { name: "Create Job" }));

      await waitFor(() => {
        expect(mockedJobsApi.createJob).toHaveBeenCalledWith(
          expect.anything(),
          expect.objectContaining({
            customerId: "cust-1",
            type: "REPAIR",
            propertyAddress: expect.objectContaining({ line1: "123 Test Ave", city: "Denver", state: "CO" }),
          })
        );
      });

      await waitFor(() => {
        expect(mockPush).toHaveBeenCalledWith("/app/jobs/job-new-1");
      });
    });
  });

  describe("Customer mode billing address assist (opt-in)", () => {
    it("does NOT auto-fill address when selecting a customer", async () => {
      mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
      mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomerWithAddress);
      mockedJobsApi.createJob.mockResolvedValue(mockCreatedJob);

      render(<NewJobPage />);

      const user = userEvent.setup();
      await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

      await waitFor(() => {
        expect(screen.getByRole("option", { name: /Jane Doe/ })).toBeInTheDocument();
      });

      await user.click(screen.getByRole("option", { name: /Jane Doe/ }));

      await waitFor(() => {
        expect(mockedCustomersApi.getCustomer).toHaveBeenCalledWith(expect.anything(), "cust-1");
      });

      expect(screen.getByPlaceholderText("123 Main Street")).toHaveValue("");
      expect(screen.getByPlaceholderText("Apt 4B")).toHaveValue("");
      expect(screen.getByPlaceholderText("Denver")).toHaveValue("");
      expect(screen.getByPlaceholderText("CO")).toHaveValue("");
      expect(screen.getByPlaceholderText("80202")).toHaveValue("");
    });

    it("fills address when clicking Use billing address", async () => {
      mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
      mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomerWithAddress);
      mockedJobsApi.createJob.mockResolvedValue(mockCreatedJob);

      render(<NewJobPage />);

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

      expect(screen.getByDisplayValue("456 Billing Ave")).toBeInTheDocument();
      expect(screen.getByDisplayValue("Suite 2")).toBeInTheDocument();
      expect(screen.getByDisplayValue("Chicago")).toBeInTheDocument();
      expect(screen.getByDisplayValue("IL")).toBeInTheDocument();
      expect(screen.getByDisplayValue("60601")).toBeInTheDocument();
    });

    it("clears address when clicking Clear address", async () => {
      mockedCustomersApi.listCustomers.mockResolvedValue(customersPage);
      mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomerWithAddress);
      mockedJobsApi.createJob.mockResolvedValue(mockCreatedJob);

      render(<NewJobPage />);

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
      expect(screen.getByDisplayValue("456 Billing Ave")).toBeInTheDocument();

      await user.click(screen.getByRole("button", { name: "Clear address" }));

      expect(screen.getByPlaceholderText("123 Main Street")).toHaveValue("");
      expect(screen.getByPlaceholderText("Apt 4B")).toHaveValue("");
      expect(screen.getByPlaceholderText("Denver")).toHaveValue("");
      expect(screen.getByPlaceholderText("CO")).toHaveValue("");
      expect(screen.getByPlaceholderText("80202")).toHaveValue("");
    });
  });

  it("customer dropdown shows formatted phone in option label", async () => {
    const pageWithRawPhone: PageResponse<CustomerDto> = {
      content: [mockCustomerRawPhone],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
    };
    mockedCustomersApi.listCustomers.mockResolvedValue(pageWithRawPhone);

    render(<NewJobPage />);

    const user = userEvent.setup();
    await user.click(screen.getByPlaceholderText("Search by name, email, or phone…"));

    await waitFor(() => {
      expect(screen.getByRole("option", { name: /John Smith/ })).toBeInTheDocument();
    });
    const option = screen.getByRole("option", { name: /John Smith/ });
    expect(option).toHaveTextContent(/\(312\)\s*111-2222/);
  });

  describe("Lead mode (leadId param)", () => {
    it("loads lead, prefills address from lead, and creates job with leadId; does NOT show BillingAddressAssist", async () => {
      searchParamsStr = "leadId=lead-1";
      mockedLeadsApi.getLead.mockResolvedValue(mockLead);
      mockedJobsApi.createJob.mockResolvedValue({ ...mockCreatedJob, leadId: "lead-1" });

      render(<NewJobPage />);

      await waitFor(() => {
        expect(mockedLeadsApi.getLead).toHaveBeenCalledWith(expect.anything(), "lead-1");
      });

      await waitFor(() => {
        expect(screen.getByDisplayValue("999 Lead St")).toBeInTheDocument();
      });

      expect(screen.queryByTestId("billing-address-assist")).not.toBeInTheDocument();

      const user = userEvent.setup();
      await user.selectOptions(screen.getByLabelText(/Job type/i), "REPLACEMENT");
      await user.click(screen.getByRole("button", { name: "Create Job" }));

      await waitFor(() => {
        expect(mockedJobsApi.createJob).toHaveBeenCalledWith(
          expect.anything(),
          expect.objectContaining({
            leadId: "lead-1",
            type: "REPLACEMENT",
            propertyAddress: expect.objectContaining({ line1: "999 Lead St", city: "Denver", state: "CO" }),
          })
        );
      });
    });
  });
});
