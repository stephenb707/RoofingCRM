import type { AxiosInstance } from "axios";
import {
  listLeadAttachments,
  uploadLeadAttachment,
  listJobAttachments,
  uploadJobAttachment,
  downloadAttachment,
} from "./attachmentsApi";
import type { AttachmentDto } from "./types";

describe("attachmentsApi", () => {
  const mockApi = {
    get: jest.fn(),
    post: jest.fn(),
  } as unknown as AxiosInstance;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("listLeadAttachments", () => {
    it("calls GET with correct URL", async () => {
      const data: AttachmentDto[] = [];
      (mockApi.get as jest.Mock).mockResolvedValue({ data });

      await listLeadAttachments(mockApi, "lead-1");

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/leads/lead-1/attachments");
      expect(mockApi.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("uploadLeadAttachment", () => {
    it("calls POST with FormData containing file", async () => {
      const file = new File(["content"], "doc.pdf", { type: "application/pdf" });
      const created: AttachmentDto = {
        id: "att-1",
        fileName: "doc.pdf",
        contentType: "application/pdf",
        fileSize: 7,
        leadId: "lead-1",
        jobId: null,
        tag: "OTHER",
        createdAt: "2024-01-01T00:00:00Z",
        updatedAt: "2024-01-01T00:00:00Z",
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await uploadLeadAttachment(mockApi, "lead-1", file);

      expect(mockApi.post).toHaveBeenCalledTimes(1);
      expect(mockApi.post).toHaveBeenCalledWith(
        "/api/v1/leads/lead-1/attachments",
        expect.any(FormData)
      );
      const formData = (mockApi.post as jest.Mock).mock.calls[0][1] as FormData;
      expect(formData.get("file")).toBe(file);
    });

    it("includes tag and description in FormData when provided", async () => {
      const file = new File(["x"], "damage.jpg", { type: "image/jpeg" });
      const created: AttachmentDto = {
        id: "att-2",
        fileName: "damage.jpg",
        contentType: "image/jpeg",
        fileSize: 1,
        leadId: "lead-1",
        jobId: null,
        tag: "DAMAGE",
        description: "Roof damage",
        createdAt: "2024-01-01T00:00:00Z",
        updatedAt: "2024-01-01T00:00:00Z",
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await uploadLeadAttachment(mockApi, "lead-1", file, {
        tag: "DAMAGE",
        description: "Roof damage",
      });

      const formData = (mockApi.post as jest.Mock).mock.calls[0][1] as FormData;
      expect(formData.get("tag")).toBe("DAMAGE");
      expect(formData.get("description")).toBe("Roof damage");
    });
  });

  describe("listJobAttachments", () => {
    it("calls GET with correct URL", async () => {
      const data: AttachmentDto[] = [];
      (mockApi.get as jest.Mock).mockResolvedValue({ data });

      await listJobAttachments(mockApi, "job-1");

      expect(mockApi.get).toHaveBeenCalledWith("/api/v1/jobs/job-1/attachments");
      expect(mockApi.get).toHaveBeenCalledTimes(1);
    });
  });

  describe("uploadJobAttachment", () => {
    it("calls POST with FormData containing file", async () => {
      const file = new File(["x"], "image.png", { type: "image/png" });
      const created: AttachmentDto = {
        id: "att-2",
        fileName: "image.png",
        contentType: "image/png",
        fileSize: 1,
        leadId: null,
        jobId: "job-1",
        tag: "OTHER",
        createdAt: "2024-01-01T00:00:00Z",
        updatedAt: "2024-01-01T00:00:00Z",
      };
      (mockApi.post as jest.Mock).mockResolvedValue({ data: created });

      await uploadJobAttachment(mockApi, "job-1", file);

      expect(mockApi.post).toHaveBeenCalledWith(
        "/api/v1/jobs/job-1/attachments",
        expect.any(FormData)
      );
      const formData = (mockApi.post as jest.Mock).mock.calls[0][1] as FormData;
      expect(formData.get("file")).toBe(file);
    });
  });

  describe("downloadAttachment", () => {
    it("calls GET with responseType blob", async () => {
      const blob = new Blob(["binary"]);
      (mockApi.get as jest.Mock).mockResolvedValue({ data: blob });

      await downloadAttachment(mockApi, "att-1");

      expect(mockApi.get).toHaveBeenCalledWith(
        "/api/v1/attachments/att-1/download",
        { responseType: "blob" }
      );
      expect(mockApi.get).toHaveBeenCalledTimes(1);
    });
  });
});
