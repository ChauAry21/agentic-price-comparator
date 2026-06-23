package com.agenticprice.tracking.service;

import com.agenticprice.scraper.PriceResult;
import com.agenticprice.service.OpenAIService;
import com.agenticprice.service.ScraperService;
import com.agenticprice.tracking.api.TrackingCandidateResponse;
import com.agenticprice.tracking.model.MatchMethod;
import com.agenticprice.tracking.model.TrackedQuery;
import com.agenticprice.util.ProductKeyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueryResolutionService {

    private final ScraperService scraperService;
    private final OpenAIService openAIService;

    public TrackingCandidateResponse getCandidates(String rawQuery) {
        List<PriceResult> results = scraperService.search(rawQuery);
        List<ProductCluster> clusters = clusterResults(results);
        boolean autoSelected = shouldAutoSelect(clusters);

        TrackingCandidateResponse response = new TrackingCandidateResponse();
        response.setQuery(rawQuery);
        response.setAutoSelected(autoSelected);
        response.setCandidates(clusters.stream().map(this::toCandidate).toList());
        return response;
    }

    List<ProductCluster> clusterResults(List<PriceResult> results) {
        Map<String, List<PriceResult>> grouped = results.stream()
                .filter(r -> r.getPrice() != null && !r.getPrice().isBlank())
                .filter(r -> r.getUrl() != null && !r.getUrl().isBlank())
                .collect(Collectors.groupingBy(
                        r -> ProductKeyUtil.extractProductKey(r.getUrl()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ProductCluster> clusters = new ArrayList<>();
        for (Map.Entry<String, List<PriceResult>> entry : grouped.entrySet()) {
            List<PriceResult> group = entry.getValue();
            PriceResult cheapest = group.stream()
                    .min(Comparator.comparing(r -> parsePrice(r.getPrice())))
                    .orElseThrow();

            List<String> retailers = group.stream()
                    .map(PriceResult::getRetailerName)
                    .distinct()
                    .toList();

            BigDecimal lowestPrice = group.stream()
                    .map(r -> parsePrice(r.getPrice()))
                    .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            clusters.add(new ProductCluster(
                    entry.getKey(),
                    cheapest.getProductName(),
                    cheapest.getUrl(),
                    retailers,
                    lowestPrice,
                    group.size()
            ));
        }

        return clusters.stream()
                .sorted(Comparator
                        .comparingInt(ProductCluster::resultCount).reversed()
                        .thenComparing(ProductCluster::lowestPrice))
                .limit(5)
                .toList();
    }

    public List<MatchedResult> matchResults(List<PriceResult> results, TrackedQuery tracked) {
        String canonicalKey = tracked.getCanonicalProductKey();
        List<String> nameTokens = keyTokens(tracked.getCanonicalProductName());

        List<MatchedResult> matched = new ArrayList<>();
        for (PriceResult result : results) {
            BigDecimal price = parsePrice(result.getPrice());
            boolean isMatched = false;
            MatchMethod method = MatchMethod.NONE;

            String resultKey = ProductKeyUtil.extractProductKey(result.getUrl());
            if (!canonicalKey.isBlank() && canonicalKey.equals(resultKey)) {
                isMatched = true;
                method = MatchMethod.KEY;
            } else if (result.getProductName() != null && nameMatches(result.getProductName(), nameTokens)) {
                isMatched = true;
                method = MatchMethod.NAME;
            }

            matched.add(new MatchedResult(result, price, isMatched, method));
        }
        return matched;
    }

    private boolean shouldAutoSelect(List<ProductCluster> clusters) {
        if (clusters.isEmpty()) {
            return false;
        }

        ProductCluster top = clusters.get(0);
        boolean multiRetailer = top.retailers().size() >= 2;
        boolean dominates = clusters.size() == 1
                || top.resultCount() >= clusters.get(1).resultCount() * 2;

        return multiRetailer || dominates;
    }

    private TrackingCandidateResponse.TrackingCandidate toCandidate(ProductCluster cluster) {
        TrackingCandidateResponse.TrackingCandidate candidate = new TrackingCandidateResponse.TrackingCandidate();
        candidate.setProductKey(cluster.productKey());
        candidate.setProductName(cluster.productName());
        candidate.setSampleUrl(cluster.sampleUrl());
        candidate.setRetailers(cluster.retailers());
        candidate.setLowestPrice(cluster.lowestPrice());
        candidate.setResultCount(cluster.resultCount());
        return candidate;
    }

    private boolean nameMatches(String productName, List<String> tokens) {
        if (tokens.isEmpty()) {
            return false;
        }
        String normalized = openAIService.normalizeProductName(productName).toLowerCase();
        return tokens.stream().allMatch(normalized::contains);
    }

    private List<String> keyTokens(String canonicalProductName) {
        if (canonicalProductName == null || canonicalProductName.isBlank()) {
            return List.of();
        }
        return Arrays.stream(canonicalProductName.toLowerCase().split("\\W+"))
                .filter(token -> token.length() >= 3)
                .distinct()
                .toList();
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
            return BigDecimal.ZERO;
        }
    }

    private record ProductCluster(
            String productKey,
            String productName,
            String sampleUrl,
            List<String> retailers,
            BigDecimal lowestPrice,
            int resultCount
    ) {}

    public record MatchedResult(
            PriceResult result,
            BigDecimal price,
            boolean matched,
            MatchMethod matchMethod
    ) {}
}
