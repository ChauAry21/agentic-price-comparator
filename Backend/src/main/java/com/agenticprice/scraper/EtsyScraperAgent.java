package com.agenticprice.scraper;

import com.agenticprice.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class EtsyScraperAgent implements ScraperAgent {
    private final OpenAIService openAIService;

    @Override
    public String getRetailerName() {
        return "Etsy";
    }

    @Override
    public List<PriceResult> scrape(String productQuery) {
        List<PriceResult> results = new ArrayList<>();
        try{
            String url = "https://www.etsy.com/search?q=" + URLEncoder.encode(productQuery, StandardCharsets.UTF_8);

            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

            Connection.Response landing = Jsoup.connect("https://www.etsy.com/")
                    .userAgent(userAgent)
                    .followRedirects(true)
                    .timeout(10000)
                    .execute();
            Map<String, String> cookies = landing.cookies();

            Document doc = Jsoup.connect(url)
                    .userAgent(userAgent)
                    .cookies(cookies)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .referrer("https://www.google.com")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("li.wt-list-item");
            if (items.isEmpty()) items = doc.select("div.wt-grid__item");
            if (items.isEmpty()) items = doc.select("div.wt-grid__item-inner");
            log.info("Etsy found {} items for query '{}'", items.size(), productQuery);

            if (items.isEmpty()) log.warn("Etsy page title: {}", doc.title());
            for(Element item : items.stream().limit(5).toList()) {
                String html = item.outerHtml();
                com.agenticprice.util.PriceExtraction ext = com.agenticprice.util.PriceExtractionParser.parse(openAIService.extractPrice(html));
                if ((ext == null || ext.price() == null)) continue;


                String productUrl = item.select("a.listing-link").attr("href");
                if (productUrl.isBlank()) productUrl = item.select("a[href*=etsy]").attr("href");
                String cleanUrl = productUrl.replace("'", "").replace("\"", "").trim();
                String fullUrl = cleanUrl.startsWith("/") ? "https://www.etsy.com" + cleanUrl : cleanUrl;

                String title = item.select("a.listing-link").text();
                if (title.isBlank()) title = item.select("h3.wt-text-body-01").text();
                String lower = title.toLowerCase();
                if (lower.contains("blocked") || lower.contains("captcha")
                        || lower.contains("security check") || lower.contains("access denied")) {
                    log.warn("Etsy blocked for query '{}'", productQuery);
                    return results;
                }

                if (!(ext == null || ext.price() == null) && !title.isBlank() && !title.equals("Shop on Etsy")) {
                    results.add(new PriceResult("Etsy", title, ext.price().toPlainString(), "USD", fullUrl, ext.financed()));
                }
            }
        } catch (Exception e) {
            log.error("Etsy scrape failed for query '{}': {}", productQuery, e.getMessage());
        }
        return results;
    }
}
