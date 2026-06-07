package com.agenticprice.agent;

import com.agenticprice.api.PriceComparisonResponse;
import com.agenticprice.repository.SearchCacheRepository;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.ScraperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceComparisonAgentTest {

    private final ScraperService scraperService = mock(ScraperService.class);
    private final SearchCacheRepository searchCacheRepository = mock(SearchCacheRepository.class);
    private final PriceComparisonAgent agent = new PriceComparisonAgent(
            scraperService,
            searchCacheRepository,
            new ObjectMapper()
    );

    @Test
    void compareDedupesResultsAndCalculatesPriceMetrics() {
        List<PriceResult> results = List.of(
                new PriceResult("Amazon", "iPhone 15", "$799.00", "USD", "https://amazon.com/dp/B123456789"),
                new PriceResult("Amazon", "iPhone duplicate", "$749.00", "USD", "https://amazon.com/dp/B123456789?tag=abc"),
                new PriceResult("Walmart", "iPhone 15", "$729.50", "USD", "https://walmart.com/ip/iphone-15"),
                new PriceResult("Newegg", "iPhone 15", "", "USD", "https://newegg.com/p/iphone-15")
        );

        when(searchCacheRepository.findByQueryIgnoreCase("iphone")).thenReturn(Optional.empty());
        when(searchCacheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(scraperService.search("iphone")).thenReturn(results);
        when(scraperService.getRetailerNames()).thenReturn(List.of("Amazon", "Walmart", "Newegg"));

        PriceComparisonResponse response = agent.compare("iphone");

        assertEquals(2, response.getResultCount());
        assertEquals(List.of("Amazon", "Walmart"), response.getRetailersWithResults());
        assertEquals("Walmart", response.getBestRetailer());
        assertEquals(new BigDecimal("729.50"), response.getLowestPrice());
        assertEquals(new BigDecimal("799.00"), response.getHighestPrice());
        assertEquals(new BigDecimal("764.25"), response.getAveragePrice());
        assertEquals(new BigDecimal("69.50"), response.getPotentialSavings());
    }

    @Test
    void responseSupportsLegacyRetailerWithResultsCacheField() throws Exception {
        String cachedJson = """
                {
                  "query": "iphone",
                  "resultCount": 1,
                  "retailersQueried": ["Amazon"],
                  "retailerWithResults": ["Amazon"],
                  "bestRetailer": "Amazon",
                  "lowestPrice": 799.00,
                  "highestPrice": 799.00,
                  "averagePrice": 799.00,
                  "potentialSavings": 0,
                  "results": []
                }
                """;

        PriceComparisonResponse response = new ObjectMapper()
                .readValue(cachedJson, PriceComparisonResponse.class);

        assertEquals(List.of("Amazon"), response.getRetailersWithResults());
    }
}
