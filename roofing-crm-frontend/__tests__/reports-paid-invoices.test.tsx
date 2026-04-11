import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import ReportsPage from "@/app/app/reports/page";
import * as reportsApi from "@/lib/reportsApi";

jest.mock("@/lib/reportsApi");
const mockedReportsApi = reportsApi as jest.Mocked<typeof reportsApi>;

describe("ReportsPage paid invoices report", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedReportsApi.getPaidInvoiceYears.mockResolvedValue([]);
    mockedReportsApi.downloadLeadsCsv.mockResolvedValue({
      blob: new Blob(["lead"], { type: "text/csv" }),
      filename: "leads.csv",
    });
    mockedReportsApi.downloadJobsCsv.mockResolvedValue({
      blob: new Blob(["job"], { type: "text/csv" }),
      filename: "jobs.csv",
    });
    mockedReportsApi.downloadAccountingJobsExcel.mockResolvedValue({
      blob: new Blob(["xlsx"], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      }),
      filename: "accounting-report.xlsx",
    });
  });

  it("shows no paid invoices message and disables Download PDF when no years exist", async () => {
    mockedReportsApi.getPaidInvoiceYears.mockResolvedValue([]);

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByText("No paid invoices available yet.")).toBeInTheDocument();
    });

    expect(screen.getByRole("button", { name: /Download PDF/i })).toBeDisabled();
  });

  it("downloads selected paid invoices PDF for available years", async () => {
    const blob = new Blob(["%PDF-1.4"], { type: "application/pdf" });
    mockedReportsApi.getPaidInvoiceYears.mockResolvedValue([2026, 2025]);
    mockedReportsApi.downloadPaidInvoicesPdf.mockResolvedValue({
      blob,
      filename: "paid-invoices-2026.pdf",
    });
    mockedReportsApi.triggerBrowserDownload = jest.fn();

    render(<ReportsPage />);

    await waitFor(() => {
      expect(screen.getByLabelText("Year")).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Download PDF/i }));

    await waitFor(() => {
      expect(mockedReportsApi.downloadPaidInvoicesPdf).toHaveBeenCalledWith(
        expect.anything(),
        2026
      );
    });
    expect(mockedReportsApi.triggerBrowserDownload).toHaveBeenCalledWith(
      blob,
      "paid-invoices-2026.pdf"
    );
  });
});
