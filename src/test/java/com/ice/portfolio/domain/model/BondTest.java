package com.ice.portfolio.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BondTest {

    private static final List<LocalDate> SEMI_ANNUAL_DATES = List.of(
            LocalDate.of(2027, 6, 15),
            LocalDate.of(2027, 12, 15),
            LocalDate.of(2028, 6, 15),
            LocalDate.of(2028, 12, 15)
    );

    @Test
    void shouldBuildValidBond() {
        Bond bond = new Bond.Builder()
                .isin("US912828ZT58")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1020.50)
                .build();

        assertThat(bond.getIsin()).isEqualTo("US912828ZT58");
        assertThat(bond.getFaceValue()).isEqualTo(1000.0);
        assertThat(bond.getMarketPrice()).isEqualTo(1020.50);
        assertThat(bond.getCouponDates()).hasSize(4);
    }

    @Test
    void shouldRejectNullIsin() {
        assertThatThrownBy(() -> new Bond.Builder()
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1020.50)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ISIN");
    }

    @Test
    void shouldRejectBlankIsin() {
        assertThatThrownBy(() -> new Bond.Builder()
                .isin("   ")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1020.50)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISIN");
    }

    @Test
    void shouldRejectNegativeFaceValue() {
        assertThatThrownBy(() -> new Bond.Builder()
                .isin("US912828ZT58")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(-1000.0)
                .marketPrice(1020.50)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Face value");
    }

    @Test
    void shouldRejectEmptyCouponDates() {
        assertThatThrownBy(() -> new Bond.Builder()
                .isin("US912828ZT58")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(List.of())
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1020.50)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coupon dates");
    }

    @Test
    void shouldRejectNegativeCouponRate() {
        assertThatThrownBy(() -> new Bond.Builder()
                .isin("US912828ZT58")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(-0.01)
                .faceValue(1000.0)
                .marketPrice(1020.50)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coupon rate");
    }

    @Test
    void shouldCalculateCouponPaymentForSemiAnnualBond() {
        Bond bond = new Bond.Builder()
                .isin("TEST001")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.06)
                .faceValue(1000.0)
                .marketPrice(1000.0)
                .build();

        // 6% annual rate, semi-annual payments: 1000 * 0.06 / 2 = 30
        assertThat(bond.couponPayment()).isEqualTo(30.0);
    }

    @Test
    void shouldInferSemiAnnualFrequency() {
        Bond bond = new Bond.Builder()
                .isin("TEST001")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1000.0)
                .build();

        assertThat(bond.calculatePeriodsPerYear()).isEqualTo(2);
    }

    @Test
    void shouldInferAnnualFrequency() {
        List<LocalDate> annualDates = List.of(
                LocalDate.of(2027, 6, 15),
                LocalDate.of(2028, 6, 15)
        );

        Bond bond = new Bond.Builder()
                .isin("TEST002")
                .maturityDate(LocalDate.of(2028, 6, 15))
                .couponDates(annualDates)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1000.0)
                .build();

        assertThat(bond.calculatePeriodsPerYear()).isEqualTo(1);
    }

    @Test
    void shouldFilterFutureCouponDates() {
        Bond bond = new Bond.Builder()
                .isin("TEST001")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1000.0)
                .build();

        LocalDate asOf = LocalDate.of(2027, 7, 1);
        List<LocalDate> future = bond.futureCouponDates(asOf);

        assertThat(future).hasSize(3);
        assertThat(future.get(0)).isEqualTo(LocalDate.of(2027, 12, 15));
    }

    @Test
    void shouldReturnUnmodifiableCouponDates() {
        Bond bond = new Bond.Builder()
                .isin("TEST001")
                .maturityDate(LocalDate.of(2028, 12, 15))
                .couponDates(SEMI_ANNUAL_DATES)
                .couponRate(0.05)
                .faceValue(1000.0)
                .marketPrice(1000.0)
                .build();

        assertThatThrownBy(() -> bond.getCouponDates().add(LocalDate.now()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
