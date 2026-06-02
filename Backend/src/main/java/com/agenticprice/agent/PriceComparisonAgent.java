package com.agenticprice.agent;

import com.agenticprice.api.PriceComparisonResponse;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.ScraperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        PriceResult bestDeal = getBestDeal(deduped);
        BigDecimal lowestPrice = getLowestPrice(deduped);
        BigDecimal highestPrice = getHighestPrice(deduped);
        BigDecimal averagePrice = getAveragePrice(deduped);
        BigDecimal potentialSavings = highestPrice.subtract(lowestPrice).max(BigDecimal.ZERO);

        return new PriceComparisonResponse(
                query,
                deduped.size(),
                queried,
                withResults,
                bestDeal != null ? bestDeal.getRetailerName() : null,
                lowestPrice,
                highestPrice,
                averagePrice,
                potentialSavings,
                deduped
        );
    }

    private PriceResult getBestDeal(List<PriceResult> results) {
        return results.stream()
                .filter(result -> parsePrice(result.getPrice()).compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing(result -> parsePrice(result.getPrice())))
                .orElse(null);
    }

    private BigDecimal getLowestPrice(List<PriceResult> results) {
        return results.stream()
                .map(result -> parsePrice(result.getPrice()))
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getHighestPrice(List<PriceResult> results) {
        return results.stream()
                .map(result -> parsePrice(result.getPrice()))
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getAveragePrice(List<PriceResult> results) {
        List<BigDecimal> prices = results.stream()
                .map(result -> parsePrice(result.getPrice()))
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal parsePrice(String price) {
        if (price == null || price.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            String cleaned = price.replaceAll("[^0-9.]", "");

            if (cleaned.isBlank()) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Unable to parse price value: {}", price);
            return BigDecimal.ZERO;
        }
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
