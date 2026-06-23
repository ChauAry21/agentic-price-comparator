package com.agenticprice.service;

import com.agenticprice.repository.PriceSnapshotRepository;
import com.agenticprice.repository.ProductRepository;
import com.agenticprice.repository.RetailerRepository;
import com.agenticprice.scraper.PriceResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScraperServiceRankingTest {

    @Test
    void getRankingReturnsProductsInOpenAiProvidedOrder() {
        ScraperService service = new ScraperService(
                List.of(),
                new StubOpenAIService("{\"ordered_indices\":[2,0,1]}"),
                emptyProductRepository(),
                emptyRetailerRepository(),
                emptyPriceSnapshotRepository()
        );

        List<PriceResult> products = List.of(
                product("Amazon", "Product A", "$79.99", "https://amazon.com/a"),
                product("Walmart", "Product B", "$74.99", "https://walmart.com/b"),
                product("Newegg", "Product C", "$69.99", "https://newegg.com/c")
        );

        List<PriceResult> ranked = service.getRanking(products, "best earbuds");

        assertEquals(List.of(
                "Newegg",
                "Amazon",
                "Walmart"
        ), ranked.stream().map(PriceResult::getRetailerName).toList());
    }

    @Test
    void getRankingFallsBackToOriginalOrderWhenJsonIsInvalid() {
        ScraperService service = new ScraperService(
                List.of(),
                new StubOpenAIService("this-is-not-json"),
                emptyProductRepository(),
                emptyRetailerRepository(),
                emptyPriceSnapshotRepository()
        );

        List<PriceResult> products = List.of(
                product("Amazon", "Product A", "$79.99", "https://amazon.com/a"),
                product("Walmart", "Product B", "$74.99", "https://walmart.com/b"),
                product("Newegg", "Product C", "$69.99", "https://newegg.com/c")
        );

        List<PriceResult> ranked = service.getRanking(products, "best earbuds");

        assertEquals(products, ranked);
    }

    @Test
    void getRankingStripsMarkdownFencesBeforeParsing() {
        // The LLM sometimes wraps its JSON in ```json ... ``` even when the
        // prompt forbids it. The parser must strip the fences or fall back
        // to the unranked list, which is what users were seeing.
        ScraperService service = new ScraperService(
                List.of(),
                new StubOpenAIService("```json\n{\"ordered_indices\":[2,0,1]}\n```"),
                emptyProductRepository(),
                emptyRetailerRepository(),
                emptyPriceSnapshotRepository()
        );

        List<PriceResult> products = List.of(
                product("Amazon", "Product A", "$79.99", "https://amazon.com/a"),
                product("Walmart", "Product B", "$74.99", "https://walmart.com/b"),
                product("Newegg", "Product C", "$69.99", "https://newegg.com/c")
        );

        List<PriceResult> ranked = service.getRanking(products, "best earbuds");

        assertEquals(List.of("Newegg", "Amazon", "Walmart"),
                ranked.stream().map(PriceResult::getRetailerName).toList());
    }

    @Test
    void getRankingFallsBackToOriginalOrderWhenRankingIsIncomplete() {
        ScraperService service = new ScraperService(
                List.of(),
                new StubOpenAIService("{\"ordered_indices\":[1,0]}"),
                emptyProductRepository(),
                emptyRetailerRepository(),
                emptyPriceSnapshotRepository()
        );

        List<PriceResult> products = List.of(
                product("Amazon", "Product A", "$79.99", "https://amazon.com/a"),
                product("Walmart", "Product B", "$74.99", "https://walmart.com/b"),
                product("Newegg", "Product C", "$69.99", "https://newegg.com/c")
        );

        List<PriceResult> ranked = service.getRanking(products, "best earbuds");

        assertEquals(products, ranked);
    }

    private static PriceResult product(String retailer, String name, String price, String url) {
        return new PriceResult(retailer, name, price, "USD", url);
    }

    private static ProductRepository emptyProductRepository() {
        return proxy(ProductRepository.class);
    }

    private static RetailerRepository emptyRetailerRepository() {
        return proxy(RetailerRepository.class);
    }

    private static PriceSnapshotRepository emptyPriceSnapshotRepository() {
        return proxy(PriceSnapshotRepository.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if (name.equals("toString")) {
                    return "proxy:" + type.getSimpleName();
                }
                if (name.equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (name.equals("equals")) {
                    return proxy == args[0];
                }
                throw new UnsupportedOperationException("Unexpected call to " + type.getSimpleName() + "." + name);
            }
        };

        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static final class StubOpenAIService extends OpenAIService {
        private final String rankingJson;

        private StubOpenAIService(String rankingJson) {
            super("dummy-api-key");
            this.rankingJson = rankingJson;
        }

        @Override
        public String rankProducts(List<PriceResult> products, String userQuery) {
            return rankingJson;
        }
    }
}
