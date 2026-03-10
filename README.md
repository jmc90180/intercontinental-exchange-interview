# Bond Portfolio Risk Analyzer

A Spring Boot application that models a portfolio of fixed-income instruments (plain vanilla bonds), calculates risk metrics (Yield to Maturity, Duration, Modified Duration), and exposes a REST API to query the results.

Built as part of the **ICE Senior Java Developer Assessment**.

---

## Quick Start

### Option 1: Docker Compose (Recommended)

The fastest way to get running. Requires only Docker.

```bash
docker compose up --build
```

This starts:
- **PostgreSQL 16** on port 5432
- **Spring Boot app** on port 8080

Once running, open the interactive API explorer:

> **http://localhost:8080/swagger-ui.html**

To stop:
```bash
docker compose down
```

### Option 2: Standalone (with local PostgreSQL)

Requires Java 17+ and a running PostgreSQL instance.

1. Create the database:
```sql
CREATE DATABASE bond_portfolio;
CREATE USER bond_user WITH PASSWORD 'bond_pass';
GRANT ALL PRIVILEGES ON DATABASE bond_portfolio TO bond_user;
```

2. Run the application:
```bash
./mvnw spring-boot:run
```

### Option 3: Standalone (with Docker PostgreSQL only)

Run just the database in Docker, and the app locally:

```bash
docker compose up postgres -d
./mvnw spring-boot:run
```

---

## Running Tests

Tests use an embedded H2 database — no Docker or PostgreSQL needed.

```bash
./mvnw test
```

**Test suite (31 tests):**

| Test Class | Count | What It Covers |
|-----------|-------|----------------|
| `BondTest` | 11 | Builder validation, derived methods, immutability |
| `YieldToMaturityCalculatorTest` | 5 | Newton-Raphson YTM: at-par, premium, discount, annual, edge cases |
| `DurationCalculatorTest` | 7 | Macaulay/Modified Duration against textbook values |
| `PortfolioApiIntegrationTest` | 8 | Full REST API lifecycle, risk metric accuracy, error handling |

Integration tests use a **fixed clock** (2026-01-01) for deterministic, non-flaky results.

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

### Create Portfolio

```
POST /api/v1/portfolios
```

Load bonds from JSON and create a new portfolio.

**Request body:**
```json
{
  "name": "US Treasury Portfolio",
  "bonds": [
    {
      "isin": "US912828ZT58",
      "maturityDate": "2029-01-01",
      "couponDates": ["2026-07-01", "2027-01-01", "2027-07-01", "2028-01-01", "2028-07-01", "2029-01-01"],
      "couponRate": 0.05,
      "faceValue": 1000.0,
      "marketPrice": 980.25
    },
    {
      "isin": "US912828AB12",
      "maturityDate": "2031-01-01",
      "couponDates": ["2026-07-01", "2027-01-01", "2027-07-01", "2028-01-01", "2028-07-01", "2029-01-01", "2029-07-01", "2030-01-01", "2030-07-01", "2031-01-01"],
      "couponRate": 0.06,
      "faceValue": 1000.0,
      "marketPrice": 1020.50
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "id": "32936dae-7b10-4cf9-b633-87ab091fe7dd",
  "name": "US Treasury Portfolio",
  "bondCount": 2,
  "totalMarketValue": 2000.75
}
```

### Get Portfolio

```
GET /api/v1/portfolios/{id}
```

### List Bonds

```
GET /api/v1/portfolios/{id}/bonds
```

### Get Bond

```
GET /api/v1/portfolios/{id}/bonds/{isin}
```

### Bond Risk Metrics

```
GET /api/v1/portfolios/{id}/bonds/{isin}/risk-metrics
```

**Response (200 OK):**
```json
{
  "isin": "US912828ZT58",
  "yieldToMaturity": 0.0573,
  "macaulayDuration": 2.82,
  "modifiedDuration": 2.74
}
```

### Portfolio Risk Metrics

```
GET /api/v1/portfolios/{id}/risk-metrics
```

**Response (200 OK):**
```json
{
  "portfolioId": "32936dae-7b10-4cf9-b633-87ab091fe7dd",
  "portfolioName": "US Treasury Portfolio",
  "weightedAverageDuration": 2.99,
  "bondMetrics": [
    {
      "isin": "US912828ZT58",
      "weight": 0.4901,
      "yieldToMaturity": 0.0573,
      "macaulayDuration": 2.82,
      "modifiedDuration": 2.74
    },
    {
      "isin": "US912828AB12",
      "weight": 0.5099,
      "yieldToMaturity": 0.0553,
      "macaulayDuration": 4.40,
      "modifiedDuration": 4.28
    }
  ]
}
```

### Error Responses

All errors return a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Portfolio not found with id: xyz",
  "timestamp": "2026-03-09T22:30:00Z"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Invalid input (bad JSON, negative face value, etc.) |
| 404 | Portfolio or bond not found |
| 500 | Calculation error (e.g., YTM non-convergence) |

---

## Sample curl Commands

