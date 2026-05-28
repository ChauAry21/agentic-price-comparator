package com.agenticprice.scraper;

import com.agenticprice.prompt.PriceHawkPrompt;
import com.agenticprice.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        List<PriceResult> results = new ArrayList<>();
        try {
            String url = "https://www.amazon.com/s?k=" + productQuery.replace(" ", "+");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("div[data-component-type=s-search-result]");
            for (Element item : items.stream().limit(5).toList()) {
                String html = item.outerHtml();
                String price = openAIService.extractPrice(html);
                String productUrl = openAIService.extractProductUrl(html);
                String title = item.select("h2 span").text();

                if (!price.equals("PRICE_NOT_FOUND") && !title.isBlank()) {
                    results.add(new PriceResult("Amazon", title, price, "USD", productUrl));
                }
            }
        } catch (Exception e) {
            log.error("Amazon scrape failed for query '{}': {}", productQuery, e.getMessage());
        }
        return results;
    }
}