package com.roofingcrm.service.report;

import com.roofingcrm.domain.enums.JobCostCategory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Builds a single-sheet .xlsx workbook for job accounting export.
 */
@Component
public class AccountingJobsExcelExporter {

    private static final String SHEET_NAME = "Job Accounting";

    private static final String[] HEADERS = {
            "Job ID",
            "Job Name",
            "Customer Name",
            "Job Status",
            "Agreed Amount",
            "Invoiced Amount",
            "Paid Amount",
            "Total Costs",
            "Actual Profit",
            "Projected Profit",
            "Actual Margin %",
            "Projected Margin %",
            "Materials Cost",
            "Transportation Cost",
            "Labor Cost",
            "Other Cost",
            "Last Updated"
    };

    public byte[] toXlsxBytes(List<AccountingJobsReportService.AccountingJobExportRow> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CreationHelper creationHelper = wb.getCreationHelper();
            DataFormat dataFormat = creationHelper.createDataFormat();

            Font headerFont = wb.createFont();
            headerFont.setBold(true);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle textStyle = wb.createCellStyle();
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            short currencyFormat = dataFormat.getFormat("$#,##0.00");
            CellStyle currencyStyle = wb.createCellStyle();
            currencyStyle.cloneStyleFrom(textStyle);
            currencyStyle.setDataFormat(currencyFormat);
            currencyStyle.setAlignment(HorizontalAlignment.RIGHT);

            short percentFormat = dataFormat.getFormat("0.00%");
            CellStyle percentStyle = wb.createCellStyle();
            percentStyle.cloneStyleFrom(textStyle);
            percentStyle.setDataFormat(percentFormat);
            percentStyle.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.cloneStyleFrom(textStyle);
            dateStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm"));

            Sheet sheet = wb.createSheet(SHEET_NAME);
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(HEADERS[c]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (AccountingJobsReportService.AccountingJobExportRow rowData : rows) {
                Row row = sheet.createRow(r++);
                int c = 0;
                setText(row.createCell(c++), rowData.job().getId() != null ? rowData.job().getId().toString() : "", textStyle);
                setText(row.createCell(c++), AccountingJobsReportService.jobLabel(rowData.job()), textStyle);
                setText(row.createCell(c++), AccountingJobsReportService.customerName(rowData.job()), textStyle);
                setText(row.createCell(c++), AccountingJobsReportService.humanizeEnum(rowData.job().getStatus()), textStyle);

                var s = rowData.summary();
                setCurrency(row.createCell(c++), s.getAgreedAmount(), currencyStyle);
                setCurrency(row.createCell(c++), s.getInvoicedAmount(), currencyStyle);
                setCurrency(row.createCell(c++), s.getPaidAmount(), currencyStyle);
                setCurrency(row.createCell(c++), s.getTotalCosts(), currencyStyle);
                setCurrency(row.createCell(c++), s.getActualProfit(), currencyStyle);
                setCurrency(row.createCell(c++), s.getProjectedProfit(), currencyStyle);
                setPercent(row.createCell(c++), s.getActualMarginPercent(), percentStyle);
                setPercent(row.createCell(c++), s.getProjectedMarginPercent(), percentStyle);

                var totals = s.getCategoryTotals();
                setCurrency(row.createCell(c++), AccountingJobsReportService.categoryAmount(totals, JobCostCategory.MATERIAL), currencyStyle);
                setCurrency(row.createCell(c++), AccountingJobsReportService.categoryAmount(totals, JobCostCategory.TRANSPORTATION), currencyStyle);
                setCurrency(row.createCell(c++), AccountingJobsReportService.categoryAmount(totals, JobCostCategory.LABOR), currencyStyle);
                setCurrency(row.createCell(c++), AccountingJobsReportService.categoryAmount(totals, JobCostCategory.OTHER), currencyStyle);

                setInstant(row.createCell(c++), rowData.job().getUpdatedAt(), dateStyle);
            }

            sheet.createFreezePane(0, 1);
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
                int w = sheet.getColumnWidth(i);
                int max = 18000;
                if (w > max) {
                    sheet.setColumnWidth(i, max);
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build accounting Excel export", e);
        }
    }

    private static void setText(Cell cell, String value, CellStyle style) {
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private static void setCurrency(Cell cell, BigDecimal amount, CellStyle style) {
        if (amount == null) {
            cell.setBlank();
            cell.setCellStyle(style);
            return;
        }
        cell.setCellValue(amount.doubleValue());
        cell.setCellStyle(style);
    }

    private static void setPercent(Cell cell, BigDecimal percentPoints, CellStyle style) {
        if (percentPoints == null) {
            cell.setBlank();
            cell.setCellStyle(style);
            return;
        }
        double ratio = percentPoints.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP).doubleValue();
        cell.setCellValue(ratio);
        cell.setCellStyle(style);
    }

    private static void setInstant(Cell cell, Instant instant, CellStyle dateStyle) {
        if (instant == null) {
            cell.setBlank();
            cell.setCellStyle(dateStyle);
            return;
        }
        cell.setCellValue(Date.from(instant));
        cell.setCellStyle(dateStyle);
    }
}
