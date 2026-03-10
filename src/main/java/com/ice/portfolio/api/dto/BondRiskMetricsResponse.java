package com.ice.portfolio.api.dto;

public record BondRiskMetricsResponse(
        String isin,
        double yieldToMaturity,
        double macaulayDuration,
        double modifiedDuration
) {}
