import React from "react";
import { render, screen, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import CustomersPage from "@/app/app/customers/page";
import * as customersApi from "@/lib/customersApi";
import { CustomerDto, PageResponse } from "@/lib/types";

jest.mock("@/lib/customersApi");
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;

jest.mock("next/navigation", () => ({
  usePathname: () => "/app/customers",
  useParams: () => ({}),
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
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
});
