package com.ice.portfolio.api.dto;

import java.time.LocalDate;
import java.util.List;

public record BondResponse(
        String isin,
        LocalDate maturityDate,
        List<LocalDate> couponDates,
        double couponRate,
        double faceValue,
        double marketPrice
) {}
