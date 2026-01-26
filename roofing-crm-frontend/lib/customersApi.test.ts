import { AxiosInstance } from "axios";
import { listCustomers } from "./customersApi";
import { CustomerDto, PageResponse } from "./types";

const createMockApi = () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
});

const mockCustomer: CustomerDto = {
  id: "cust-1",
  firstName: "Jane",
  lastName: "Doe",
  primaryPhone: "555-999-0000",
  email: "jane@example.com",
  notes: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockPage: PageResponse<CustomerDto> = {
  content: [mockCustomer],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 50,
  first: true,
  last: true,
};

describe("customersApi", () => {
  describe("listCustomers", () => {
    it("calls GET /api/v1/customers with page and size", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPage });

      const result = await listCustomers(mockApi as unknown as AxiosInstance, {
        page: 0,
        size: 50,
      });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/customers", {
        params: { page: 0, size: 50 },
      });
      expect(result).toEqual(mockPage);
    });

    it("calls GET /api/v1/customers with empty params when no args", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPage });

      await listCustomers(mockApi as unknown as AxiosInstance, {});

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/customers", {
        params: {},
      });
    });

    it("calls GET /api/v1/customers with only page", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPage });

      await listCustomers(mockApi as unknown as AxiosInstance, { page: 2 });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/customers", {
        params: { page: 2 },
      });
    });
  });
});
