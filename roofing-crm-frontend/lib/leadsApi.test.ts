import axios from "axios";
import { convertLeadToJob, updateLeadStatus } from "./leadsApi";
import type { ConvertLeadToJobRequest, JobDto } from "./types";

jest.mock("axios");
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe("leadsApi", () => {
  const mockApi = {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
  } as any;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("convertLeadToJob", () => {
    it("calls POST with correct URL and payload", async () => {
      const mockJob: JobDto = {
        id: "job-1",
        customerId: "cust-1",
        leadId: "lead-1",
        status: "SCHEDULED",
        type: "REPLACEMENT",
        propertyAddress: null,
        scheduledStartDate: null,
        scheduledEndDate: null,
        internalNotes: null,
        crewName: null,
        createdAt: "2024-01-01T00:00:00Z",
        updatedAt: "2024-01-01T00:00:00Z",
      };

      mockApi.post.mockResolvedValue({ data: mockJob });

      const payload: ConvertLeadToJobRequest = {
        type: "REPLACEMENT",
        scheduledStartDate: "2024-02-01",
        crewName: "Team Alpha",
        internalNotes: "Test notes",
      };

      const result = await convertLeadToJob(mockApi, "lead-1", payload);

      expect(mockApi.post).toHaveBeenCalledWith("/api/v1/leads/lead-1/convert", payload);
      expect(result).toEqual(mockJob);
    });
  });

  describe("updateLeadStatus", () => {
    it("sends status and position when position provided", async () => {
      mockApi.post.mockResolvedValue({
        data: { id: "lead-1", status: "CONTACTED", pipelinePosition: 2 },
      });

      await updateLeadStatus(mockApi, "lead-1", "CONTACTED", 2);

      expect(mockApi.post).toHaveBeenCalledWith(
        "/api/v1/leads/lead-1/status",
        expect.objectContaining({ status: "CONTACTED", position: 2 })
      );
    });

    it("sends only status when position omitted", async () => {
      mockApi.post.mockResolvedValue({
        data: { id: "lead-1", status: "CONTACTED" },
      });

      await updateLeadStatus(mockApi, "lead-1", "CONTACTED");

      expect(mockApi.post).toHaveBeenCalledWith(
        "/api/v1/leads/lead-1/status",
        { status: "CONTACTED" }
      );
    });
  });
});
