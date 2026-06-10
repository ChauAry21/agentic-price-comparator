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

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
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
                    snapshot.setPrice(parsePrice(result.getPrice()));
                    snapshot.setCurrency(result.getCurrency());
                    snapshot.setUrl(result.getUrl());
                    snapshot.setScrapedAt(OffsetDateTime.now());
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

    private BigDecimal parsePrice(String priceStr) {
        try {
            String cleaned = priceStr.replaceAll("[^0-9.]", "");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public List<PriceResult> getRanking(List<PriceResult> products, String query) {
        String raw_ranking = openAIService.rankProducts(products, query);
        List<Integer> indices = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(raw_ranking);
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
}