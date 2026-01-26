import { AxiosInstance } from "axios";
import { listLeads, getLead, createLead, updateLead, updateLeadStatus } from "./leadsApi";
import { LeadDto, CreateLeadRequest, UpdateLeadRequest, PageResponse } from "./types";

// Mock axios instance
const createMockApi = () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
});

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

const mockPageResponse: PageResponse<LeadDto> = {
  content: [mockLead],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("leadsApi", () => {
  describe("listLeads", () => {
    it("should call GET /api/v1/leads without params", async () => {
      const mockApi = createMockApi();
      mockApi.get.mockResolvedValue({ data: mockPageResponse });

      const result = await listLeads(mockApi as unknown as AxiosInstance, {});

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads", { params: {} });
      expect(result).toEqual(mockPageResponse);
    });

    it("should call GET /api/v1/leads with status filter", async () => {
      const mockApi = createMockApi();
      mockApi.get.mockResolvedValue({ data: mockPageResponse });

      const result = await listLeads(mockApi as unknown as AxiosInstance, {
        status: "NEW",
      });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads", {
        params: { status: "NEW" },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it("should call GET /api/v1/leads with pagination params", async () => {
      const mockApi = createMockApi();
      mockApi.get.mockResolvedValue({ data: mockPageResponse });

      const result = await listLeads(mockApi as unknown as AxiosInstance, {
        page: 2,
        size: 10,
      });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads", {
        params: { page: 2, size: 10 },
      });
      expect(result).toEqual(mockPageResponse);
    });

    it("should call GET /api/v1/leads with all params combined", async () => {
      const mockApi = createMockApi();
      mockApi.get.mockResolvedValue({ data: mockPageResponse });

      const result = await listLeads(mockApi as unknown as AxiosInstance, {
        status: "CONTACTED",
        page: 1,
        size: 15,
      });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads", {
        params: { status: "CONTACTED", page: 1, size: 15 },
      });
      expect(result).toEqual(mockPageResponse);
    });
  });

  describe("getLead", () => {
    it("should call GET /api/v1/leads/{id}", async () => {
      const mockApi = createMockApi();
      mockApi.get.mockResolvedValue({ data: mockLead });

      const result = await getLead(
        mockApi as unknown as AxiosInstance,
        "lead-123"
      );

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads/lead-123");
      expect(result).toEqual(mockLead);
    });
  });

  describe("createLead", () => {
    it("should call POST /api/v1/leads with payload", async () => {
      const mockApi = createMockApi();
      mockApi.post.mockResolvedValue({ data: mockLead });

      const payload: CreateLeadRequest = {
        newCustomer: {
          firstName: "John",
          lastName: "Doe",
          primaryPhone: "555-123-4567",
          email: "john@example.com",
        },
        source: "WEBSITE",
        leadNotes: "Test notes",
        propertyAddress: {
          line1: "123 Main St",
          city: "Denver",
          state: "CO",
          zip: "80202",
        },
        preferredContactMethod: "Phone",
      };

      const result = await createLead(
        mockApi as unknown as AxiosInstance,
        payload
      );

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/leads", payload);
      expect(result).toEqual(mockLead);
    });
  });

  describe("updateLead", () => {
    it("should call PUT /api/v1/leads/{id} with payload", async () => {
      const mockApi = createMockApi();
      mockApi.put.mockResolvedValue({ data: mockLead });

      const payload: UpdateLeadRequest = {
        source: "REFERRAL",
        leadNotes: "Updated notes",
        preferredContactMethod: "Email",
        propertyAddress: { line1: "456 Oak St", city: "Chicago", state: "IL", zip: "60601" },
      };

      const result = await updateLead(
        mockApi as unknown as AxiosInstance,
        "lead-123",
        payload
      );

      expect(mockApi.put).toHaveBeenCalledWith("/api/v1/leads/lead-123", payload);
      expect(result).toEqual(mockLead);
    });
  });

  describe("updateLeadStatus", () => {
    it("should call POST /api/v1/leads/{id}/status with status payload", async () => {
      const mockApi = createMockApi();
      const updatedLead = { ...mockLead, status: "CONTACTED" as const };
      mockApi.post.mockResolvedValue({ data: updatedLead });

      const result = await updateLeadStatus(
        mockApi as unknown as AxiosInstance,
        "lead-123",
        "CONTACTED"
      );

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/leads/lead-123/status", {
        status: "CONTACTED",
      });
      expect(result).toEqual(updatedLead);
    });
  });
});
