package com.agenticprice.controller;

import com.agenticprice.agent.PriceComparisonAgent;
import com.agenticprice.api.PriceComparisonResponse;
import com.agenticprice.repository.SearchCacheRepository;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class PriceController {

    private final PriceComparisonAgent priceComparisonAgent;
    private final ScraperService scraperService;
    private final SearchCacheRepository searchCacheRepository;

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
}