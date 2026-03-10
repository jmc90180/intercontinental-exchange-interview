package com.ice.portfolio.service;

import com.ice.portfolio.domain.exception.BondNotFoundException;
import com.ice.portfolio.domain.exception.PortfolioNotFoundException;
import com.ice.portfolio.domain.model.Bond;
import com.ice.portfolio.domain.model.Portfolio;
import com.ice.portfolio.domain.repository.PortfolioRepository;
import com.ice.portfolio.service.calculator.DurationCalculator;
import com.ice.portfolio.service.calculator.PortfolioRiskCalculator;
import com.ice.portfolio.service.calculator.YieldToMaturityCalculator;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Service layer that orchestrates risk metric calculations and portfolio operations.
 */
@Service
public class RiskMetricsService {

    private final PortfolioRepository portfolioRepository;
    private final YieldToMaturityCalculator ytmCalculator;
    private final DurationCalculator durationCalculator;
    private final PortfolioRiskCalculator portfolioRiskCalculator;
    private final Clock clock;

    public RiskMetricsService(PortfolioRepository portfolioRepository,
                              YieldToMaturityCalculator ytmCalculator,
                              DurationCalculator durationCalculator,
                              PortfolioRiskCalculator portfolioRiskCalculator,
                              Clock clock) {
        this.portfolioRepository = portfolioRepository;
        this.ytmCalculator = ytmCalculator;
        this.durationCalculator = durationCalculator;
        this.portfolioRiskCalculator = portfolioRiskCalculator;
        this.clock = clock;
    }

    public Portfolio createPortfolio(Portfolio portfolio) {
        return portfolioRepository.save(portfolio);
    }

    public Portfolio getPortfolio(String id) {
        return portfolioRepository.findById(id)
                .orElseThrow(() -> new PortfolioNotFoundException(id));
    }

    public Bond getBond(String portfolioId, String isin) {
        Portfolio portfolio = getPortfolio(portfolioId);
        return portfolio.getBonds().stream()
                .filter(b -> b.getIsin().equals(isin))
                .findFirst()
                .orElseThrow(() -> new BondNotFoundException(isin));
    }

    public double calculateYtm(Bond bond) {
        return ytmCalculator.calculate(bond, valuationDate());
    }

    public double calculateMacaulayDuration(Bond bond, double ytm) {
        return durationCalculator.calculateMacaulayDuration(bond, ytm, valuationDate());
    }

    public double calculateModifiedDuration(double macaulayDuration, double ytm, int periodsPerYear) {
        return durationCalculator.calculateModifiedDuration(macaulayDuration, ytm, periodsPerYear);
    }

    public double calculatePortfolioWeightedDuration(Portfolio portfolio) {
        return portfolioRiskCalculator.calculateWeightedAverageDuration(portfolio, valuationDate());
    }

    private LocalDate valuationDate() {
        return LocalDate.now(clock);
    }
}
