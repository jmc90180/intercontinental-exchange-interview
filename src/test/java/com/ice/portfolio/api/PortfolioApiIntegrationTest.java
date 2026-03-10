package com.ice.portfolio.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ice.portfolio.api.dto.PortfolioRequest;
import com.ice.portfolio.api.dto.BondRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests that exercise the REST API and verify financial calculation results.
 * Uses H2 in-memory database and a fixed clock for deterministic results.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioApiIntegrationTest {

    private static final LocalDate FIXED_DATE = LocalDate.of(2026, 1, 1);

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock fixedClock() {
            return Clock.fixed(
                    FIXED_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Full lifecycle: POST portfolio -> GET portfolio -> GET bond -> GET risk metrics.
     */
    @Test
    void shouldCreateAndRetrievePortfolioWithRiskMetrics() throws Exception {
        PortfolioRequest request = buildTestPortfolioRequest();

        // Create portfolio
        MvcResult createResult = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("Test Portfolio")))
                .andExpect(jsonPath("$.bondCount", is(2)))
                .andReturn();

        String portfolioId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asText();

        // Get portfolio
        mockMvc.perform(get("/api/v1/portfolios/{id}", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Portfolio")))
                .andExpect(jsonPath("$.bondCount", is(2)));

        // List bonds
        mockMvc.perform(get("/api/v1/portfolios/{id}/bonds", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // Get specific bond
        mockMvc.perform(get("/api/v1/portfolios/{id}/bonds/{isin}", portfolioId, "BOND_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isin", is("BOND_A")));

        // Get bond risk metrics
        mockMvc.perform(get("/api/v1/portfolios/{id}/bonds/{isin}/risk-metrics", portfolioId, "BOND_A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isin", is("BOND_A")))
                .andExpect(jsonPath("$.yieldToMaturity").isNumber())
                .andExpect(jsonPath("$.macaulayDuration").isNumber())
                .andExpect(jsonPath("$.modifiedDuration").isNumber());
    }

    /**
     * At-par bond: YTM should approximately equal coupon rate.
     * Test Case 7 from bond-test-data.md.
     */
    @Test
    void shouldReturnCouponRateAsYtmForAtParBond() throws Exception {
        List<LocalDate> couponDates = generateSemiAnnualDates(FIXED_DATE, 6);

        BondRequest atParBond = new BondRequest(
                "AT_PAR_001", couponDates.get(couponDates.size() - 1),
                couponDates, 0.06, 100.0, 100.0);

        PortfolioRequest request = new PortfolioRequest("At Par Portfolio", List.of(atParBond));

        MvcResult result = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String portfolioId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText();

        MvcResult metricsResult = mockMvc.perform(get("/api/v1/portfolios/{id}/bonds/{isin}/risk-metrics",
                        portfolioId, "AT_PAR_001"))
                .andExpect(status().isOk())
                .andReturn();

        double ytm = objectMapper.readTree(
                metricsResult.getResponse().getContentAsString()).get("yieldToMaturity").asDouble();
        assertThat(ytm).isCloseTo(0.06, org.assertj.core.data.Offset.offset(0.001));
    }

    /**
     * Verify portfolio-level weighted average duration.
     */
    @Test
    void shouldCalculatePortfolioWeightedDuration() throws Exception {
        PortfolioRequest request = buildTestPortfolioRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String portfolioId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/portfolios/{id}/risk-metrics", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId", is(portfolioId)))
                .andExpect(jsonPath("$.weightedAverageDuration").isNumber())
                .andExpect(jsonPath("$.bondMetrics", hasSize(2)))
                .andExpect(jsonPath("$.bondMetrics[0].isin").isString())
                .andExpect(jsonPath("$.bondMetrics[0].weight").isNumber())
                .andExpect(jsonPath("$.bondMetrics[0].yieldToMaturity").isNumber())
                .andExpect(jsonPath("$.bondMetrics[0].macaulayDuration").isNumber())
                .andExpect(jsonPath("$.bondMetrics[0].modifiedDuration").isNumber());
    }

    /**
     * Premium bond: YTM should be less than coupon rate.
     */
    @Test
    void shouldReturnLowerYtmForPremiumBond() throws Exception {
        List<LocalDate> couponDates = generateSemiAnnualDates(FIXED_DATE, 20);

        BondRequest premiumBond = new BondRequest(
                "PREMIUM_001", couponDates.get(couponDates.size() - 1),
                couponDates, 0.06, 1000.0, 1050.0);

        PortfolioRequest request = new PortfolioRequest("Premium Portfolio", List.of(premiumBond));

        MvcResult result = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String portfolioId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText();

        MvcResult metricsResult = mockMvc.perform(get("/api/v1/portfolios/{id}/bonds/{isin}/risk-metrics",
                        portfolioId, "PREMIUM_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modifiedDuration").isNumber())
                .andReturn();

        // Extract and verify YTM manually to avoid Hamcrest/BigDecimal type mismatch
        double ytm = objectMapper.readTree(
                metricsResult.getResponse().getContentAsString()).get("yieldToMaturity").asDouble();
        // Premium bond: YTM < coupon rate (0.06)
        assertThat(ytm).isLessThan(0.06);
        assertThat(ytm).isCloseTo(0.054, org.assertj.core.data.Offset.offset(0.008));
    }

    /**
     * 404 for non-existent portfolio.
     */
    @Test
    void shouldReturn404ForMissingPortfolio() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    /**
     * 404 for non-existent bond ISIN.
     */
    @Test
    void shouldReturn404ForMissingBond() throws Exception {
        PortfolioRequest request = buildTestPortfolioRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String portfolioId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/portfolios/{id}/bonds/{isin}", portfolioId, "NONEXISTENT"))
                .andExpect(status().isNotFound());
    }

    /**
     * 400 for invalid input (missing required fields).
     */
    @Test
    void shouldReturn400ForInvalidInput() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "bonds": []
                }
                """;

        mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * Single remaining coupon edge case.
     */
    @Test
    void shouldHandleBondWithSingleCouponRemaining() throws Exception {
        List<LocalDate> couponDates = List.of(LocalDate.of(2026, 7, 1));

        BondRequest singleCoupon = new BondRequest(
                "SINGLE_001", LocalDate.of(2026, 7, 1),
                couponDates, 0.05, 1000.0, 995.0);

        PortfolioRequest request = new PortfolioRequest("Single Coupon Portfolio", List.of(singleCoupon));

        MvcResult result = mockMvc.perform(post("/api/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String portfolioId = objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/portfolios/{id}/bonds/{isin}/risk-metrics",
                        portfolioId, "SINGLE_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yieldToMaturity").isNumber())
                .andExpect(jsonPath("$.macaulayDuration").isNumber());
    }

    // --- Helper methods ---

    private PortfolioRequest buildTestPortfolioRequest() {
        List<LocalDate> semiAnnual6 = generateSemiAnnualDates(FIXED_DATE, 6);
        List<LocalDate> semiAnnual10 = generateSemiAnnualDates(FIXED_DATE, 10);

        BondRequest bondA = new BondRequest(
                "BOND_A", semiAnnual6.get(semiAnnual6.size() - 1),
                semiAnnual6, 0.05, 1000.0, 980.0);

        BondRequest bondB = new BondRequest(
                "BOND_B", semiAnnual10.get(semiAnnual10.size() - 1),
                semiAnnual10, 0.06, 1000.0, 1020.0);

        return new PortfolioRequest("Test Portfolio", List.of(bondA, bondB));
    }

    private List<LocalDate> generateSemiAnnualDates(LocalDate start, int periods) {
        return IntStream.rangeClosed(1, periods)
                .mapToObj(i -> start.plusMonths(6L * i))
                .toList();
    }
}
