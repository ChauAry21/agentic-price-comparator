package com.agenticprice.scraper;

import com.agenticprice.service.OpenAIService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemuScraperAgentSmokeTest {

    public static void main(String[] args) {
        String query = args.length > 0 ? String.join(" ", args) : "wireless earbuds";
        System.out.println("Running Temu smoke test for query: " + query);

        TemuScraperAgent scraper = new TemuScraperAgent(new StubOpenAIService());
        List<PriceResult> results = scraper.scrape(query);

        System.out.println("Found " + results.size() + " results:");
        results.forEach(System.out::println);
    }

    private static class StubOpenAIService extends OpenAIService {
        public StubOpenAIService() {
            super("dummy-api-key");
        }

        @Override
        public String extractPrice(String rawHtml) {
            Pattern pattern = Pattern.compile("\\$\\s?[0-9]+(?:[\\.,][0-9]{2})?");
            Matcher matcher = pattern.matcher(rawHtml);
            return matcher.find() ? matcher.group() : "PRICE_NOT_FOUND";
        }

        @Override
        public String extractProductUrl(String rawHtml) {
            Pattern hrefPattern = Pattern.compile("href=\\\"([^\\\"]+)\\\"");
            Matcher hrefMatcher = hrefPattern.matcher(rawHtml);
            if (hrefMatcher.find()) {
                return hrefMatcher.group(1);
            }
            Pattern altHrefPattern = Pattern.compile("href='([^']+)'");
            Matcher altHrefMatcher = altHrefPattern.matcher(rawHtml);
            if (altHrefMatcher.find()) {
                return altHrefMatcher.group(1);
            }
            return "URL_NOT_FOUND";
        }
    }
}
