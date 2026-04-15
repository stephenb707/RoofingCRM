import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import ReportsPage from "@/app/app/reports/page";
import * as reportsApi from "@/lib/reportsApi";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";

jest.mock("@/lib/reportsApi");
const mockedReportsApi = reportsApi as jest.Mocked<typeof reportsApi>;

jest.mock("@/lib/pipelineStatusesApi");
const mockedPipelineApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

describe("ReportsPage", () => {
  const mockBlob = new Blob(["a,b\n1,2"], { type: "text/csv" });
  const mockDownloadResult = { blob: mockBlob, filename: "x.csv" };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedPipelineApi.listPipelineStatuses.mockImplementation((_api, type) => {
      if (type === "LEAD") {
        return Promise.resolve([
          {
            id: "lead-new",
            pipelineType: "LEAD",
            systemKey: "NEW",
            label: "New",
            sortOrder: 0,
            builtIn: true,
            active: true,
          },
        ]);
      }
      return Promise.resolve([
        {
          id: "job-completed",
          pipelineType: "JOB",
          systemKey: "COMPLETED",
          label: "Completed",
          sortOrder: 0,
          builtIn: true,
          active: true,
        },
      ]);
    });
    mockedReportsApi.getPaidInvoiceYears.mockResolvedValue([]);
    mockedReportsApi.downloadAccountingJobsExcel.mockResolvedValue({
      blob: new Blob(["xlsx"], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      }),
      filename: "accounting-report-2026-04-09.xlsx",
    });
  });

  it("renders both sections and download buttons", async () => {
    mockedReportsApi.downloadLeadsCsv.mockResolvedValue(mockDownloadResult);
    mockedReportsApi.downloadJobsCsv.mockResolvedValue(mockDownloadResult);

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByText("Reports")).toBeInTheDocument();
    });

    expect(screen.getByText("Pipeline / Leads Export")).toBeInTheDocument();
    expect(screen.getByText("Jobs Export")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Download Leads CSV/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Download Jobs CSV/i })).toBeInTheDocument();
    expect(screen.getByText("Accounting Report")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Download Excel/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /Customer photo reports/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Open photo reports/i })).toHaveAttribute(
      "href",
      "/app/reports/customer-reports"
    );
  });

  it("calls downloadLeadsCsv with selected filters when clicking Download Leads CSV", async () => {
    mockedReportsApi.downloadLeadsCsv.mockResolvedValue(mockDownloadResult);

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByText("Pipeline / Leads Export")).toBeInTheDocument();
    });

    const leadsStatusSelect = document.getElementById("leads-status");
    if (!leadsStatusSelect) throw new Error("leads-status not found");
    fireEvent.change(leadsStatusSelect, { target: { value: "lead-new" } });

    const sourceSelect = document.getElementById("leads-source");
    if (!sourceSelect) throw new Error("leads-source not found");
    fireEvent.change(sourceSelect, { target: { value: "WEBSITE" } });

    const downloadBtn = screen.getByRole("button", { name: /Download Leads CSV/i });
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(mockedReportsApi.downloadLeadsCsv).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          statusDefinitionId: "lead-new",
          source: "WEBSITE",
        })
      );
    });
  });

  it("calls downloadJobsCsv with selected filters when clicking Download Jobs CSV", async () => {
    mockedReportsApi.downloadJobsCsv.mockResolvedValue(mockDownloadResult);

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByText("Jobs Export")).toBeInTheDocument();
    });

    const statusComboboxes = screen.getAllByRole("combobox", { name: /status/i });
    const jobsStatusSelect = statusComboboxes[1];
    fireEvent.change(jobsStatusSelect, { target: { value: "job-completed" } });

    const downloadBtn = screen.getByRole("button", { name: /Download Jobs CSV/i });
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(mockedReportsApi.downloadJobsCsv).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          statusDefinitionId: "job-completed",
        })
      );
    });
  });

  it("calls triggerBrowserDownload after successful leads download", async () => {
    mockedReportsApi.downloadLeadsCsv.mockResolvedValue(mockDownloadResult);
    mockedReportsApi.triggerBrowserDownload = jest.fn();

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Download Leads CSV/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Download Leads CSV/i }));

    await waitFor(() => {
      expect(mockedReportsApi.triggerBrowserDownload).toHaveBeenCalledWith(mockBlob, "x.csv");
    });
  });

  it("calls downloadAccountingJobsExcel when clicking Download Excel", async () => {
    const accountingResult = {
      blob: new Blob(["xlsx"], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      }),
      filename: "accounting-report-2026-04-09.xlsx",
    };
    mockedReportsApi.downloadAccountingJobsExcel.mockResolvedValue(accountingResult);
    mockedReportsApi.triggerBrowserDownload = jest.fn();

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Download Excel/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Download Excel/i }));

    await waitFor(() => {
      expect(mockedReportsApi.downloadAccountingJobsExcel).toHaveBeenCalledWith(expect.anything());
    });
    expect(mockedReportsApi.triggerBrowserDownload).toHaveBeenCalledWith(
      accountingResult.blob,
      accountingResult.filename
    );
  });

  it("shows error message when leads download fails", async () => {
    mockedReportsApi.downloadLeadsCsv.mockRejectedValue(new Error("Network error"));

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Download Leads CSV/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Download Leads CSV/i }));

    await waitFor(() => {
      expect(screen.getByText("Failed to download. Check backend is running.")).toBeInTheDocument();
    });
  });
});
