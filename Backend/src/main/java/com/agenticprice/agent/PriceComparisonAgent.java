package com.agenticprice.agent;

import com.agenticprice.api.PriceComparisonResponse;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceComparisonAgent {

    private final ScraperService scraperService;

    public PriceComparisonResponse compare(String query) {
        List<PriceResult> raw = scraperService.search(query);

        List<PriceResult> deduped = raw.stream()
                .filter(r -> r.getPrice() != null && !r.getPrice().isBlank())
                .filter(r -> r.getUrl() != null && !r.getUrl().isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                r -> extractProductKey(r.getUrl()),
                                r -> r,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ),
                        m -> new ArrayList<>(m.values())
                ));

        List<String> queried = scraperService.getRetailerNames();
        List<String> withResults = deduped.stream()
                .map(PriceResult::getRetailerName)
                .distinct()
                .toList();

        return new PriceComparisonResponse(query, deduped.size(), queried, withResults, deduped);
    }

    private String extractProductKey(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            Matcher m = Pattern.compile("/dp/([A-Z0-9]{10})").matcher(path);
            if (m.find()) return m.group(1);
            String[] parts = path.split("/");
            String last = parts[parts.length - 1];
            return last.isEmpty() ? parts[parts.length - 2] : last;
        } catch (Exception e) {
            return url.split("\\?")[0];
        }
    }
}