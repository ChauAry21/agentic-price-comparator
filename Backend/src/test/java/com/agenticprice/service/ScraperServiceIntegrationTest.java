package com.agenticprice.service;

import com.agenticprice.model.PriceSnapshot;
import com.agenticprice.model.Product;
import com.agenticprice.model.Retailer;
import com.agenticprice.repository.PriceSnapshotRepository;
import com.agenticprice.repository.ProductRepository;
import com.agenticprice.repository.RetailerRepository;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.scraper.ScraperAgent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ScraperServiceIntegrationTest {

    @Test
    void rankingParsesJsonAndReturnsProductsInExpectedOrder() {
        System.out.println("[smoke] starting ranking test");

        CapturingScraperAgent amazon = new CapturingScraperAgent(
                "Amazon",
                List.of(
                        new PriceResult("Amazon", "Product A", "$79.99", "USD", "https://amazon.com/dp/a"),
                        new PriceResult("Amazon", "Product B", "$74.99", "USD", "https://amazon.com/dp/b")));

        OpenAIService openAIService = new StubOpenAIService();
        ScraperService scraperService = new ScraperService(
                List.of(amazon),
                openAIService,
                fakeProductRepository(),
                fakeRetailerRepository(),
                fakePriceSnapshotRepository());

        List<PriceResult> input = List.of(
                new PriceResult("Amazon", "Product A", "$79.99", "USD", "https://amazon.com/dp/a"),
                new PriceResult("Walmart", "Product B", "$74.99", "USD", "https://walmart.com/ip/b"));

        List<PriceResult> ranked = scraperService.getRanking(input, "best wireless earbuds under 100");

        System.out.println("[smoke] ranking input:");
        input.forEach(result -> System.out.println("  - " + describe(result)));
        System.out.println("[smoke] ranking output:");
        ranked.forEach(result -> System.out.println("  - " + describe(result)));

        assertEquals(2, ranked.size());
        assertEquals("Walmart", ranked.get(0).getRetailerName());
        assertEquals("Amazon", ranked.get(1).getRetailerName());
        System.out.println("[smoke] ranking test passed");
    }

    @Test
    void scrapeAndSavePersistsSnapshotsWithoutMockito() {
        System.out.println("[smoke] starting persistence test");

        CapturingScraperAgent amazon = new CapturingScraperAgent(
                "Amazon",
                List.of(new PriceResult("Amazon", "Wireless Earbuds", "$79.99", "USD",
                        "https://amazon.com/dp/example")));

        InMemoryProductRepository productRepository = new InMemoryProductRepository();
        InMemoryRetailerRepository retailerRepository = new InMemoryRetailerRepository();
        InMemoryPriceSnapshotRepository priceSnapshotRepository = new InMemoryPriceSnapshotRepository();

        OpenAIService openAIService = new StubOpenAIService();
        ScraperService scraperService = new ScraperService(
                List.of(amazon),
                openAIService,
                productRepository.proxy(),
                retailerRepository.proxy(),
                priceSnapshotRepository.proxy());

        scraperService.scrapeAndSave("best wireless earbuds under 100");

        assertEquals("wireless earbuds", amazon.getLastQuery());
        assertEquals(1, productRepository.savedCount());
        assertEquals(1, retailerRepository.savedCount());
        assertEquals(1, priceSnapshotRepository.savedCount());

        Product savedProduct = productRepository.findBySlug("wireless-earbuds").orElseThrow();
        Retailer savedRetailer = retailerRepository.findByNameIgnoreCase("Amazon").orElseThrow();
        PriceSnapshot savedSnapshot = priceSnapshotRepository.savedSnapshots().get(0);

        assertEquals("Wireless Earbuds", savedProduct.getName());
        assertEquals("Amazon", savedRetailer.getName());
        assertEquals(new BigDecimal("79.99"), savedSnapshot.getPrice());
        assertEquals("USD", savedSnapshot.getCurrency());
        assertEquals("https://amazon.com/dp/example", savedSnapshot.getUrl());
        assertNotNull(savedSnapshot.getScrapedAt());
        assertNotNull(savedSnapshot.getProduct());
        assertNotNull(savedSnapshot.getRetailer());
        assertFalse(savedSnapshot.getScrapedAt().isAfter(OffsetDateTime.now().plusSeconds(5)));

        System.out.println("[smoke] persistence test passed");
    }

    private static String describe(PriceResult result) {
        return result.getRetailerName() + " | " + result.getProductName() + " | " + result.getPrice() + " | "
                + result.getUrl();
    }

    private static ProductRepository fakeProductRepository() {
        return new InMemoryProductRepository().proxy();
    }

    private static RetailerRepository fakeRetailerRepository() {
        return new InMemoryRetailerRepository().proxy();
    }

    private static PriceSnapshotRepository fakePriceSnapshotRepository() {
        return new InMemoryPriceSnapshotRepository().proxy();
    }

    private static final class StubOpenAIService extends OpenAIService {
        private StubOpenAIService() {
            super("dummy-api-key");
        }

        @Override
        public String parseQuery(String userQuery) {
            return "wireless earbuds";
        }

        @Override
        public String normalizeProductName(String productName) {
            return "Wireless Earbuds";
        }

        @Override
        public String rankProducts(List<PriceResult> products, String userQuery) {
            return """
                    {"ordered_indices":[1,0]}
                    """;
        }
    }

    private static final class CapturingScraperAgent implements ScraperAgent {
        private final String retailerName;
        private final List<PriceResult> results;
        private String lastQuery;

        private CapturingScraperAgent(String retailerName, List<PriceResult> results) {
            this.retailerName = retailerName;
            this.results = results;
        }

        @Override
        public String getRetailerName() {
            return retailerName;
        }

        @Override
        public List<PriceResult> scrape(String productQuery) {
            this.lastQuery = productQuery;
            return results;
        }

        private String getLastQuery() {
            return lastQuery;
        }
    }

    private static final class InMemoryProductRepository implements InvocationHandler {
        private final Map<String, Product> bySlug = new HashMap<>();
        private final AtomicInteger saved = new AtomicInteger();

        ProductRepository proxy() {
            return (ProductRepository) Proxy.newProxyInstance(
                    ProductRepository.class.getClassLoader(),
                    new Class<?>[] { ProductRepository.class },
                    this);
        }

        Optional<Product> findBySlug(String slug) {
            return Optional.ofNullable(bySlug.get(slug));
        }

        int savedCount() {
            return saved.get();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("findBySlug")) {
                return Optional.ofNullable(bySlug.get(args[0]));
            }
            if (name.equals("save")) {
                Product product = (Product) args[0];
                if (product.getSlug() != null) {
                    bySlug.put(product.getSlug(), product);
                }
                saved.incrementAndGet();
                return product;
            }
            if (name.equals("toString")) {
                return "InMemoryProductRepository";
            }
            throw new UnsupportedOperationException("Unsupported product repo method: " + name);
        }
    }

    private static final class InMemoryRetailerRepository implements InvocationHandler {
        private final Map<String, Retailer> byName = new HashMap<>();
        private final AtomicInteger saved = new AtomicInteger();

        RetailerRepository proxy() {
            return (RetailerRepository) Proxy.newProxyInstance(
                    RetailerRepository.class.getClassLoader(),
                    new Class<?>[] { RetailerRepository.class },
                    this);
        }

        Optional<Retailer> findByNameIgnoreCase(String name) {
            return Optional.ofNullable(byName.get(name.toLowerCase()));
        }

        int savedCount() {
            return saved.get();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("findByNameIgnoreCase")) {
                return Optional.ofNullable(byName.get(String.valueOf(args[0]).toLowerCase()));
            }
            if (name.equals("save")) {
                Retailer retailer = (Retailer) args[0];
                if (retailer.getName() != null) {
                    byName.put(retailer.getName().toLowerCase(), retailer);
                }
                saved.incrementAndGet();
                return retailer;
            }
            if (name.equals("toString")) {
                return "InMemoryRetailerRepository";
            }
            throw new UnsupportedOperationException("Unsupported retailer repo method: " + name);
        }
    }

    private static final class InMemoryPriceSnapshotRepository implements InvocationHandler {
        private final List<PriceSnapshot> savedSnapshots = new ArrayList<>();

        PriceSnapshotRepository proxy() {
            return (PriceSnapshotRepository) Proxy.newProxyInstance(
                    PriceSnapshotRepository.class.getClassLoader(),
                    new Class<?>[] { PriceSnapshotRepository.class },
                    this);
        }

        List<PriceSnapshot> savedSnapshots() {
            return savedSnapshots;
        }

        int savedCount() {
            return savedSnapshots.size();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("save")) {
                PriceSnapshot snapshot = (PriceSnapshot) args[0];
                savedSnapshots.add(snapshot);
                return snapshot;
            }
            if (name.equals("findByProductId") || name.equals("findByProductIdOrderByScrapedAtDesc")) {
                return List.of();
            }
            if (name.equals("toString")) {
                return "InMemoryPriceSnapshotRepository";
            }
            throw new UnsupportedOperationException("Unsupported price snapshot repo method: " + name);
        }
    }
}
