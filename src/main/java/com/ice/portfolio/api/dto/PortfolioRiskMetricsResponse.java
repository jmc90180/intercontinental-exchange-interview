package com.ice.portfolio.api.dto;

import java.util.List;

public record PortfolioRiskMetricsResponse(
        String portfolioId,
        String portfolioName,
        double weightedAverageDuration,
        List<BondMetricDetail> bondMetrics
) {
    public record BondMetricDetail(
            String isin,
            double weight,
            double yieldToMaturity,
            double macaulayDuration,
            double modifiedDuration
    ) {}
}
