import React from "react";
import { fireEvent, render, screen, waitFor, within } from "./test-utils";
import { ReportBuilderForm } from "@/app/app/reports/customer-reports/ReportBuilderForm";
import * as customerReportsApi from "@/lib/customerPhotoReportsApi";
import * as customersApi from "@/lib/customersApi";
import * as jobsApi from "@/lib/jobsApi";
import * as attachmentsApi from "@/lib/attachmentsApi";

jest.mock("@/lib/customerPhotoReportsApi");
jest.mock("@/lib/customersApi");
jest.mock("@/lib/jobsApi");
jest.mock("@/lib/attachmentsApi");

const mockReplace = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mockReplace, push: jest.fn(), back: jest.fn() }),
}));

const mockedCustomerReportsApi = customerReportsApi as jest.Mocked<typeof customerReportsApi>;
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;
const mockedAttachmentsApi = attachmentsApi as jest.Mocked<typeof attachmentsApi>;

describe("ReportBuilderForm", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    if (!global.crypto?.randomUUID) {
      Object.defineProperty(global, "crypto", {
        value: { randomUUID: () => Math.random().toString(16).slice(2) },
        configurable: true,
      });
    }

    mockedCustomersApi.listCustomers.mockResolvedValue({
      content: [{ id: "cust-1", firstName: "Jane", lastName: "Doe", email: "jane@example.com" }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 200,
      first: true,
      last: true,
    });
    mockedJobsApi.listJobs.mockResolvedValue({
      content: [
        {
          id: "job-1",
          customerId: "cust-1",
          leadId: null,
          statusDefinitionId: "status-1",
          statusKey: "SCHEDULED",
          statusLabel: "Scheduled",
          type: "REPLACEMENT",
          propertyAddress: { line1: "123 Main St" },
          scheduledStartDate: null,
          scheduledEndDate: null,
          internalNotes: null,
          crewName: null,
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 100,
      first: true,
      last: true,
    });
    mockedCustomerReportsApi.listReportAttachmentCandidates.mockResolvedValue([
      {
        id: "img-1",
        fileName: "roof-front.jpg",
        contentType: "image/jpeg",
        fileSize: 100,
        jobId: "job-1",
        leadId: null,
      },
      {
        id: "img-2",
        fileName: "roof-back.jpg",
        contentType: "image/jpeg",
        fileSize: 120,
        jobId: "job-1",
        leadId: null,
      },
    ]);
    mockedAttachmentsApi.downloadAttachment.mockResolvedValue(new Blob(["image"], { type: "image/jpeg" }));
    Object.defineProperty(URL, "createObjectURL", {
      value: jest.fn(() => "blob:preview-url"),
      configurable: true,
    });
  });

  it("hides the picker by default, shows thumbnails when opened, and confirms selection before closing", async () => {
    mockedAttachmentsApi.uploadJobAttachment.mockResolvedValue({
      id: "img-3",
      fileName: "new-upload.jpg",
      contentType: "image/jpeg",
      fileSize: 333,
      jobId: "job-1",
      leadId: null,
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-01-01T00:00:00Z",
    });
    mockedCustomerReportsApi.createCustomerPhotoReport.mockImplementation(async (_api, payload) => ({
      id: "report-1",
      customerId: payload.customerId,
      customerName: "Jane Doe",
      jobId: payload.jobId,
      title: payload.title,
      reportType: payload.reportType,
      summary: payload.summary,
      sections: payload.sections.map((section, index) => ({
        id: `sec-${index + 1}`,
        sortOrder: index,
        title: section.title,
        body: section.body,
        photos: (section.attachmentIds ?? []).map((attachmentId, photoIndex) => ({
          attachmentId,
          sortOrder: photoIndex,
        })),
      })),
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-01-01T00:00:00Z",
    }));

    render(<ReportBuilderForm initialCustomerId="cust-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /\+ add section/i })).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText(/123 Main St/)).toBeInTheDocument();
    });

    const selects = screen.getAllByRole("combobox");
    fireEvent.change(selects[1]!, { target: { value: "job-1" } });
    fireEvent.click(screen.getByRole("button", { name: /\+ add section/i }));

    const section1 = await screen.findByTestId("report-section-1");
    const section2 = await screen.findByTestId("report-section-2");

    expect(within(section1).queryByTestId("photo-picker-list-1")).not.toBeInTheDocument();
    fireEvent.click(within(section1).getByRole("button", { name: /select existing photos/i }));

    await waitFor(() => {
      expect(within(section1).getByTestId("photo-picker-list-1")).toBeInTheDocument();
      expect(within(section1).getByTestId("photo-thumbnail-img-1")).toBeInTheDocument();
      expect(within(section1).getByAltText("roof-front.jpg")).toBeInTheDocument();
    });

    fireEvent.click(within(section1).getByText("roof-front.jpg"));
    fireEvent.click(within(section1).getByRole("button", { name: /confirm selection/i }));

    await waitFor(() => {
      expect(within(section1).queryByTestId("photo-picker-list-1")).not.toBeInTheDocument();
      expect(within(section1).getByText("roof-front.jpg")).toBeInTheDocument();
      expect(within(section1).getByTestId("selected-photo-thumbnail-img-1")).toBeInTheDocument();
      expect(within(section1).getByAltText("Selected roof-front.jpg")).toBeInTheDocument();
    });

    fireEvent.click(within(section2).getByRole("button", { name: /select existing photos/i }));

    await waitFor(() => {
      expect(within(section2).getByLabelText("Upload photos to section 2")).toBeInTheDocument();
    });

    const uploadInput = within(section2).getByLabelText("Upload photos to section 2");
    const file = new File(["bytes"], "new-upload.jpg", { type: "image/jpeg" });
    fireEvent.change(uploadInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(within(section2).getByText("new-upload.jpg")).toBeInTheDocument();
    });

    expect(within(section1).queryByText("new-upload.jpg")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));

    await waitFor(() => {
      expect(mockedCustomerReportsApi.createCustomerPhotoReport).toHaveBeenCalled();
    });

    expect(mockedCustomerReportsApi.createCustomerPhotoReport).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        customerId: "cust-1",
        jobId: "job-1",
        sections: [
          expect.objectContaining({ attachmentIds: ["img-1"] }),
          expect.objectContaining({ attachmentIds: ["img-3"] }),
        ],
      })
    );
  });

  it("removes a photo from only the selected section", async () => {
    mockedCustomerReportsApi.createCustomerPhotoReport.mockImplementation(async (_api, payload) => ({
      id: "report-1",
      customerId: payload.customerId,
      customerName: "Jane Doe",
      jobId: payload.jobId,
      title: payload.title,
      reportType: payload.reportType,
      summary: payload.summary,
      sections: payload.sections.map((section, index) => ({
        id: `sec-${index + 1}`,
        sortOrder: index,
        title: section.title,
        body: section.body,
        photos: (section.attachmentIds ?? []).map((attachmentId, photoIndex) => ({
          attachmentId,
          sortOrder: photoIndex,
        })),
      })),
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-01-01T00:00:00Z",
    }));

    render(<ReportBuilderForm initialCustomerId="cust-1" />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /\+ add section/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /\+ add section/i }));

    const section1 = await screen.findByTestId("report-section-1");
    const section2 = await screen.findByTestId("report-section-2");

    fireEvent.click(within(section1).getByRole("button", { name: /select existing/i }));
    fireEvent.click(within(section1).getByText("roof-front.jpg"));
    fireEvent.click(within(section1).getByRole("button", { name: /confirm selection/i }));
    fireEvent.click(within(section2).getByRole("button", { name: /select existing/i }));
    fireEvent.click(within(section2).getByText("roof-back.jpg"));
    fireEvent.click(within(section2).getByRole("button", { name: /confirm selection/i }));

    await waitFor(() => {
      expect(within(section1).getByRole("button", { name: /remove roof-front\.jpg from section 1/i })).toBeInTheDocument();
      expect(within(section2).getByRole("button", { name: /remove roof-back\.jpg from section 2/i })).toBeInTheDocument();
      expect(within(section1).getByTestId("selected-photo-thumbnail-img-1")).toBeInTheDocument();
      expect(within(section2).getByTestId("selected-photo-thumbnail-img-2")).toBeInTheDocument();
    });

    fireEvent.click(within(section1).getByRole("button", { name: /remove roof-front\.jpg from section 1/i }));

    await waitFor(() => {
      expect(within(section1).queryByRole("button", { name: /remove roof-front\.jpg from section 1/i })).not.toBeInTheDocument();
    });
    expect(within(section2).getByRole("button", { name: /remove roof-back\.jpg from section 2/i })).toBeInTheDocument();
  });

  it("shows a clean related job/date display and prefills send email for an existing report", async () => {
    mockedCustomerReportsApi.getCustomerPhotoReport.mockResolvedValue({
      id: "report-1",
      customerId: "cust-1",
      customerName: "Jane Doe",
      customerEmail: "jane@example.com",
      jobId: "job-1",
      jobDisplayName: "123 Main St, Denver, CO 80202",
      title: "Spring inspection",
      reportType: "Inspection",
      summary: "Summary",
      sections: [],
      createdAt: "2026-04-10T00:00:00Z",
      updatedAt: "2026-04-14T18:30:00Z",
    });
    mockedCustomerReportsApi.sendCustomerPhotoReportEmail.mockResolvedValue({
      success: true,
      sentAt: "2026-04-14T18:35:00Z",
    });

    render(<ReportBuilderForm reportId="report-1" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("Spring inspection")).toBeInTheDocument();
    });

    expect(screen.getByTestId("report-related-job")).toHaveTextContent("123 Main St, Denver, CO 80202");
    expect(screen.getByTestId("report-date")).toHaveTextContent("Apr 14, 2026");

    fireEvent.click(screen.getByRole("button", { name: /send email/i }));
    const dialog = screen.getByRole("dialog", { name: /send report by email/i });

    expect((within(dialog).getByLabelText(/recipient email/i) as HTMLInputElement).value).toBe("jane@example.com");
    expect((within(dialog).getByLabelText(/subject/i) as HTMLInputElement).value).toMatch(/Spring inspection/);
    expect((within(dialog).getByLabelText(/^message \(optional\)$/i) as HTMLTextAreaElement).value).toMatch(
      /Attached is your customer photo report/i
    );

    fireEvent.click(within(dialog).getByRole("button", { name: /^send email$/i }));

    await waitFor(() => {
      expect(mockedCustomerReportsApi.sendCustomerPhotoReportEmail).toHaveBeenCalledWith(
        expect.anything(),
        "report-1",
        expect.objectContaining({
          recipientEmail: "jane@example.com",
          subject: expect.stringContaining("Spring inspection"),
        })
      );
    });
  });
});
