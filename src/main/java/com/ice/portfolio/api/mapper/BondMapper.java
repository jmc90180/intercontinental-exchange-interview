package com.ice.portfolio.api.mapper;

import com.ice.portfolio.api.dto.BondRequest;
import com.ice.portfolio.api.dto.BondResponse;
import com.ice.portfolio.api.dto.PortfolioRequest;
import com.ice.portfolio.api.dto.PortfolioResponse;
import com.ice.portfolio.domain.model.Bond;
import com.ice.portfolio.domain.model.Portfolio;

import java.util.List;

/**
 * Maps between API DTOs and domain objects.
 */
public final class BondMapper {

    private BondMapper() {
        // Utility class
    }

    public static Bond toDomain(BondRequest request) {
        return new Bond.Builder()
                .isin(request.isin())
                .maturityDate(request.maturityDate())
                .couponDates(request.couponDates())
                .couponRate(request.couponRate())
                .faceValue(request.faceValue())
                .marketPrice(request.marketPrice())
                .build();
    }

    public static Portfolio toDomain(PortfolioRequest request) {
        List<Bond> bonds = request.bonds().stream()
                .map(BondMapper::toDomain)
                .toList();
        return new Portfolio(request.name(), bonds);
    }

    public static BondResponse toResponse(Bond bond) {
        return new BondResponse(
                bond.getIsin(),
                bond.getMaturityDate(),
                bond.getCouponDates(),
                bond.getCouponRate(),
                bond.getFaceValue(),
                bond.getMarketPrice()
        );
    }

    public static PortfolioResponse toResponse(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.getId(),
                portfolio.getName(),
                portfolio.getBonds().size(),
                portfolio.totalMarketValue()
        );
    }
}
