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
 * Unit tests for Duration calculations verified against textbook values.
 * All test cases are sourced from bond-test-data.md.
 */
class DurationCalculatorTest {

    private DurationCalculator calculator;
    private YieldToMaturityCalculator ytmCalculator;

    private static final LocalDate AS_OF = LocalDate.of(2026, 1, 1);

    @BeforeEach
    void setUp() {
        calculator = new DurationCalculator();
        ytmCalculator = new YieldToMaturityCalculator();
    }

    /**
     * Test Case 5 from bond-test-data.md: Semi-annual high-coupon premium bond.
     * Face=$100, 3yr, 10% semi-annual, YTM=4%.
     * Expected: MacD=2.693yr, ModD=2.641yr.
     */
    @Test
    void shouldCalculateDurationForSemiAnnualBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 6);

        Bond bond = new Bond.Builder()
                .isin("SEMIANNUAL")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.10)
                .faceValue(100.0)
                .marketPrice(116.80)
                .build();

        double ytm = 0.04; // Given YTM from test case
        double macD = calculator.calculateMacaulayDuration(bond, ytm, AS_OF);
        double modD = calculator.calculateModifiedDuration(macD, ytm, 2);

        assertThat(macD).isCloseTo(2.693, within(0.02));
        assertThat(modD).isCloseTo(2.641, within(0.02));
    }

    /**
     * Test Case 7 from bond-test-data.md: At-par semi-annual bond.
     * Par=$100, 6% semi-annual, 3yr, YTM=6%.
     * Expected: MacD=2.79yr.
     */
    @Test
    void shouldCalculateDurationForAtParBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 6);

        Bond bond = new Bond.Builder()
                .isin("ATPAR")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.06)
                .faceValue(100.0)
                .marketPrice(100.0)
                .build();

        double ytm = 0.06;
        double macD = calculator.calculateMacaulayDuration(bond, ytm, AS_OF);

        assertThat(macD).isCloseTo(2.79, within(0.02));
    }

    /**
     * Test Case 8 from bond-test-data.md: 10-year semi-annual discount bond.
     * 6% semi-annual, 10yr, YTM=8%.
     * Expected: MacD=7.45yr, ModD=7.16yr.
     */
    @Test
    void shouldCalculateDurationFor10YearBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 20);

        // Calculate the market price at 8% YTM for a 6% semi-annual 10yr bond
        // Price = sum(30/(1.04)^t, t=1..20) + 1000/(1.04)^20
        double price = 0;
        for (int t = 1; t <= 20; t++) {
            price += 30.0 / Math.pow(1.04, t);
        }
        price += 1000.0 / Math.pow(1.04, 20);

        Bond bond = new Bond.Builder()
                .isin("10YEAR")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.06)
                .faceValue(1000.0)
                .marketPrice(price)
                .build();

        double ytm = 0.08;
        double macD = calculator.calculateMacaulayDuration(bond, ytm, AS_OF);
        double modD = calculator.calculateModifiedDuration(macD, ytm, 2);

        assertThat(macD).isCloseTo(7.45, within(0.05));
        assertThat(modD).isCloseTo(7.16, within(0.05));
    }

    /**
     * Test Case 9 from bond-test-data.md.
     * Par=$1000, 5%, price=$960.27, YTM=6.5%, 3yr.
     * Expected: MacD=2.856yr, ModD=2.682yr.
     */
    @Test
    void shouldCalculateDurationForDiscountBond() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 6);

        Bond bond = new Bond.Builder()
                .isin("DQYDJ")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(960.27)
                .build();

        double ytm = 0.065;
        double macD = calculator.calculateMacaulayDuration(bond, ytm, AS_OF);
        double modD = calculator.calculateModifiedDuration(macD, ytm, 2);

        assertThat(macD).isCloseTo(2.856, within(0.05));
        assertThat(modD).isCloseTo(2.682, within(0.05));
    }

    /**
     * Modified Duration should always be less than Macaulay Duration when YTM > 0.
     */
    @Test
    void modifiedDurationShouldBeLessThanMacaulay() {
        double macD = 5.0;
        double ytm = 0.06;
        double modD = calculator.calculateModifiedDuration(macD, ytm, 2);

        assertThat(modD).isLessThan(macD);
    }

    /**
     * Duration should be less than maturity for coupon-bearing bonds.
     */
    @Test
    void durationShouldBeLessThanMaturity() {
        List<LocalDate> couponDates = generateSemiAnnualDates(AS_OF, 20);

        Bond bond = new Bond.Builder()
                .isin("MATURITY_CHECK")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1000.0)
                .build();

        double ytm = ytmCalculator.calculate(bond, AS_OF);
        double macD = calculator.calculateMacaulayDuration(bond, ytm, AS_OF);

        // For coupon bonds, Macaulay Duration < maturity in years
        assertThat(macD).isLessThan(10.0);
        assertThat(macD).isPositive();
    }

    /**
     * Test Case 1 from bond-test-data.md: Annual coupon discount bond.
     * 6% annual, 3yr, YTM=8%, par=100.
     * Expected: MacD=2.82yr.
     */
    @Test
    void shouldCalculateDurationForAnnualCouponBond() {
        List<LocalDate> couponDates = generateAnnualDates(AS_OF, 3);

        // Price at 8% YTM: 6/1.08 + 6/1.08^2 + 106/1.08^3
        double price = 6.0 / 1.08 + 6.0 / Math.pow(1.08, 2) + 106.0 / Math.pow(1.08, 3);

        Bond bond = new Bond.Builder()
                .isin("ANNUAL")
                .maturityDate(couponDates.get(couponDates.size() - 1))
                .couponDates(couponDates)
                .couponRate(0.06)
                .faceValue(100.0)
                .marketPrice(price)
                .build();

        double macD = calculator.calculateMacaulayDuration(bond, 0.08, AS_OF);
        assertThat(macD).isCloseTo(2.82, within(0.02));
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
