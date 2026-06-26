package com.agenticprice.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class TargetScraperAgentSmokeTest {

    public static void main(String[] args) {
        try {
            String url = "https://www.target.com/s?searchTerm=mens+shorts";
            System.out.println("Fetching Target search page: " + url);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(10000)
                    .get();

            Elements items = doc.select("div[data-test='@web/site-top-of-funnel/ProductCardWrapper']");
            System.out.println(
                    "Found " + items.size() + " product cards using selector: div[data-test='@web/site-top-of-funnel/ProductCardWrapper']");

            items.stream().limit(3).forEach(item -> {
                String title = item.select("a[href*='/p/']").attr("aria-label");
                if (title.isBlank()) {
                    title = item.select("h2").text();
                }
                System.out.println("  - Product: " + title);
            });

        } catch (Exception e) {
            System.out.println("Target smoke test failed");
            e.printStackTrace(System.out);
        }
    }
}
