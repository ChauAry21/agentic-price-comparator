package com.agenticprice.service;

import com.agenticprice.model.PriceSnapshot;
import com.agenticprice.model.Product;
import com.agenticprice.model.Retailer;
import com.agenticprice.repository.PriceSnapshotRepository;
import com.agenticprice.repository.ProductRepository;
import com.agenticprice.repository.RetailerRepository;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.scraper.ScraperAgent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private final List<ScraperAgent> scraperAgents;
    private final OpenAIService openAIService;
    private final ProductRepository productRepository;
    private final RetailerRepository retailerRepository;
    private final PriceSnapshotRepository priceSnapshotRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> getRetailerNames() {
        return scraperAgents.stream()
                .map(ScraperAgent::getRetailerName)
                .toList();
    }

    public List<PriceResult> search(String rawQuery) {
        String parsedQuery = openAIService.parseQuery(rawQuery);
        log.info("Parsed query '{}' -> '{}'", rawQuery, parsedQuery);

        List<CompletableFuture<List<PriceResult>>> futures = scraperAgents.stream()
                .map(agent -> CompletableFuture.supplyAsync(() -> {
                    log.info("Running scraper: {}", agent.getRetailerName());
                    return agent.scrape(parsedQuery);
                }))
                .toList();

        List<PriceResult> results = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        return results;
    }

    public void scrapeAndSave(String rawQuery) {
        String parsedQuery = openAIService.parseQuery(rawQuery);
        String normalizedName = openAIService.normalizeProductName(parsedQuery);
        String slug = normalizedName.toLowerCase().replaceAll("[^a-z0-9]+", "-");

        Product product = productRepository.findBySlug(slug).orElseGet(() -> {
            Product p = new Product();
            p.setName(normalizedName);
            p.setSlug(slug);
            p.setCreatedAt(OffsetDateTime.now());
            return productRepository.save(p);
        });

        for (ScraperAgent agent : scraperAgents) {
            List<PriceResult> results = agent.scrape(parsedQuery);
            Retailer retailer = findOrCreateRetailer(agent.getRetailerName());

            for (PriceResult result : results) {
                try {
                    PriceSnapshot snapshot = new PriceSnapshot();
                    snapshot.setProduct(product);
                    snapshot.setRetailer(retailer);
                    snapshot.setPrice(toBigDecimal(result.getPrice()));
                    snapshot.setCurrency(result.getCurrency());
                    snapshot.setUrl(result.getUrl());
                    snapshot.setScrapedAt(OffsetDateTime.now());
                    snapshot.setFinanced(result.isFinanced());
                    priceSnapshotRepository.save(snapshot);
                } catch (Exception e) {
                    log.warn("Failed to save snapshot for {}: {}", result.getProductName(), e.getMessage());
                }
            }
        }
    }

    private Retailer findOrCreateRetailer(String name) {
        return retailerRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Retailer r = new Retailer();
            r.setName(name);
            r.setBaseUrl("https://www." + name.toLowerCase() + ".com");
            return retailerRepository.save(r);
        });
    }

    /**
     * Converts a {@link PriceResult#getPrice()} string to a BigDecimal.
     * The field is populated by the LLM extractor (or scraped directly) as
     * a canonical numeric string, so no regex stripping is needed. Returns
     * BigDecimal.ZERO on malformed input so the save can't fail.
     */
    private static BigDecimal toBigDecimal(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) return BigDecimal.ZERO;
        // Defensive: scrapers that build prices from raw HTML may emit
        // currency-prefixed strings (e.g. "$19.79"). Strip everything
        // that isn't a digit or decimal point before parsing so a stray
        // "$" doesn't silently zero a listing.
        String cleaned = priceStr.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Unable to parse price value: {}", priceStr);
            return BigDecimal.ZERO;
        }
    }

    public List<PriceResult> getRanking(List<PriceResult> products, String query) {
        String rawRanking = openAIService.rankProducts(products, query);
        List<Integer> indices = new ArrayList<>();
        try {
            String cleaned = stripMarkdownFences(rawRanking);
            JsonNode root = objectMapper.readTree(cleaned);
            JsonNode indicesNode = root.get("ordered_indices");
            if (indicesNode != null && indicesNode.isArray()) {
                for (JsonNode node : indicesNode) {
                    indices.add(node.asInt());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse ranking JSON: {}", e.getMessage());
        }
        if (indices.isEmpty()) {
            return products;
        }
        List<PriceResult> ranked = new ArrayList<>();
        for (Integer index : indices) {
            if (index != null && index >= 0 && index < products.size()) {
                ranked.add(products.get(index));
            }
        }
        return ranked.size() == products.size() ? ranked : products;
    }

    /**
     * Strips markdown code fences from a string if present. The LLM
     * sometimes wraps JSON in ```json ... ``` even when the prompt asks
     * for raw JSON, and the parser fails on the leading backtick.
     */
    private String stripMarkdownFences(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0) cleaned = cleaned.substring(firstNewline + 1);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();
        }
        return cleaned;
    }
}
