package com.ice.portfolio.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a portfolio containing a collection of bonds.
 */
@Entity
@Table(name = "portfolios")
public class Portfolio {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id")
    private List<Bond> bonds = new ArrayList<>();

    protected Portfolio() {
        // Required by JPA
    }

    public Portfolio(String name, List<Bond> bonds) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.bonds = new ArrayList<>(bonds);
    }

    @PrePersist
    private void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Bond> getBonds() {
        return Collections.unmodifiableList(bonds);
    }

    /**
     * Total market value of the portfolio (sum of all bond market prices).
     */
    public double totalMarketValue() {
        return bonds.stream()
                .mapToDouble(Bond::getMarketPrice)
                .sum();
    }

    /**
     * Market value weight of a bond within this portfolio.
     */
    public double bondWeight(Bond bond) {
        double total = totalMarketValue();
        if (total == 0) {
            return 0;
        }
        return bond.getMarketPrice() / total;
    }

    @Override
    public String toString() {
        return "Portfolio{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", bondCount=" + bonds.size() +
                '}';
    }
}
