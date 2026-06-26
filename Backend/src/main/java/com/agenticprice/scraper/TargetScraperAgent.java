package com.agenticprice.service;

import com.agenticprice.service.OpenAIService;
import com.agenticprice.service.PlaywrightService;
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
public class TargetScraperAgent implements ScraperAgent {

    private final OpenAIService openAIService;
    private final PlaywrightService playwrightService;

    @Override
    public String getRetailerName() {
        return "Target";
    }

    @Override
    public List<PriceResult> scrape(String productQuery) {
        try {
            String url = "https://www.target.com/s?searchTerm=" + productQuery.replace(" ", "+");
            String html = playwrightService.fetchRenderedHtml(url);
            if (html.isBlank()) return List.of();

            Document doc = Jsoup.parse(html);
            Elements items = doc.select("div[data-test='@web/site-top-of-funnel/ProductCardWrapper']");
            if (items.isEmpty()) items = doc.select("article");

            log.info("Target found {} potential product items", items.size());

            List<CompletableFuture<PriceResult>> futures = items.stream()
                    .limit(5)
                    .map(item -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String itemHtml = item.outerHtml();
                            com.agenticprice.util.PriceExtraction ext = com.agenticprice.util.PriceExtractionParser.parse(openAIService.extractPrice(itemHtml));
                            String productUrl = openAIService.extractProductUrl(itemHtml);
                            String title = item.select("a[href*='/p/']").attr("aria-label");
                            if (title.isBlank()) title = item.select("h2").text();
                            if ((ext == null || ext.price() == null) || title.isBlank()) return null;
                            String fullUrl = productUrl.startsWith("/") ? "https://www.target.com" + productUrl : productUrl;
                            return new PriceResult("Target", title, ext.price().toPlainString(), "USD", fullUrl, ext != null && ext.financed());
                        } catch (Exception e) {
                            log.warn("Failed to process Target item: {}", e.getMessage());
                            return null;
                        }
                    }))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Target scrape failed for query '{}': {}", productQuery, e.getMessage());
            return List.of();
        }
    }
}