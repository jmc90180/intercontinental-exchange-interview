package com.ice.portfolio.api.dto;

public record PortfolioResponse(
        String id,
        String name,
        int bondCount,
        double totalMarketValue
) {}
