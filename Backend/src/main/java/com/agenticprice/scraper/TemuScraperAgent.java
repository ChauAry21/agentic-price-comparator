package com.agenticprice.scraper;

import com.agenticprice.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemuScraperAgent implements ScraperAgent {

    private final OpenAIService openAIService;
    private final PlaywrightService playwrightService;

    @Override
    public String getRetailerName() {
        return "Temu";
    }

    @Override
    public List<PriceResult> scrape(String productQuery) {
        try {
            String url = "https://www.temu.com/search_result.html?search_key=" + productQuery.replace(" ", "+");
            String html = playwrightService.fetchRenderedHtml(url);
            if (html.isBlank()) return List.of();

            Document doc = Jsoup.parse(html);
            Elements items = doc.select("div._29dBm1gx.autoFitGoodsList");

            log.info("Temu found {} potential product items", items.size());

            List<CompletableFuture<PriceResult>> futures = items.stream()
                    .limit(5)
                    .map(item -> CompletableFuture.<PriceResult>supplyAsync(() -> {
                        try {
                            String itemHtml = item.outerHtml();
                            com.agenticprice.util.PriceExtraction ext = com.agenticprice.util.PriceExtractionParser.parse(openAIService.extractPrice(itemHtml));
                            String productUrl = openAIService.extractProductUrl(itemHtml);
                            String title = item.select("h2 span").text();
                            if ((ext == null || ext.price() == null) || title.isBlank()) return null;
                            String fullUrl = productUrl.startsWith("/") ? "https://www.temu.com" + productUrl : productUrl;
                            return new PriceResult("Temu", title, ext.price().toPlainString(), "USD", fullUrl, ext != null && ext.financed());
                        } catch (Exception e) {
                            log.warn("Failed to process Temu item: {}", e.getMessage());
                            return null;
                        }
                    }))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Temu scrape failed for query '{}': {}", productQuery, e.getMessage());
            return List.of();
        }
    }
}