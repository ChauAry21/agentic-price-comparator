package com.agenticprice.scraper;

import java.util.List;

public interface ScraperAgent {
    String getRetailerName();
    List<PriceResult> scrape(String productQuery);
}