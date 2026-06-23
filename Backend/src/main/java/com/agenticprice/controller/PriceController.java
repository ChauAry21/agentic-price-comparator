package com.agenticprice.controller;

import com.agenticprice.agent.PriceComparisonAgent;
import com.agenticprice.api.FeatureExtractionRequest;
import com.agenticprice.api.FeatureExtractionResponse;
import com.agenticprice.api.PriceComparisonResponse;
import com.agenticprice.repository.SearchCacheRepository;
import com.agenticprice.service.FeatureExtractionService;
import com.agenticprice.service.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceComparisonAgent priceComparisonAgent;
    private final ScraperService scraperService;
    private final SearchCacheRepository searchCacheRepository;
    private final FeatureExtractionService featureExtractionService;

    @GetMapping("/search")
    public ResponseEntity<PriceComparisonResponse> search(@RequestParam String query) {
        return ResponseEntity.ok(priceComparisonAgent.compare(query));
    }

    @PostMapping("/scrape")
    public ResponseEntity<Void> scrapeAndSave(@RequestParam String query) {
        scraperService.scrapeAndSave(query);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Void> clearCache(@RequestParam String query) {
        searchCacheRepository.deleteByQueryIgnoreCase(query.trim().toLowerCase());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/extract-features")
    public ResponseEntity<FeatureExtractionResponse> extractFeatures(@RequestBody FeatureExtractionRequest request) {
        return ResponseEntity.ok(featureExtractionService.extract(request));
    }
}
