package com.ice.portfolio.service.calculator;

import com.ice.portfolio.domain.model.Bond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for YTM calculations verified against textbook values.
 * All test cases are sourced from bond-test-data.md.
 */
class YieldToMaturityCalculatorTest {

    private YieldToMaturityCalculator calculator;

    // Fixed valuation date for deterministic tests
    private static final LocalDate AS_OF = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void setUp() {
        calculator = new YieldToMaturityCalculator();
    }

    /**
     * Test Case 7 from bond-test-data.md: At-par bond.
     * 6% semi-annual, 3yr, price=$100, face=$100.
     * YTM should equal coupon rate (6%) when bond is at par.
     */
    @Test
    void shouldReturnCouponRateForAtParBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 6);

        Bond bond = new Bond.Builder()
                .isin("AT_PAR")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.06)
                .faceValue(100.0)
                .marketPrice(100.0)
                .build();

        double ytm = calculator.calculate(bond, AS_OF);
        assertThat(ytm).isCloseTo(0.06, within(0.001));
    }

    /**
     * Test Case 10 from bond-test-data.md: Premium bond YTM.
     * 6% semi-annual, 10yr, face=$1000, price=$1050.
     * Expected YTM ~5.4%.
     */
    @Test
    void shouldCalculateYtmForPremiumBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 20);

        Bond bond = new Bond.Builder()
                .isin("PREMIUM")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.06)
                .faceValue(1000.0)
                .marketPrice(1050.0)
                .build();

        double ytm = calculator.calculate(bond, AS_OF);

        // Premium bond: YTM < coupon rate
        assertThat(ytm).isLessThan(0.06);
        assertThat(ytm).isCloseTo(0.054, within(0.002));
    }

    /**
     * Test Case 11 from bond-test-data.md: Discount bond YTM.
     * 4.2% semi-annual, 3yr, face=$1000, price=$965.
     * Expected YTM ~5.48%.
     */
    @Test
    void shouldCalculateYtmForDiscountBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 6);

        Bond bond = new Bond.Builder()
                .isin("DISCOUNT")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.042)
                .faceValue(1000.0)
                .marketPrice(965.0)
                .build();

        double ytm = calculator.calculate(bond, AS_OF);

        // Discount bond: YTM > coupon rate
        assertThat(ytm).isGreaterThan(0.042);
        assertThat(ytm).isCloseTo(0.0548, within(0.002));
    }

    /**
     * Annual coupon bond: 5% annual, 3yr, par=100, YTM should give price ~102.775 at 4% YTM.
     * Reversed: given price 102.775, YTM should be ~4%.
     * Test Case 3 from bond-test-data.md.
     */
    @Test
    void shouldCalculateYtmForAnnualCouponBond() {
        List<LocalDate> couponDates = generateAnnualDates(AS_OF, 3);

        Bond bond = new Bond.Builder()
                .isin("ANNUAL")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.05)
                .faceValue(100.0)
                .marketPrice(102.775)
                .build();

        double ytm = calculator.calculate(bond, AS_OF);
        assertThat(ytm).isCloseTo(0.04, within(0.001));
    }

    /**
     * Single remaining coupon period edge case.
     */
    @Test
    void shouldHandleSingleRemainingCoupon() {
        List<LocalDate> couponDates = List.of(LocalDate.of(2026, 7, 1));

        Bond bond = new Bond.Builder()
                .isin("SINGLE")
                .maturityDate(LocalDate.of(2026, 7, 1))
                .couponDates(couponDates)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(990.0)
                .build();

        double ytm = calculator.calculate(bond, AS_OF);
        assertThat(ytm).isPositive();
    }

    private List<LocalDate> generateSemiAnnualDates(LocalDate start, int periods) {
        return IntStream.rangeClosed(1, periods)
                .mapToObj(i -> start.plusMonths(6L * i))
                .toList();
    }

    private List<LocalDate> generateAnnualDates(LocalDate start, int periods) {
        return IntStream.rangeClosed(1, periods)
                .mapToObj(i -> start.plusYears(i))
                .toList();
    }
}
