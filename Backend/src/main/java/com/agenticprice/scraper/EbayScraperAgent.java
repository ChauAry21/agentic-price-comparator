package com.agenticprice.scraper;

import com.agenticprice.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EbayScraperAgent implements ScraperAgent {

    private final OpenAIService openAIService;

    @Override
    public String getRetailerName() {
        return "eBay";
    }

    @Override
    public List<PriceResult> scrape(String productQuery) {
        List<PriceResult> results = new ArrayList<>();
        try {
            String url = "https://www.ebay.com/sch/i.html?_nkw=" + productQuery.replace(" ", "+");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("li.s-item");
            for (Element item : items.stream().limit(5).toList()) {
                String html = item.outerHtml();
                String price = openAIService.extractPrice(html);
                String productUrl = item.select("a.s-item__link").attr("href");
                String title = item.select(".s-item__title").text();

                if (!price.equals("PRICE_NOT_FOUND") && !title.isBlank() && !title.equals("Shop on eBay")) {
                    results.add(new PriceResult("eBay", title, price, "USD", productUrl));
                }
            }
        } catch (Exception e) {
            log.error("eBay scrape failed for query '{}': {}", productQuery, e.getMessage());
        }
        return results;
    }
}