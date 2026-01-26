import type { AxiosInstance } from "axios";
import {
  listEstimatesForJob,
  getEstimate,
  createEstimateForJob,
  updateEstimate,
  updateEstimateStatus,
} from "./estimatesApi";
import type { EstimateDto, CreateEstimateRequest, UpdateEstimateRequest } from "./types";

const createMockApi = () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
});

const mockEstimate: EstimateDto = {
  id: "est-1",
  jobId: "job-1",
  customerId: "cust-1",
  status: "DRAFT",
  title: "Roof Replacement",
  notes: null,
  issueDate: null,
  validUntil: null,
  items: [
    { id: "item-1", name: "Shingles", description: null, quantity: 10, unitPrice: 50, unit: "sq ft" },
  ],
  subtotal: 500,
  total: 500,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("estimatesApi", () => {
  describe("listEstimatesForJob", () => {
    it("calls GET /api/v1/jobs/{jobId}/estimates", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: [mockEstimate] });

      const result = await listEstimatesForJob(mockApi as unknown as AxiosInstance, "job-1");

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs/job-1/estimates");
      expect(result).toEqual([mockEstimate]);
    });
  });

  describe("getEstimate", () => {
    it("calls GET /api/v1/estimates/{id}", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockEstimate });

      const result = await getEstimate(mockApi as unknown as AxiosInstance, "est-1");

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/estimates/est-1");
      expect(result).toEqual(mockEstimate);
    });
  });

  describe("createEstimateForJob", () => {
    it("calls POST /api/v1/jobs/{jobId}/estimates with payload", async () => {
      const mockApi = createMockApi();
      (mockApi.post as jest.Mock).mockResolvedValue({ data: mockEstimate });

      const payload: CreateEstimateRequest = {
        title: "New Estimate",
        notes: null,
        issueDate: null,
        validUntil: null,
        items: [{ name: "Labor", description: null, quantity: 1, unitPrice: 1000, unit: "job" }],
        status: null,
      };

      const result = await createEstimateForJob(
        mockApi as unknown as AxiosInstance,
        "job-1",
        payload
      );

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/jobs/job-1/estimates", payload);
      expect(result).toEqual(mockEstimate);
    });
  });

  describe("updateEstimate", () => {
    it("calls PUT /api/v1/estimates/{id} with payload", async () => {
      const mockApi = createMockApi();
      (mockApi.put as jest.Mock).mockResolvedValue({ data: { ...mockEstimate, title: "Updated" } });

      const payload: UpdateEstimateRequest = {
        title: "Updated",
        items: [{ name: "Shingles", description: null, quantity: 12, unitPrice: 50, unit: "sq ft" }],
      };

      const result = await updateEstimate(
        mockApi as unknown as AxiosInstance,
        "est-1",
        payload
      );

      expect(mockApi.put).toHaveBeenCalledWith("/api/v1/estimates/est-1", payload);
      expect(result.title).toBe("Updated");
    });
  });

  describe("updateEstimateStatus", () => {
    it("calls POST /api/v1/estimates/{id}/status with { status }", async () => {
      const mockApi = createMockApi();
      (mockApi.post as jest.Mock).mockResolvedValue({ data: { ...mockEstimate, status: "SENT" } });

      const result = await updateEstimateStatus(
        mockApi as unknown as AxiosInstance,
        "est-1",
        "SENT"
      );

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/estimates/est-1/status", { status: "SENT" });
      expect(result.status).toBe("SENT");
    });
  });
});
