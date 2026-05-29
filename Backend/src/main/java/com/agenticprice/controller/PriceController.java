package com.agenticprice.controller;

import com.agenticprice.agent.PriceComparisonAgent;
import com.agenticprice.api.PriceComparisonResponse;
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

    @GetMapping("/search")
    public ResponseEntity<PriceComparisonResponse> search(@RequestParam String query) {
        return ResponseEntity.ok(priceComparisonAgent.compare(query));
    }

    @PostMapping("/scrape")
    public ResponseEntity<Void> scrapeAndSave(@RequestParam String query) {
        scraperService.scrapeAndSave(query);
        return ResponseEntity.accepted().build();
    }
}