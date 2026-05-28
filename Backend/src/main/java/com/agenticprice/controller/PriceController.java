package com.agenticprice.controller;

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

    private final ScraperService scraperService;

    @GetMapping("/search")
    public ResponseEntity<List<PriceResult>> search(@RequestParam String query) {
        List<PriceResult> results = scraperService.search(query);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/scrape")
    public ResponseEntity<Void> scrapeAndSave(@RequestParam String query) {
        scraperService.scrapeAndSave(query);
        return ResponseEntity.accepted().build();
    }
}