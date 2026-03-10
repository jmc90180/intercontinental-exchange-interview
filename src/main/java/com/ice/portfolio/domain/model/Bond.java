package com.ice.portfolio.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents a plain vanilla bond with fixed periodic coupon payments.
 * Immutable after construction via the Builder pattern.
 */
@Entity
@Table(name = "bonds")
public class Bond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String isin;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @ElementCollection
    @CollectionTable(name = "bond_coupon_dates", joinColumns = @JoinColumn(name = "bond_id"))
    @Column(name = "coupon_date")
    @OrderColumn(name = "date_order")
    private List<LocalDate> couponDates = new ArrayList<>();

    @Column(nullable = false)
    private double couponRate;

    @Column(nullable = false)
    private double faceValue;

    @Column(nullable = false)
    private double marketPrice;

    protected Bond() {
        // Required by JPA
    }

    private Bond(Builder builder) {
        this.isin = builder.isin;
        this.maturityDate = builder.maturityDate;
        this.couponDates = new ArrayList<>(builder.couponDates);
        this.couponRate = builder.couponRate;
        this.faceValue = builder.faceValue;
        this.marketPrice = builder.marketPrice;
    }

    public Long getId() {
        return id;
    }

    public String getIsin() {
        return isin;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public List<LocalDate> getCouponDates() {
        return Collections.unmodifiableList(couponDates);
    }

    public double getCouponRate() {
        return couponRate;
    }

    public double getFaceValue() {
        return faceValue;
    }

    public double getMarketPrice() {
        return marketPrice;
    }

    /**
     * Calculates the coupon payment per period.
     * For a bond with annual coupon rate C and n payments per year:
     * payment = faceValue * couponRate / periodsPerYear
     */
    public double couponPayment() {
        int periodsPerYear = calculatePeriodsPerYear();
        return faceValue * couponRate / periodsPerYear;
    }

    /**
     * Infers the number of coupon payments per year from the spacing between coupon dates.
     * Falls back to semi-annual (2) if fewer than 2 coupon dates are available.
     */
    public int calculatePeriodsPerYear() {
        if (couponDates.size() < 2) {
            return inferPeriodsFromSingleDate();
        }

        List<LocalDate> sorted = couponDates.stream().sorted().collect(Collectors.toList());
        long daysBetween = ChronoUnit.DAYS.between(sorted.get(0), sorted.get(1));

        if (daysBetween <= 100) {       // ~quarterly (90 days)
            return 4;
        } else if (daysBetween <= 200) { // ~semi-annual (180 days)
            return 2;
        } else {                         // ~annual (365 days)
            return 1;
        }
    }

    /**
     * Returns only the coupon dates that fall after the given valuation date, sorted chronologically.
     */
    public List<LocalDate> futureCouponDates(LocalDate asOfDate) {
        return couponDates.stream()
                .filter(d -> d.isAfter(asOfDate))
                .sorted()
                .collect(Collectors.toList());
    }

    private int inferPeriodsFromSingleDate() {
        // Default to semi-annual if we can't infer from coupon date spacing
        return 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bond bond = (Bond) o;
        return Objects.equals(isin, bond.isin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isin);
    }

    @Override
    public String toString() {
        return "Bond{" +
                "isin='" + isin + '\'' +
                ", maturityDate=" + maturityDate +
                ", couponRate=" + couponRate +
                ", faceValue=" + faceValue +
                ", marketPrice=" + marketPrice +
                '}';
    }

    public static class Builder {
        private String isin;
        private LocalDate maturityDate;
        private List<LocalDate> couponDates = new ArrayList<>();
        private double couponRate;
        private double faceValue;
        private double marketPrice;

        public Builder isin(String isin) {
            this.isin = isin;
            return this;
        }

        public Builder maturityDate(LocalDate maturityDate) {
            this.maturityDate = maturityDate;
            return this;
        }

        public Builder couponDates(List<LocalDate> couponDates) {
            this.couponDates = new ArrayList<>(couponDates);
            return this;
        }

        public Builder couponRate(double couponRate) {
            this.couponRate = couponRate;
            return this;
        }

        public Builder faceValue(double faceValue) {
            this.faceValue = faceValue;
            return this;
        }

        public Builder marketPrice(double marketPrice) {
            this.marketPrice = marketPrice;
            return this;
        }

        public Bond build() {
            validate();
            return new Bond(this);
        }

        private void validate() {
            Objects.requireNonNull(isin, "ISIN must not be null");
            if (isin.isBlank()) {
                throw new IllegalArgumentException("ISIN must not be blank");
            }
            Objects.requireNonNull(maturityDate, "Maturity date must not be null");
            if (couponDates == null || couponDates.isEmpty()) {
                throw new IllegalArgumentException("Coupon dates must not be empty");
            }
            if (couponRate < 0) {
                throw new IllegalArgumentException("Coupon rate must not be negative");
            }
            if (faceValue <= 0) {
                throw new IllegalArgumentException("Face value must be positive");
            }
            if (marketPrice <= 0) {
                throw new IllegalArgumentException("Market price must be positive");
            }
        }
    }
}
