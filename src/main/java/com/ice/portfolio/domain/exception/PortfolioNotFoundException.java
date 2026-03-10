package com.ice.portfolio.domain.exception;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String id) {
        super("Portfolio not found with id: " + id);
    }
}
