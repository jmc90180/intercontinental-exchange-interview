package com.ice.portfolio.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

public record BondRequest(
        @NotBlank(message = "ISIN is required")
        String isin,

        @NotNull(message = "Maturity date is required")
        LocalDate maturityDate,

        @NotEmpty(message = "At least one coupon date is required")
        List<LocalDate> couponDates,

        @PositiveOrZero(message = "Coupon rate must not be negative")
        double couponRate,

        @Positive(message = "Face value must be positive")
        double faceValue,

        @Positive(message = "Market price must be positive")
        double marketPrice
) {}
