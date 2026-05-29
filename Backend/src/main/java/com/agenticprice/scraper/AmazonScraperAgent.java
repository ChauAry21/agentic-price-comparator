package com.agenticprice.scraper;

import com.agenticprice.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonScraperAgent implements ScraperAgent {

    private final OpenAIService openAIService;

    @Override
    public String getRetailerName() {
        return "Amazon";
    }

    @Override
    public List<PriceResult> scrape(String productQuery) {
        try {
            String url = "https://www.amazon.com/s?k=" + productQuery.replace(" ", "+");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("div[data-component-type=s-search-result]");

            List<CompletableFuture<PriceResult>> futures = items.stream()
                    .limit(5)
                    .map(item -> CompletableFuture.supplyAsync(() -> {
                        try {
                            String html = item.outerHtml();
                            String price = openAIService.extractPrice(html);
                            String productUrl = openAIService.extractProductUrl(html);
                            String title = item.select("h2 span").text();
                            if (price.equals("PRICE_NOT_FOUND") || title.isBlank()) return null;
                            String fullUrl = productUrl.startsWith("/") ? "https://www.amazon.com" + productUrl : productUrl;
                            return new PriceResult("Amazon", title, price, "USD", fullUrl);
                        } catch (Exception e) {
                            log.warn("Failed to process Amazon item: {}", e.getMessage());
                            return null;
                        }
                    }))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Amazon scrape failed for query '{}': {}", productQuery, e.getMessage());
            return List.of();
        }
    }
}