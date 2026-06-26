package com.agenticprice.tracking.service;

import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryResolutionService {

    private final ScraperService scraperService;

    public List<PriceResult> resolve(String rawQuery) {
        try {
            return scraperService.search(rawQuery);
        } catch (Exception e) {
            log.error("Failed to resolve query '{}': {}", rawQuery, e.getMessage());
            return List.of();
        }
    }
}
