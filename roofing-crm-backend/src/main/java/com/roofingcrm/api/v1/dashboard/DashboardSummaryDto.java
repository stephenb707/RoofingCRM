package com.roofingcrm.api.v1.dashboard;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DashboardSummaryDto {

    private long customerCount;

    private long leadCount;

    private long jobCount;

    private long estimateCount;

    private long invoiceCount;

    private long openTaskCount;

    private Map<String, Long> leadCountByStatus = new LinkedHashMap<>();

    private long jobsScheduledThisWeek;

    private long unscheduledJobsCount;

    private long estimatesSentCount;

    private long unpaidInvoiceCount;

    private long activePipelineLeadCount;

    private List<DashboardLeadSnippetDto> recentLeads;

    private List<DashboardJobSnippetDto> upcomingJobs;

    private List<DashboardTaskSnippetDto> openTasks;
}
