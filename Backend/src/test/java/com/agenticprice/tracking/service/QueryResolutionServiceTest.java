package com.agenticprice.tracking.service;

import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.OpenAIService;
import com.agenticprice.service.ScraperService;
import com.agenticprice.tracking.api.TrackingCandidateResponse;
import com.agenticprice.tracking.model.MatchMethod;
import com.agenticprice.tracking.model.TrackedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QueryResolutionServiceTest {

    private final ScraperService scraperService = mock(ScraperService.class);
    private final OpenAIService openAIService = mock(OpenAIService.class);
    private QueryResolutionService service;

    @BeforeEach
    void setUp() {
        service = new QueryResolutionService(scraperService, openAIService);
        when(openAIService.normalizeProductName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void clusterResults_dedupesSameAsin() {
        List<PriceResult> results = List.of(
                new PriceResult("Amazon", "iPhone 15", "$799.00", "USD", "https://amazon.com/dp/B123456789"),
                new PriceResult("Amazon", "iPhone duplicate", "$749.00", "USD", "https://amazon.com/dp/B123456789?tag=abc"),
                new PriceResult("Walmart", "iPhone 15", "$729.50", "USD", "https://walmart.com/ip/iphone-15")
        );
        when(scraperService.search("iphone")).thenReturn(results);

        TrackingCandidateResponse response = service.getCandidates("iphone");

        assertEquals(2, response.getCandidates().size());

        TrackingCandidateResponse.TrackingCandidate asinCluster = response.getCandidates().stream()
                .filter(candidate -> "B123456789".equals(candidate.getProductKey()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, asinCluster.getResultCount());
        assertEquals(new BigDecimal("749.00"), asinCluster.getLowestPrice());
        assertEquals("iPhone duplicate", asinCluster.getProductName());
        assertEquals(List.of("Amazon"), asinCluster.getRetailers());
    }

    @Test
    void getCandidates_autoSelectsWhenTopClusterSpansMultipleRetailers() {
        List<PriceResult> results = List.of(
                new PriceResult("Amazon", "Phone", "$100.00", "USD", "https://amazon.com/dp/B123456789"),
                new PriceResult("eBay", "Phone", "$110.00", "USD", "https://ebay.com/itm/B123456789"),
                new PriceResult("Walmart", "Other", "$50.00", "USD", "https://walmart.com/ip/other-product")
        );
        when(scraperService.search("phone")).thenReturn(results);

        TrackingCandidateResponse response = service.getCandidates("phone");

        assertTrue(response.isAutoSelected());
        assertEquals("B123456789", response.getCandidates().get(0).getProductKey());
        assertEquals(2, response.getCandidates().get(0).getRetailers().size());
    }

    @Test
    void getCandidates_doesNotAutoSelectAmbiguousClusters() {
        List<PriceResult> results = List.of(
                new PriceResult("Amazon", "Product A", "$100.00", "USD", "https://amazon.com/dp/AAAAAAAAAA"),
                new PriceResult("Walmart", "Product B", "$90.00", "USD", "https://walmart.com/ip/product-b")
        );
        when(scraperService.search("products")).thenReturn(results);

        TrackingCandidateResponse response = service.getCandidates("products");

        assertFalse(response.isAutoSelected());
        assertEquals(2, response.getCandidates().size());
    }

    @Test
    void matchResults_matchesByProductKey() {
        TrackedQuery tracked = trackedQuery("B123456789", "iPhone 15");
        List<PriceResult> results = List.of(
                new PriceResult("Amazon", "iPhone 15", "$799.00", "USD", "https://amazon.com/dp/B123456789"),
                new PriceResult("Walmart", "Random case", "$19.99", "USD", "https://walmart.com/ip/phone-case")
        );

        List<QueryResolutionService.MatchedResult> matched = service.matchResults(results, tracked);

        assertEquals(MatchMethod.KEY, matched.get(0).matchMethod());
        assertTrue(matched.get(0).matched());
        assertEquals(new BigDecimal("799.00"), matched.get(0).price());

        assertEquals(MatchMethod.NONE, matched.get(1).matchMethod());
        assertFalse(matched.get(1).matched());
    }

    @Test
    void matchResults_matchesByNormalizedNameWhenKeyDiffers() {
        TrackedQuery tracked = trackedQuery("B123456789", "iPhone 15");
        List<PriceResult> results = List.of(
                new PriceResult("Walmart", "Apple iPhone 15 128GB", "$729.50", "USD", "https://walmart.com/ip/iphone-15-walmart-listing")
        );
        when(openAIService.normalizeProductName("Apple iPhone 15 128GB")).thenReturn("iPhone 15 128GB");

        List<QueryResolutionService.MatchedResult> matched = service.matchResults(results, tracked);

        assertEquals(MatchMethod.NAME, matched.get(0).matchMethod());
        assertTrue(matched.get(0).matched());
        assertEquals(new BigDecimal("729.50"), matched.get(0).price());
    }

    @Test
    void matchResults_doesNotFalseMatchUnrelatedProduct() {
        TrackedQuery tracked = trackedQuery("B123456789", "iPhone 15");
        List<PriceResult> results = List.of(
                new PriceResult("Best Buy", "Samsung Galaxy S24", "$899.00", "USD", "https://bestbuy.com/site/samsung-galaxy-s24")
        );
        when(openAIService.normalizeProductName("Samsung Galaxy S24")).thenReturn("Samsung Galaxy S24");

        List<QueryResolutionService.MatchedResult> matched = service.matchResults(results, tracked);

        assertEquals(MatchMethod.NONE, matched.get(0).matchMethod());
        assertFalse(matched.get(0).matched());
        assertEquals(new BigDecimal("899.00"), matched.get(0).price());
    }

    private TrackedQuery trackedQuery(String canonicalProductKey, String canonicalProductName) {
        TrackedQuery tracked = new TrackedQuery();
        tracked.setCanonicalProductKey(canonicalProductKey);
        tracked.setCanonicalProductName(canonicalProductName);
        return tracked;
    }
}
