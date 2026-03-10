package com.ice.portfolio.service.calculator;

import com.ice.portfolio.domain.model.Bond;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Calculates Macaulay Duration and Modified Duration for a plain vanilla bond.
 *
 * <p>Macaulay Duration is the weighted average time to receive all cash flows:</p>
 * <pre>
 * D_mac = (1/P) * sum(timeInYears_t * PV(CF_t), t=1..n)
 * </pre>
 *
 * <p>Modified Duration measures price sensitivity to yield changes:</p>
 * <pre>
 * D_mod = D_mac / (1 + ytmAnnual / periodsPerYear)
 * </pre>
 */
@Component
public class DurationCalculator {

    /**
     * Calculates the Macaulay Duration in years.
     *
     * @param bond       the bond to calculate duration for
     * @param ytmAnnual  the annualized yield to maturity
     * @param asOfDate   the valuation date
     * @return Macaulay Duration in years
     */
    public double calculateMacaulayDuration(Bond bond, double ytmAnnual, LocalDate asOfDate) {
        List<LocalDate> futureDates = bond.futureCouponDates(asOfDate);
        if (futureDates.isEmpty()) {
            throw new IllegalArgumentException(
                    "No future coupon dates for bond " + bond.getIsin() + " as of " + asOfDate);
        }

        double C = bond.couponPayment();
        double F = bond.getFaceValue();
        double P = bond.getMarketPrice();
        int periodsPerYear = bond.calculatePeriodsPerYear();
        double yPerPeriod = ytmAnnual / periodsPerYear;

        int n = futureDates.size();
        double weightedSum = 0.0;

        for (int t = 1; t <= n; t++) {
            double timeInYears = (double) t / periodsPerYear;
            double cashFlow = C;

            // Last period includes principal repayment
            if (t == n) {
                cashFlow += F;
            }

            double presentValue = cashFlow / Math.pow(1 + yPerPeriod, t);
            weightedSum += timeInYears * presentValue;
        }

        return weightedSum / P;
    }

    /**
     * Calculates the Modified Duration.
     *
     * @param macaulayDuration the Macaulay Duration in years
     * @param ytmAnnual        the annualized yield to maturity
     * @param periodsPerYear   number of coupon payments per year
     * @return Modified Duration in years
     */
    public double calculateModifiedDuration(double macaulayDuration, double ytmAnnual, int periodsPerYear) {
        return macaulayDuration / (1.0 + ytmAnnual / periodsPerYear);
    }
}