```bash
# Create a portfolio from the sample file
curl -X POST http://localhost:8080/api/v1/portfolios \
  -H "Content-Type: application/json" \
  -d @src/test/resources/sample-portfolio.json

# Get portfolio details (replace {id} with actual ID from create response)
curl http://localhost:8080/api/v1/portfolios/{id}

# List all bonds in the portfolio
curl http://localhost:8080/api/v1/portfolios/{id}/bonds

# Get risk metrics for a specific bond
curl http://localhost:8080/api/v1/portfolios/{id}/bonds/US912828ZT58/risk-metrics

# Get portfolio-level risk metrics (weighted average duration)
curl http://localhost:8080/api/v1/portfolios/{id}/risk-metrics
```

---

## Architecture

```
com.ice.portfolio
├── BondPortfolioApplication.java          Entry point
├── domain/
│   ├── model/
│   │   ├── Bond.java                      JPA entity, Builder pattern
│   │   └── Portfolio.java                 JPA entity, bond collection
│   ├── repository/
│   │   └── PortfolioRepository.java       Spring Data JPA interface
│   └── exception/
│       ├── BondNotFoundException.java
│       └── PortfolioNotFoundException.java
├── service/
│   ├── RiskMetricsService.java            Orchestrates calculations
│   └── calculator/
│       ├── YieldToMaturityCalculator.java  Newton-Raphson YTM solver
│       ├── DurationCalculator.java         Macaulay + Modified Duration
│       └── PortfolioRiskCalculator.java    Weighted average duration
├── api/
│   ├── controller/
│   │   └── PortfolioController.java       REST endpoints + Swagger
│   ├── dto/                               Java 17 records
│   ├── mapper/
│   │   └── BondMapper.java                DTO ↔ Domain conversion
│   └── exception/
│       └── GlobalExceptionHandler.java    @ControllerAdvice
└── config/
    └── AppConfig.java                     Clock bean for testability
```

**Dependency direction:** `api` → `service` → `domain`

---

## Design Decisions

### Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Builder** | `Bond.Builder` | 6 fields with validation; readable construction |
| **Strategy** | `calculator/` package | Each risk metric is a distinct algorithm; extensible to new metrics |
| **Repository** | `PortfolioRepository` | Abstracted persistence via Spring Data JPA |
| **DTO/Mapper** | `api/dto/` + `api/mapper/` | Decouples API contract from JPA entity structure |
| **Service Layer** | `RiskMetricsService` | Thin controllers; business logic in services |

### Key Choices

- **Java 17** — LTS release, widely adopted in enterprise environments
- **No Lombok** — Hand-written Builder demonstrates Java proficiency; no hidden magic
- **Java 17 records for DTOs** — Concise, immutable, and shows modern Java knowledge
- **PostgreSQL + Docker Compose** — Durable persistence; `docker compose up` gives reviewers a one-command setup
- **H2 for tests** — Fast test execution without Docker; tests run anywhere with just Java
- **Injected `Clock` bean** — Deterministic date handling in tests; no flaky temporal behavior
- **JPA annotations on domain model** — Pragmatic for this scope; a larger system would separate persistence entities from domain objects
- **Versioned API paths (`/api/v1/`)** — Demonstrates API evolution awareness
- **SpringDoc OpenAPI** — Interactive Swagger UI at `/swagger-ui.html` for easy API exploration

---

## Financial Calculations

### Yield to Maturity (Newton-Raphson)

Finds the discount rate `y` that equates present value of all cash flows to market price:

```
P = Σ(C / (1+y)^t) + F / (1+y)^n
```

Solved iteratively using Newton-Raphson with initial guess `(C + (F-P)/n) / ((F+P)/2)`.

### Macaulay Duration

Weighted average time to receive cash flows:

```
D_mac = (1/P) × Σ(timeInYears_t × PV(CF_t))
```

### Modified Duration

Price sensitivity to yield changes:

```
D_mod = D_mac / (1 + ytm / periodsPerYear)
```

### Portfolio Weighted Average Duration

```
D_portfolio = Σ(w_i × D_mod_i)    where w_i = marketPrice_i / totalMarketValue
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.4.3 |
| Build | Maven (via Maven Wrapper) |
| Database | PostgreSQL 16 (Docker) / H2 (tests) |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5, AssertJ, MockMvc |
| Containerization | Docker, Docker Compose |

---

## Project Structure

```
.
├── pom.xml                          Maven build configuration
├── Dockerfile                       Multi-stage build
├── docker-compose.yml               PostgreSQL + app
├── mvnw / .mvn/                     Maven Wrapper (no Maven install needed)
├── src/
│   ├── main/
│   │   ├── java/com/ice/portfolio/  Application source code
│   │   └── resources/
│   │       └── application.yml      Production config (PostgreSQL)
│   └── test/
│       ├── java/com/ice/portfolio/  Test source code
│       └── resources/
│           ├── application-test.yml Test config (H2)
│           └── sample-portfolio.json Sample input data
├── bond-test-data.md                Test data reference (sources & verified values)
└── README.md                        This file
```
