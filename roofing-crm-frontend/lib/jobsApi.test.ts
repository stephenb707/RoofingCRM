import { AxiosInstance } from "axios";
import {
  listJobs,
  getJob,
  createJob,
  updateJobStatus,
} from "./jobsApi";
import {
  JobDto,
  JobStatus,
  JobType,
  CreateJobRequest,
  PageResponse,
} from "./types";

const createMockApi = () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
});

const mockJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: null,
  status: "SCHEDULED",
  type: "REPLACEMENT",
  propertyAddress: {
    line1: "123 Main St",
    city: "Denver",
    state: "CO",
    zip: "80202",
  },
  scheduledStartDate: "2024-06-01",
  scheduledEndDate: "2024-06-02",
  internalNotes: "Note",
  crewName: "Crew A",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockPage: PageResponse<JobDto> = {
  content: [mockJob],
  totalElements: 1,
  totalPages: 1,
  number: 0,
  size: 20,
  first: true,
  last: true,
};

describe("jobsApi", () => {
  describe("listJobs", () => {
    it("calls GET /api/v1/jobs with status and pagination", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPage });

      const result = await listJobs(mockApi as unknown as AxiosInstance, {
        status: "IN_PROGRESS",
        page: 1,
        size: 20,
      });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs", {
        params: { status: "IN_PROGRESS", page: 1, size: 20 },
      });
      expect(result).toEqual(mockPage);
    });

    it("calls GET /api/v1/jobs without status when not provided", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPage });

      await listJobs(mockApi as unknown as AxiosInstance, { page: 0, size: 20 });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs", {
        params: { page: 0, size: 20 },
      });
    });

    it("calls GET /api/v1/jobs with customerId", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPage });

      await listJobs(mockApi as unknown as AxiosInstance, {
        customerId: "cust-1",
        page: 0,
        size: 20,
      });

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs", {
        params: { customerId: "cust-1", page: 0, size: 20 },
      });
    });
  });

  describe("getJob", () => {
    it("calls GET /api/v1/jobs/{id}", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockJob });

      const result = await getJob(
        mockApi as unknown as AxiosInstance,
        "job-1"
      );

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs/job-1");
      expect(result).toEqual(mockJob);
    });
  });

  describe("createJob", () => {
    it("calls POST /api/v1/jobs with payload", async () => {
      const mockApi = createMockApi();
      (mockApi.post as jest.Mock).mockResolvedValue({ data: mockJob });

      const payload: CreateJobRequest = {
        customerId: "cust-1",
        type: "REPAIR" as JobType,
        propertyAddress: {
          line1: "456 Oak Ave",
          city: "Denver",
          state: "CO",
          zip: "80203",
        },
      };

      const result = await createJob(
        mockApi as unknown as AxiosInstance,
        payload
      );

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/jobs", payload);
      expect(result).toEqual(mockJob);
    });

    it("calls POST /api/v1/jobs with leadId", async () => {
      const mockApi = createMockApi();
      (mockApi.post as jest.Mock).mockResolvedValue({ data: mockJob });

      const payload: CreateJobRequest = {
        leadId: "lead-1",
        type: "REPLACEMENT" as JobType,
        propertyAddress: { line1: "123 Main", city: "Denver", state: "CO" },
      };

      await createJob(mockApi as unknown as AxiosInstance, payload);

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/jobs", payload);
    });
  });

  describe("updateJobStatus", () => {
    it("calls POST /api/v1/jobs/{id}/status with { status }", async () => {
      const mockApi = createMockApi();
      const updated = { ...mockJob, status: "IN_PROGRESS" as JobStatus };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: updated });

      const result = await updateJobStatus(
        mockApi as unknown as AxiosInstance,
        "job-1",
        "IN_PROGRESS"
      );

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/jobs/job-1/status", {
        status: "IN_PROGRESS",
      });
      expect(result.status).toBe("IN_PROGRESS");
    });
  });
});
