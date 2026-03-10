package com.ice.portfolio.domain.repository;

import com.ice.portfolio.domain.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
}
