package com.ice.portfolio.service.calculator;

import com.ice.portfolio.domain.model.Bond;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Calculates the Yield to Maturity (YTM) for a plain vanilla bond using the Newton-Raphson method.
 *
 * <p>YTM is the discount rate that equates the present value of all future cash flows
 * (coupon payments + principal) to the bond's current market price:</p>
 *
 * <pre>
 * P = sum(C / (1+y)^t, t=1..n) + F / (1+y)^n
 * </pre>
 *
 * <p>Where P = market price, C = coupon payment per period, F = face value,
 * y = yield per period, n = number of remaining periods.</p>
 */
@Component
public class YieldToMaturityCalculator {

    private static final int MAX_ITERATIONS = 1000;
    private static final double TOLERANCE = 1e-10;

    /**
     * Calculates the annualized Yield to Maturity for a bond.
     *
     * @param bond     the bond to calculate YTM for
     * @param asOfDate the valuation date (typically today)
     * @return the annualized YTM as a decimal (e.g., 0.05 for 5%)
     * @throws IllegalStateException if Newton-Raphson fails to converge
     */
    public double calculate(Bond bond, LocalDate asOfDate) {
        List<LocalDate> futureDates = bond.futureCouponDates(asOfDate);
        if (futureDates.isEmpty()) {
            throw new IllegalArgumentException(
                    "No future coupon dates for bond " + bond.getIsin() + " as of " + asOfDate);
        }

        int n = futureDates.size();
        double C = bond.couponPayment();
        double F = bond.getFaceValue();
        double P = bond.getMarketPrice();
        int periodsPerYear = bond.calculatePeriodsPerYear();

        // Initial guess using the simple YTM approximation formula
        double y = (C + (F - P) / n) / ((F + P) / 2.0);

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double fValue = -P;
            double fDerivative = 0.0;

            for (int t = 1; t <= n; t++) {
                double discountFactor = Math.pow(1 + y, t);
                fValue += C / discountFactor;
                fDerivative += -t * C / Math.pow(1 + y, t + 1);
            }

            // Add face value repayment at maturity (last period)
            fValue += F / Math.pow(1 + y, n);
            fDerivative += -n * F / Math.pow(1 + y, n + 1);

            if (Math.abs(fValue) < TOLERANCE) {
                return y * periodsPerYear;
            }

            // Guard against zero derivative
            if (Math.abs(fDerivative) < TOLERANCE) {
                throw new IllegalStateException(
                        "YTM calculation encountered zero derivative for bond " + bond.getIsin());
            }

            y = y - fValue / fDerivative;
        }

        throw new IllegalStateException(
                "YTM calculation did not converge within " + MAX_ITERATIONS
                        + " iterations for bond " + bond.getIsin());
    }
}
