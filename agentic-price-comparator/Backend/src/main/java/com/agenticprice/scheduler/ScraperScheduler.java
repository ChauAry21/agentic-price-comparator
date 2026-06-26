package com.agenticprice.scheduler;

import com.agenticprice.repository.SearchCacheRepository;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.PriceAlertService;
import com.agenticprice.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperScheduler {

    private final ScraperService scraperService;
    private final PriceAlertService priceAlertService;
    private final SearchCacheRepository searchCacheRepository;

    @Value("${scraper.queries:laptop,headphones,keyboard}")
    private String defaultQueries;

    @Scheduled(cron = "${SCRAPER_CRON:0 0 0/6 * * *}")
    public void runScheduledScrape() {
        log.info("Running scheduled scrape...");
        List<String> queries = List.of(defaultQueries.split(","));
        for (String query : queries) {
            try {
                List<PriceResult> results = scraperService.search(query.trim());
                priceAlertService.checkAlerts(results);
                // scrapeAndSave persists snapshots; search() already ran the scrapers,
                // so we call the save path only (no re-scrape).
                scraperService.saveResults(query.trim(), results);
            } catch (Exception e) {
                log.error("Scheduled scrape failed for query '{}': {}", query, e.getMessage());
            }
        }
        log.info("Scheduled scrape complete.");
    }

    // Evict cache entries older than 24 hours — runs daily at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void evictStaleCache() {
        searchCacheRepository.deleteByCachedAtBefore(OffsetDateTime.now().minusHours(24));
        log.info("Stale search cache evicted.");
    }
}