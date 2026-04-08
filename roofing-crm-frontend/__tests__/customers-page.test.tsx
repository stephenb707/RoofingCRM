import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import userEvent from "@testing-library/user-event";
import CustomersPage from "@/app/app/customers/page";
import * as customersApi from "@/lib/customersApi";
import { CustomerDto, PageResponse } from "@/lib/types";

jest.mock("@/lib/customersApi");
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;

const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  usePathname: () => "/app/customers",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
}));

const mockEmptyPage: PageResponse<CustomerDto> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

const mockCustomer: CustomerDto = {
  id: "cust-1",
  firstName: "Jane",
  lastName: "Doe",
  email: "jane@example.com",
  primaryPhone: "555-0100",
};

const mockCustomerPage: PageResponse<CustomerDto> = {
  content: [mockCustomer],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("CustomersPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("shows No customers yet when list is empty and no search", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(mockEmptyPage);

    render(<CustomersPage />);

    await waitFor(() => {
      expect(screen.getByText("No customers yet")).toBeInTheDocument();
    });

    expect(screen.getByText("Get started by adding your first customer.")).toBeInTheDocument();
    expect(mockedCustomersApi.listCustomers).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ page: 0, size: 20 })
    );
  });

  it("shows No matching customers when search returns empty", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(mockEmptyPage);

    render(<CustomersPage />);

    await waitFor(() => {
      expect(screen.getByText("No customers yet")).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText("Search customers by name, email, or phone...");
    await userEvent.type(searchInput, "nonexistent");

    await waitFor(
      () => {
        expect(screen.getByText("No matching customers")).toBeInTheDocument();
        expect(screen.getByText(/No customers match "nonexistent"./)).toBeInTheDocument();
      },
      { timeout: 5000 }
    );

    expect(mockedCustomersApi.listCustomers).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ q: "nonexistent" })
    );
  });

  it("navigates to customer detail when clicking the row", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(mockCustomerPage);

    render(<CustomersPage />);

    await waitFor(() => {
      expect(screen.getByText("Jane Doe")).toBeInTheDocument();
    });

    mockPush.mockClear();

    const row = screen.getByLabelText("Open customer Jane Doe");
    fireEvent.click(row);

    expect(mockPush).toHaveBeenCalledWith("/app/customers/cust-1");
  });

  it("View link does not double-navigate when router.push is used for the row", async () => {
    mockedCustomersApi.listCustomers.mockResolvedValue(mockCustomerPage);

    render(<CustomersPage />);

    await waitFor(() => {
      expect(screen.getByRole("link", { name: "View" })).toBeInTheDocument();
    });

    mockPush.mockClear();
    fireEvent.click(screen.getByRole("link", { name: "View" }));

    expect(mockPush).not.toHaveBeenCalled();
  });
});
