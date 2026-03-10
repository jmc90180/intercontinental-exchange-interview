package com.ice.portfolio.domain.exception;

public class BondNotFoundException extends RuntimeException {

    public BondNotFoundException(String isin) {
        super("Bond not found with ISIN: " + isin);
    }
}
