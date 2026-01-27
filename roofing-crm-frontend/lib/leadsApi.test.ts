import axios from "axios";
import { convertLeadToJob } from "./leadsApi";
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
});
