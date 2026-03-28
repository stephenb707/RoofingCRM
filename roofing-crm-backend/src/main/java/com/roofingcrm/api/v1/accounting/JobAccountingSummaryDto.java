package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.domain.enums.JobCostCategory;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class JobAccountingSummaryDto {

    private BigDecimal agreedAmount;
    private BigDecimal invoicedAmount;
    private BigDecimal paidAmount;
    private BigDecimal totalCosts;
    private BigDecimal grossProfit;
    private BigDecimal marginPercent;
    private BigDecimal projectedProfit;
    private BigDecimal actualProfit;
    private BigDecimal projectedMarginPercent;
    private BigDecimal actualMarginPercent;
    private Map<JobCostCategory, BigDecimal> categoryTotals;
    private boolean hasAcceptedEstimate;
}
