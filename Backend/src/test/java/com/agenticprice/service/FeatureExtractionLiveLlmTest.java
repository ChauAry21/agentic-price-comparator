package com.agenticprice.service;

import com.agenticprice.api.FeatureExtractionRequest;
import com.agenticprice.api.FeatureExtractionResponse;
import com.agenticprice.scraper.PriceResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live integration tests for the EXTRACT_FEATURES prompt. These tests call
 * the real OpenAI API and are disabled by default to keep the test suite
 * fast and offline. Enable them per-run with:
 *
 * mvn test -Dtest=FeatureExtractionLiveLlmTest
 *
 * The {@code OPENAI_API_KEY} environment variable must be set; if it isn't,
 * each test is skipped silently via JUnit assumptions.
 *
 * Each test prints the LLM's raw response to stdout so the prompt's output
 * shape can be eyeballed. Assertions are intentionally light because the
 * LLM is non-deterministic — the goal is to validate the end-to-end
 * pipeline, not to lock the model output down.
 */
@Disabled("Hits the live OpenAI API. Enable explicitly with -Dtest=FeatureExtractionLiveLlmTest and OPENAI_API_KEY set.")
class FeatureExtractionLiveLlmTest {

    private static FeatureExtractionService service;
    private static String apiKey;

    @BeforeAll
    static void setUp() {
        apiKey = System.getenv("OPENAI_API_KEY");
        boolean ok = apiKey != null && !apiKey.isBlank();
        System.out.println("[FeatureExtractionLiveLlmTest] OPENAI_API_KEY set: " + ok
                + " (length=" + (apiKey == null ? 0 : apiKey.length()) + ")");
        if (!ok) {
            System.out.println("[FeatureExtractionLiveLlmTest] Skipping: OPENAI_API_KEY is not set in this shell.");
        }
        assumeTrue(ok, "OPENAI_API_KEY is not set; skipping live LLM tests");
        service = new FeatureExtractionService(new OpenAIService(apiKey));
    }

    @Test
    void headphonesWithDifferentNamingConventions() {
        List<PriceResult> products = List.of(
                new PriceResult("Amazon", "Sony WH-1000XM5 Wireless Noise Cancelling Headphones (Black)", "$349.00",
                        "USD", "https://amazon.com/dp/A111111111"),
                new PriceResult("Walmart", "Sony WH1000XM5 Over-Ear Bluetooth Headphones - Black", "$329.00", "USD",
                        "https://walmart.com/ip/xm5"),
                new PriceResult("Newegg", "Sony WH-1000XM5 Premium Wireless Headphones", "$359.00", "USD",
                        "https://newegg.com/p/xm5"));
        runAndPrint("headphones", products);
    }

    @Test
    void smartphonesWithVariants() {
        List<PriceResult> products = List.of(
                new PriceResult("Amazon", "Apple iPhone 15 Pro 256GB - Natural Titanium (Unlocked)", "$1099.00", "USD",
                        "https://amazon.com/dp/A222222222"),
                new PriceResult("Walmart", "Apple iPhone 15 Pro 256GB, Natural Titanium, Unlocked", "$1079.00", "USD",
                        "https://walmart.com/ip/iphone15pro"),
                new PriceResult("eBay", "iPhone 15 Pro 256GB Natural Titanium Unlocked - Refurbished", "$899.00", "USD",
                        "https://ebay.com/itm/iphone15pro"));
        runAndPrint("smartphones", products);
    }

    @Test
    void sparseListingsWhereSomeFeaturesAreMissing() {
        // Two of the three products omit Color. The LLM should return null
        // for the missing cells, not invent values.
        List<PriceResult> products = List.of(
                new PriceResult("Amazon", "Bose QuietComfort 45 Headphones", "$279.00", "USD",
                        "https://amazon.com/dp/A333333333"),
                new PriceResult("Walmart", "Bose QC45 Wireless Headphones Black", "$269.00", "USD",
                        "https://walmart.com/ip/qc45"),
                new PriceResult("Newegg", "Bose QuietComfort 45", "$289.00", "USD", "https://newegg.com/p/qc45"));
        runAndPrint("sparse", products);
    }

    @Test
    void singleProductIsABaseline() {
        // A single listing should still produce a sensible (1-column) table.
        List<PriceResult> products = List.of(
                new PriceResult("Amazon", "Logitech MX Master 3S Wireless Mouse (Graphite)", "$99.99", "USD",
                        "https://amazon.com/dp/A444444444"));
        runAndPrint("single", products);
    }

    @Test
    void differentBrandsForSameCategoryShouldNotBeConfused() {
        // Sanity check: two different SKUs in the same category. The LLM
        // should produce features that apply to both, not invent shared
        // features that don't exist (e.g. claiming both have ANC).
        List<PriceResult> products = List.of(
                new PriceResult("Amazon", "Sony WH-1000XM5 (Black)", "$349.00", "USD",
                        "https://amazon.com/dp/A111111111"),
                new PriceResult("Walmart", "Apple AirPods Max (Space Gray)", "$479.00", "USD",
                        "https://walmart.com/ip/airpodsmax"));
        runAndPrint("differentBrands", products);
    }

    private void runAndPrint(String label, List<PriceResult> products) {
        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(products));

        System.out.println("\n=== Live LLM test: " + label + " ===");
        System.out.println("Columns: " + response.getColumns().size());
        for (var column : response.getColumns()) {
            System.out.println("  " + column.getKey() + ": " + column.getRetailerName() + " - " + column.getProductName());
        }
        System.out.println("Warnings: " + response.getWarnings());
        for (var feature : response.getFeatures()) {
            System.out.println("  " + feature.getName() + " -> " + feature.getValues());
        }
        System.out.println("=== End " + label + " ===\n");
    }
}
