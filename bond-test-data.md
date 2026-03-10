# Bond Test Data & Verification Sources

Reference file for validating the Bond Portfolio Risk Analyzer's financial calculations.

---

## 1. Pre-Verified Test Cases (Textbook-Grade)

These can be hardcoded directly as unit/integration test fixtures with known expected values.

### Test Case 1: Annual Coupon - Discount Bond
- **Source**: [AnalystPrep CFA Level 1](https://analystprep.com/cfa-level-1-exam/fixed-income/macaulay-modified-effective-durations/)
- **Parameters**: Coupon = 6% annual, Maturity = 3 years, YTM = 8%, Par = 100
- **Expected**:
  - Price = 94.85
  - Macaulay Duration = 2.82 years

### Test Case 2: Annual Coupon - Different Spread
- **Source**: [AnalystPrep CFA Level 1](https://analystprep.com/cfa-level-1-exam/fixed-income/macaulay-modified-effective-durations/)
- **Parameters**: Coupon = 5% annual, Maturity = 3 years, YTM = 7.5%, Par = 100
- **Expected**:
  - Price = 93.50
  - Macaulay Duration = 2.85 years

### Test Case 3: Annual Coupon - Approximate Duration Verification
- **Source**: [AnalystPrep CFA Level 1](https://analystprep.com/cfa-level-1-exam/fixed-income/macaulay-modified-effective-durations/)
- **Parameters**: Coupon = 5% annual, Maturity = 3 years, YTM = 4%, Par = 100
- **Expected**:
  - Price = 102.775
  - Approximate Modified Duration = 2.761
  - Approximate Macaulay Duration = 2.87 years

### Test Case 4: 5-Year Annual Coupon
- **Source**: [AnalystPrep CFA Level 1](https://analystprep.com/cfa-level-1-exam/fixed-income/macaulay-modified-effective-durations/)
- **Parameters**: 5-year bond, 8% annual coupon
- **Expected**:
  - Macaulay Duration = 4.247 years
  - Modified Duration = 3.786
  - Interpretation: 1% yield change -> ~3.786% inverse price change

### Test Case 5: Semi-Annual Coupon - High Coupon Premium Bond
- **Source**: [Bionic Turtle](https://www.bionicturtle.com/modified-duration/)
- **Parameters**: Face = $100, Maturity = 3 years, Coupon = 10% semi-annual, YTM = 4%
- **Expected**:
  - Price = $116.80
  - Macaulay Duration = 2.693 years
  - Modified Duration = 2.641 years

### Test Case 6: Zero-Coupon Bond
- **Source**: [Bionic Turtle](https://www.bionicturtle.com/modified-duration/)
- **Parameters**: Face = $100, Maturity = 3 years, Coupon = 0%, YTM = 4% (semi-annual compounding)
- **Expected**:
  - Macaulay Duration = 3.000 years (always equals maturity for zero-coupon)
  - Modified Duration = 2.941 years
- **Note**: Our app handles fixed-coupon bonds only, but this is a useful sanity check

### Test Case 7: Semi-Annual Coupon - At Par
- **Source**: [thismatter.com](https://thismatter.com/money/bonds/duration-convexity.htm)
- **Parameters**: Par = $100, Coupon = 6% semi-annual, Maturity = 3 years, YTM = 6%
- **Expected**:
  - Price = $100.00 (at par - YTM equals coupon rate)
  - Macaulay Duration = 2.79 years
- **Key validation**: When price == face value, YTM should equal coupon rate

### Test Case 8: Semi-Annual Coupon - 10-Year Discount
- **Source**: [thismatter.com](https://thismatter.com/money/bonds/duration-convexity.htm)
- **Parameters**: Coupon = 6% semi-annual, Maturity = 10 years, YTM = 8%
- **Expected**:
  - Macaulay Duration = 7.45 years
  - Modified Duration = 7.16 years

### Test Case 9: Semi-Annual - Discount with Known YTM
- **Source**: [DQYDJ Bond Duration Calculator](https://dqydj.com/bond-duration-calculator/)
- **Parameters**: Par = $1,000, Coupon = 5%, Price = $960.27, YTM = 6.5%, Maturity = 3 years
- **Expected**:
  - Macaulay Duration = 2.856 years
  - Modified Duration = 2.682 years

### Test Case 10: YTM from Price - Premium Bond
- **Source**: [Wall Street Prep](https://www.wallstreetprep.com/knowledge/yield-to-maturity-ytm/)
- **Parameters**: Face = $1,000, Coupon = 6% semi-annual, Maturity = 10 years, Price = $1,050
- **Expected**:
  - YTM = ~5.4% (annualized)
- **Use**: Validates Newton-Raphson YTM solver on a premium bond

### Test Case 11: YTM from Price - Discount Bond
- **Source**: [FINRA](https://www.finra.org/investors/insights/bond-yield-return)
- **Parameters**: Face = $1,000, Coupon = 4.2% semi-annual, Maturity = 3 years, Price = $965
- **Expected**:
  - Semi-annual coupon = $21
  - YTM = ~5.48% (annualized)
- **Use**: Validates Newton-Raphson YTM solver on a discount bond

### Test Case 12: Zero-Coupon Bond YTM
- **Source**: [Wall Street Prep](https://www.wallstreetprep.com/knowledge/zero-coupon-bond/)
- **Parameters**: Face = $1,000, Price = $600, Maturity = 10 years
- **Expected**:
  - YTM = ~5.19%
- **Note**: Edge case for our app (no periodic coupons)

### Test Case 13: Portfolio Duration - Multi-Bond
- **Source**: [AnalystPrep CFA Level 1](https://analystprep.com/cfa-level-1-exam/fixed-income/duration-and-convexity-of-a-bond-portfolio/)
- **Bond A**: 8% annual coupon, 1-year maturity, YTM = 20%, Price = 90 per 100 par
- **Bond B**: 5% annual coupon, 2-year maturity, YTM = 12%, Price = 88.17 per 100 par
- **Use**: Calculate individual Modified Durations, then portfolio weighted average duration using market value weights
- **Key validation**: Weights must sum to 1.0; portfolio duration should be between the min and max individual durations

### Test Case 14: Semi-Annual Bond Price from YTM
- **Source**: [AnalystPrep](https://analystprep.com/cfa-level-1-exam/fixed-income/bond-price-calculation-based-on-ytm/)
- **Parameters**: Coupon = 2.75% annual (semi-annual payments), Maturity = 5 years, YTM = 3.5%, Par = 100
- **Expected**:
  - Price = 96.587 per 100 par
- **Use**: Validates bond pricing (inverse of YTM calculation)

---

## 2. Sanity Check Rules

Use these invariants to catch bugs regardless of specific test data:

| Rule | Description |
|------|-------------|
| **At-par bond** | If marketPrice == faceValue, then YTM ~= couponRate |
| **Premium bond** | If marketPrice > faceValue, then YTM < couponRate |
| **Discount bond** | If marketPrice < faceValue, then YTM > couponRate |
| **Zero-coupon duration** | Macaulay Duration == time to maturity |
| **ModD < MacD** | Modified Duration is always less than Macaulay Duration (when YTM > 0) |
| **Duration < Maturity** | Macaulay Duration is always <= maturity for coupon-bearing bonds |
| **Portfolio weights** | Bond weights in portfolio must sum to 1.0 |
| **Portfolio duration bounds** | Portfolio duration must be between min and max individual bond durations |
| **YTM positive** | YTM should be positive for standard bonds (guard against Newton-Raphson divergence) |

---

## 3. Real Bond Data Sources

### US Treasury Fiscal Data API (FREE, no auth)
- **Endpoint**: `https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/auctions_query`
- **Example** (5 most recent auctions):
  ```
  https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/auctions_query?sort=-auction_date&page[size]=5&format=json
  ```
- **Fields**: cusip, security_type, security_term, interest_rate, maturity_date, issue_date
- **Limitation**: Provides auction/issuance data, not live secondary market prices

### TreasuryDirect Web API (FREE)
- **Lookup by CUSIP**: `http://www.treasurydirect.gov/TA_WS/securities/{CUSIP}/{issueDate}?format=json`
- **Fields**: cusip, issueDate, securityType, maturityDate, interestRate

### FRED - Federal Reserve Economic Data (FREE)
- **Daily Treasury Yields**: https://fred.stlouisfed.org/series/DGS10
- **Series IDs**: `DGS1`, `DGS2`, `DGS3`, `DGS5`, `DGS7`, `DGS10`, `DGS20`, `DGS30`
- **Use**: Derive current market prices for bonds using the yield curve

### Finnhub API (FREE tier with rate limits)
- **Docs**: https://finnhub.io/docs/api/bond-price
- **Features**: Bond profile lookup by FIGI/ISIN/CUSIP, bond price data
- **Requires**: Free API key registration

---

## 4. Online Calculators (Cross-Validation)

Use these to verify our implementation against known-good calculators:

| Calculator | URL | Best For |
|-----------|-----|----------|
| **DQYDJ YTM** | https://dqydj.com/bond-yield-to-maturity-calculator/ | YTM from price |
| **DQYDJ Duration** | https://dqydj.com/bond-duration-calculator/ | Macaulay & Modified Duration |
| **DQYDJ Convexity** | https://dqydj.com/bond-convexity-calculator/ | Convexity (if we extend) |
| **Calculator.net** | https://www.calculator.net/bond-calculator.html | Quick solve: input 4 of 5 fields |
| **OmniCalculator** | https://www.omnicalculator.com/finance/bond-price | Bond price calculations |

---

## 5. Academic & CFA References

For understanding the underlying formulas:

| Resource | URL |
|----------|-----|
| **CFA Institute: Yield-Based Duration** | https://www.cfainstitute.org/insights/professional-learning/refresher-readings/2026/yield-based-bond-duration-measures-and-properties |
| **AnalystPrep: Duration Types** | https://analystprep.com/cfa-level-1-exam/fixed-income/macaulay-modified-effective-durations/ |
| **AnalystPrep: Bond Pricing** | https://analystprep.com/cfa-level-1-exam/fixed-income/bond-price-calculation-based-on-ytm/ |
| **AnalystPrep: Portfolio Duration** | https://analystprep.com/cfa-level-1-exam/fixed-income/duration-and-convexity-of-a-bond-portfolio/ |
| **NYU Stern: Duration (PDF)** | https://pages.stern.nyu.edu/~jcarpen0/courses/b403333/04duration.pdf |
| **Bionic Turtle: Modified Duration** | https://www.bionicturtle.com/modified-duration/ |
| **BlackRock: Understanding Duration (PDF)** | https://www.blackrock.com/fp/documents/understanding_duration.pdf |
| **FINRA: Bond Yield and Return** | https://www.finra.org/investors/insights/bond-yield-return |

---

## 6. Recommended Test Strategy

1. **Unit tests** (Phase 3 of plan): Hardcode test cases 1-12 as fixtures in `YieldToMaturityCalculatorTest.java` and `DurationCalculatorTest.java`. Use `assertThat(value).isCloseTo(expected, within(0.01))` for floating-point tolerance.

2. **Portfolio unit tests** (Phase 3): Use test case 13 for `PortfolioRiskCalculator` validation.

3. **Integration tests** (Phase 7): Build a portfolio JSON payload using 3-4 bonds from the test cases above. POST to the API, then GET risk metrics and verify against known values.

4. **Cross-validation**: After implementation, manually input our test bond parameters into the DQYDJ calculators and compare outputs. Document any discrepancies.

5. **Smoke test with real data**: Use the Treasury Fiscal Data API to pull 2-3 real US Treasury bonds, construct a portfolio, and verify the results are in reasonable ranges.
