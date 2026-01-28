import type { AxiosInstance } from "axios";
import {
  listLeadCommunicationLogs,
  addLeadCommunicationLog,
  listJobCommunicationLogs,
  addJobCommunicationLog,
} from "./communicationLogsApi";
import type { CommunicationLogDto, CreateCommunicationLogRequest } from "./types";

describe("communicationLogsApi", () => {
  const mockApi = {
    get: jest.fn(),
    post: jest.fn(),
  } as unknown as AxiosInstance;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("listLeadCommunicationLogs", () => {
    it("calls GET with correct URL", async () => {
      const data: CommunicationLogDto[] = [];
      (mockApi.get as jest.Mock).mockResolvedValue({ data });

      await listLeadCommunicationLogs(mockApi, "lead-1");

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads/lead-1/communications");
      expect(mockApi.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("addLeadCommunicationLog", () => {
    it("calls POST with correct URL and payload", async () => {
      const payload: CreateCommunicationLogRequest = {
        channel: "NOTE",
        subject: "Called customer",
        body: "Left voicemail",
        occurredAt: "2024-01-15T14:00:00Z",
      };
      const created: CommunicationLogDto = {
        id: "log-1",
        channel: "NOTE",
        direction: null,
        subject: "Called customer",
        body: "Left voicemail",
        occurredAt: "2024-01-15T14:00:00Z",
        leadId: "lead-1",
        jobId: null,
        createdAt: "2024-01-15T14:00:00Z",
        updatedAt: "2024-01-15T14:00:00Z",
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await addLeadCommunicationLog(mockApi, "lead-1", payload);

      expect(mockApi.post).toHaveBeenCalledWith(
        "/api/v1/leads/lead-1/communications",
        payload
      );
      expect(mockApi.post).toHaveBeenCalledTimes(1);
    });
  });

  describe("listJobCommunicationLogs", () => {
    it("calls GET with correct URL", async () => {
      const data: CommunicationLogDto[] = [];
      (mockApi.get as jest.Mock).mockResolvedValue({ data });

      await listJobCommunicationLogs(mockApi, "job-1");

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs/job-1/communications");
      expect(mockApi.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("addJobCommunicationLog", () => {
    it("calls POST with correct URL and payload", async () => {
      const payload: CreateCommunicationLogRequest = {
        channel: "CALL",
        direction: "OUTBOUND",
        subject: "Schedule visit",
        body: null,
      };
      const created: CommunicationLogDto = {
        id: "log-2",
        channel: "CALL",
        direction: "OUTBOUND",
        subject: "Schedule visit",
        body: null,
        occurredAt: "2024-01-16T10:00:00Z",
        leadId: null,
        jobId: "job-1",
        createdAt: "2024-01-16T10:00:00Z",
        updatedAt: "2024-01-16T10:00:00Z",
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await addJobCommunicationLog(mockApi, "job-1", payload);

      expect(mockApi.post).toHaveBeenCalledWith(
        "/api/v1/jobs/job-1/communications",
        payload
      );
      expect(mockApi.post).toHaveBeenCalledTimes(1);
    });
  });
});
