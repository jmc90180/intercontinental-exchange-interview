package com.ice.portfolio.api.controller;

import com.ice.portfolio.api.dto.BondResponse;
import com.ice.portfolio.api.dto.BondRiskMetricsResponse;
import com.ice.portfolio.api.dto.PortfolioRequest;
import com.ice.portfolio.api.dto.PortfolioResponse;
import com.ice.portfolio.api.dto.PortfolioRiskMetricsResponse;
import com.ice.portfolio.api.mapper.BondMapper;
import com.ice.portfolio.domain.model.Bond;
import com.ice.portfolio.domain.model.Portfolio;
import com.ice.portfolio.service.RiskMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@Tag(name = "Portfolio", description = "Bond portfolio management and risk metrics")
public class PortfolioController {

    private final RiskMetricsService riskMetricsService;

    public PortfolioController(RiskMetricsService riskMetricsService) {
        this.riskMetricsService = riskMetricsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a portfolio", description = "Load bonds from JSON and create a new portfolio")
    @ApiResponse(responseCode = "201", description = "Portfolio created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    public PortfolioResponse createPortfolio(@Valid @RequestBody PortfolioRequest request) {
        Portfolio portfolio = BondMapper.toDomain(request);
        Portfolio saved = riskMetricsService.createPortfolio(portfolio);
        return BondMapper.toResponse(saved);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get portfolio", description = "Retrieve portfolio details by ID")
    @ApiResponse(responseCode = "200", description = "Portfolio found")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public PortfolioResponse getPortfolio(@PathVariable String id) {
        Portfolio portfolio = riskMetricsService.getPortfolio(id);
        return BondMapper.toResponse(portfolio);
    }

    @GetMapping("/{id}/bonds")
    @Operation(summary = "List bonds", description = "List all bonds in a portfolio")
    public List<BondResponse> getBonds(@PathVariable String id) {
        Portfolio portfolio = riskMetricsService.getPortfolio(id);
        return portfolio.getBonds().stream()
                .map(BondMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}/bonds/{isin}")
    @Operation(summary = "Get bond", description = "Retrieve a specific bond by ISIN within a portfolio")
    @ApiResponse(responseCode = "200", description = "Bond found")
    @ApiResponse(responseCode = "404", description = "Portfolio or bond not found")
    public BondResponse getBond(@PathVariable String id, @PathVariable String isin) {
        Bond bond = riskMetricsService.getBond(id, isin);
        return BondMapper.toResponse(bond);
    }

    @GetMapping("/{id}/bonds/{isin}/risk-metrics")
    @Operation(summary = "Bond risk metrics",
            description = "Calculate YTM, Macaulay Duration, and Modified Duration for a specific bond")
    @ApiResponse(responseCode = "200", description = "Risk metrics calculated")
    @ApiResponse(responseCode = "404", description = "Portfolio or bond not found")
    public BondRiskMetricsResponse getBondRiskMetrics(@PathVariable String id, @PathVariable String isin) {
        Bond bond = riskMetricsService.getBond(id, isin);

        double ytm = riskMetricsService.calculateYtm(bond);
        double macaulayDuration = riskMetricsService.calculateMacaulayDuration(bond, ytm);
        double modifiedDuration = riskMetricsService.calculateModifiedDuration(
                macaulayDuration, ytm, bond.calculatePeriodsPerYear());

        return new BondRiskMetricsResponse(bond.getIsin(), ytm, macaulayDuration, modifiedDuration);
    }

    @GetMapping("/{id}/risk-metrics")
    @Operation(summary = "Portfolio risk metrics",
            description = "Calculate weighted average duration and per-bond metrics for the portfolio")
    @ApiResponse(responseCode = "200", description = "Portfolio risk metrics calculated")
    @ApiResponse(responseCode = "404", description = "Portfolio not found")
    public PortfolioRiskMetricsResponse getPortfolioRiskMetrics(@PathVariable String id) {
        Portfolio portfolio = riskMetricsService.getPortfolio(id);
        double weightedAvgDuration = riskMetricsService.calculatePortfolioWeightedDuration(portfolio);

        List<PortfolioRiskMetricsResponse.BondMetricDetail> bondMetrics = new ArrayList<>();
        for (Bond bond : portfolio.getBonds()) {
            double ytm = riskMetricsService.calculateYtm(bond);
            double macD = riskMetricsService.calculateMacaulayDuration(bond, ytm);
            double modD = riskMetricsService.calculateModifiedDuration(
                    macD, ytm, bond.calculatePeriodsPerYear());
            double weight = portfolio.bondWeight(bond);

            bondMetrics.add(new PortfolioRiskMetricsResponse.BondMetricDetail(
                    bond.getIsin(), weight, ytm, macD, modD));
        }

        return new PortfolioRiskMetricsResponse(
                portfolio.getId(), portfolio.getName(), weightedAvgDuration, bondMetrics);
    }
}
