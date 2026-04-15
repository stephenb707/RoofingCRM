package com.roofingcrm.service.customerreport;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.value.Address;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CustomerPhotoReportPresentationHelper {

    private static final DateTimeFormatter JOB_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final ZoneId REPORT_DATE_ZONE = ZoneId.systemDefault();

    private CustomerPhotoReportPresentationHelper() {
    }

    public static String formatCustomerName(Customer customer) {
        if (customer == null) {
            return "";
        }
        String name = ((customer.getFirstName() != null ? customer.getFirstName() : "") + " "
                + (customer.getLastName() != null ? customer.getLastName() : "")).trim();
        return name.isEmpty() ? "Customer" : name;
    }

    public static Instant resolveReportDate(CustomerPhotoReport report) {
        if (report == null) {
            return Instant.now();
        }
        if (report.getUpdatedAt() != null) {
            return report.getUpdatedAt();
        }
        if (report.getCreatedAt() != null) {
            return report.getCreatedAt();
        }
        return Instant.now();
    }

    public static LocalDate resolveReportLocalDate(CustomerPhotoReport report) {
        return resolveReportDate(report).atZone(REPORT_DATE_ZONE).toLocalDate();
    }

    public static String formatJobDisplay(Job job) {
        if (job == null) {
            return null;
        }
        String address = formatAddress(job.getPropertyAddress());
        if (!address.isBlank()) {
            return address;
        }

        LocalDate scheduledStart = job.getScheduledStartDate();
        if (scheduledStart != null) {
            return "Job scheduled " + JOB_DATE_FORMAT.format(scheduledStart);
        }

        if (job.getCustomer() != null) {
            String customerName = formatCustomerName(job.getCustomer());
            if (!customerName.isBlank()) {
                return customerName + " job";
            }
        }

        return job.getId() != null ? "Job reference " + job.getId().toString().substring(0, 8) : "Related job";
    }

    private static String formatAddress(Address address) {
        if (address == null) {
            return "";
        }
        String cityState = joinNonBlank(", ", address.getCity(), address.getState());
        return joinNonBlank(", ", address.getLine1(), address.getLine2(), cityState, address.getZip());
    }

    private static String joinNonBlank(String delimiter, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(delimiter);
            }
            sb.append(part.trim());
        }
        return sb.toString();
    }
}
