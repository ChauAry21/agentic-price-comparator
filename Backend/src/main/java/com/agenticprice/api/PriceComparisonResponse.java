package com.agenticprice.api;

import com.agenticprice.scraper.PriceResult;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PriceComparisonResponse {
    private String query;
    private int resultCount;
    private List<String> retailersQueried;
    private List<String> retailersWithResults;
    private List<PriceResult> results;
}