package com.agenticprice.scraper;

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
public class NeweggScraperAgent implements ScraperAgent {

    private final OpenAIService openAIService;

    @Override
    public String getRetailerName() {
        return "Newegg";
    }

    @Override
    public List<PriceResult> scrape(String productQuery) {
        List<PriceResult> results = new ArrayList<>();
        try {
            String url = "https://www.newegg.com/p/pl?d=" + productQuery.replace(" ", "+");
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("div.item-cell");
            if (items.isEmpty()) items = doc.select(".item-container");
            if (items.isEmpty()) items = doc.select("[class*=item-cell]");
            log.info("Newegg found {} items for query '{}'", items.size(), productQuery);
            if (items.isEmpty()) log.warn("Newegg page title: {}", doc.title());
            for (Element item : items.stream().limit(5).toList()) {
                String title = item.select("a.item-title").text();
                if (title.isBlank()) title = item.select("[class*=item-title]").text();
                String productUrl = item.select("a.item-title").attr("href");
                if (productUrl.isBlank()) productUrl = item.select("a[href*=newegg]").attr("href");
                String priceWhole = item.select("li.price-current strong").text();
                String priceFraction = item.select("li.price-current sup").text();

                if (!title.isBlank() && !priceWhole.isBlank()) {
                    String price = "$" + priceWhole.replace(",", "") + (priceFraction.isBlank() ? ".00" : priceFraction);
                    results.add(new PriceResult("Newegg", title, price, "USD", productUrl));
                }
            }
        } catch (Exception e) {
            log.error("Newegg scrape failed for query '{}': {}", productQuery, e.getMessage());
        }
        return results;
    }
}