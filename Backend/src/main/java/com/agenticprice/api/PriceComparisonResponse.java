package com.agenticprice.api;

import com.agenticprice.scraper.PriceResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceComparisonResponse {
    private String query;
    private int resultCount;
    private List<String> retailersQueried;
    private List<String> retailerWithResults;
    private String bestRetailer;
    private BigDecimal lowestPrice;
    private BigDecimal highestPrice;
    private BigDecimal averagePrice;
    private BigDecimal potentialSavings;
    private List<PriceResult> results;
}
