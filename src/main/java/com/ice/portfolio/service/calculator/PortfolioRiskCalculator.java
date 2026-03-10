package com.ice.portfolio.service.calculator;

import com.ice.portfolio.domain.model.Bond;
import com.ice.portfolio.domain.model.Portfolio;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Calculates portfolio-level risk metrics.
 *
 * <p>Portfolio weighted average duration:</p>
 * <pre>
 * D_portfolio = sum(w_i * D_mod_i)
 * where w_i = marketPrice_i / totalMarketValue
 * </pre>
 */
@Component
public class PortfolioRiskCalculator {

    private final YieldToMaturityCalculator ytmCalculator;
    private final DurationCalculator durationCalculator;

    public PortfolioRiskCalculator(YieldToMaturityCalculator ytmCalculator,
                                   DurationCalculator durationCalculator) {
        this.ytmCalculator = ytmCalculator;
        this.durationCalculator = durationCalculator;
    }

    /**
     * Calculates the portfolio-level weighted average modified duration.
     *
     * @param portfolio the portfolio to analyze
     * @param asOfDate  the valuation date
     * @return weighted average modified duration in years
     */
    public double calculateWeightedAverageDuration(Portfolio portfolio, LocalDate asOfDate) {
        double totalMarketValue = portfolio.totalMarketValue();
        if (totalMarketValue == 0) {
            return 0;
        }

        double weightedDuration = 0.0;

        for (Bond bond : portfolio.getBonds()) {
            double ytm = ytmCalculator.calculate(bond, asOfDate);
            double macaulayDuration = durationCalculator.calculateMacaulayDuration(bond, ytm, asOfDate);
            double modifiedDuration = durationCalculator.calculateModifiedDuration(
                    macaulayDuration, ytm, bond.calculatePeriodsPerYear());
            double weight = bond.getMarketPrice() / totalMarketValue;
            weightedDuration += weight * modifiedDuration;
        }

        return weightedDuration;
    }
}
