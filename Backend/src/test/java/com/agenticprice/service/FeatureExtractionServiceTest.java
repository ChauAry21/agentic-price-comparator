package com.agenticprice.service;

import com.agenticprice.api.Column;
import com.agenticprice.api.FeatureExtractionRequest;
import com.agenticprice.api.FeatureExtractionResponse;
import com.agenticprice.api.FeatureRow;
import com.agenticprice.prompt.PriceHawkPrompt;
import com.agenticprice.scraper.PriceResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FeatureExtractionService}. The LLM call is faked
 * with a hand-rolled {@link LlmCaller} lambda so the test runs in isolation
 * (no OpenAI client, no API key, no network).
 */
class FeatureExtractionServiceTest {

    // --- Helpers -----------------------------------------------------------

    private static PriceResult product(String retailer, String name, String price) {
        return new PriceResult(retailer, name, price, "USD", "https://example.com/" + retailer);
    }

    private static List<PriceResult> sampleProducts() {
        return List.of(
                product("Amazon", "Sony WH-1000XM5 Wireless Noise Cancelling Headphones (Black)", "$349.00"),
                product("Walmart", "Sony WH1000XM5 Over-Ear Headphones - Black", "$329.00"),
                product("Newegg", "Sony WH-1000XM5 Wireless Headphones", "$359.00")
        );
    }

    private static List<Column> expectedColumns() {
        return List.of(
                new Column("p0", "Amazon", "Sony WH-1000XM5 Wireless Noise Cancelling Headphones (Black)", false),
                new Column("p1", "Walmart", "Sony WH1000XM5 Over-Ear Headphones - Black", false),
                new Column("p2", "Newegg", "Sony WH-1000XM5 Wireless Headphones", false)
        );
    }

    private static String wellFormedLlmResponse() {
        return """
                {
                  "features": [
                    { "name": "Brand", "values": { "p0": "Sony", "p1": "Sony", "p2": "Sony" } },
                    { "name": "Model", "values": { "p0": "WH-1000XM5", "p1": "WH1000XM5", "p2": "WH-1000XM5" } },
                    { "name": "Color", "values": { "p0": "Black", "p1": "Black", "p2": null } },
                    { "name": "Connectivity", "values": { "p0": "Wireless", "p1": "Wireless", "p2": "Wireless" } }
                  ]
                }
                """;
    }

    private static FeatureExtractionService serviceWith(LlmCaller caller) {
        return new FeatureExtractionService(caller);
    }

    // --- Happy path --------------------------------------------------------

    @Test
    void happyPathMergesLlmRowsWithGuaranteedRows() {
        FeatureExtractionService service = serviceWith(p -> wellFormedLlmResponse());

        FeatureExtractionRequest request = new FeatureExtractionRequest();
        request.setProducts(sampleProducts());

        FeatureExtractionResponse response = service.extract(request);

        assertEquals(expectedColumns(), response.getColumns());

        // Price and Condition come first (locally computed), LLM rows appended.
        assertEquals("Price", response.getFeatures().get(0).getName());
        assertEquals("Condition", response.getFeatures().get(1).getName());
        assertEquals("Brand", response.getFeatures().get(2).getName());
        assertEquals("Model", response.getFeatures().get(3).getName());
        assertEquals("Color", response.getFeatures().get(4).getName());
        assertEquals("Connectivity", response.getFeatures().get(5).getName());

        // Price row is keyed by column, not by retailer.
        FeatureRow price = response.getFeatures().get(0);
        assertEquals("$349.00", price.getValues().get("p0"));
        assertEquals("$329.00", price.getValues().get("p1"));
        assertEquals("$359.00", price.getValues().get("p2"));

        // Condition is "New" for all three because no name mentions renew/refurb.
        FeatureRow condition = response.getFeatures().get(1);
        for (Column column : response.getColumns()) {
            assertEquals("New", condition.getValues().get(column.getKey()));
        }

        // Brand is the LLM's contribution.
        FeatureRow brand = response.getFeatures().get(2);
        assertEquals("Sony", brand.getValues().get("p0"));
        assertEquals("Sony", brand.getValues().get("p1"));
        assertEquals("Sony", brand.getValues().get("p2"));

        // No warnings on a clean response.
        assertTrue(response.getWarnings() == null || response.getWarnings().isEmpty(),
                "Expected no warnings, got " + response.getWarnings());
    }

    @Test
    void nullValuesInLlmResponseArePreservedAsNull() {
        FeatureExtractionService service = serviceWith(p -> """
                { "features": [
                    { "name": "Color", "values": { "p0": "Black", "p1": null, "p2": "Black" } }
                ] }
                """);

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        FeatureRow color = response.getFeatures().stream()
                .filter(r -> r.getName().equals("Color"))
                .findFirst()
                .orElseThrow();
        assertEquals("Black", color.getValues().get("p0"));
        assertNull(color.getValues().get("p1"));
        assertEquals("Black", color.getValues().get("p2"));
    }

    // --- Same-retailer fixture -------------------------------------------

    @Test
    void twoProductsFromSameRetailerGetSeparateColumns() {
        // The headline reason for the refactor: selecting two products from
        // the same retailer must produce two columns with their own values,
        // not silently overwrite one with the other.
        List<PriceResult> products = List.of(
                product("Amazon", "Apple iPhone 15 128GB (Black, Unlocked)", "$799.00"),
                product("Amazon", "Apple iPhone 15 256GB (Blue, Unlocked)", "$899.00"),
                product("Walmart", "Apple iPhone 15 128GB (Black, Unlocked)", "$779.00")
        );

        FeatureExtractionService service = serviceWith(p -> """
                { "features": [
                    { "name": "Brand", "values": { "p0": "Apple", "p1": "Apple", "p2": "Apple" } },
                    { "name": "Model", "values": { "p0": "iPhone 15", "p1": "iPhone 15", "p2": "iPhone 15" } },
                    { "name": "Storage", "values": { "p0": "128GB", "p1": "256GB", "p2": "128GB" } },
                    { "name": "Color", "values": { "p0": "Black", "p1": "Blue", "p2": "Black" } }
                ] }
                """);

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(products));

        // Three columns: two Amazon, one Walmart. Same retailer, different keys.
        assertEquals(3, response.getColumns().size());
        assertEquals("Amazon", response.getColumns().get(0).getRetailerName());
        assertEquals("Amazon", response.getColumns().get(1).getRetailerName());
        assertEquals("Walmart", response.getColumns().get(2).getRetailerName());
        assertEquals("p0", response.getColumns().get(0).getKey());
        assertEquals("p1", response.getColumns().get(1).getKey());
        assertEquals("p2", response.getColumns().get(2).getKey());

        // Each column has its own price (no silent overwrite).
        FeatureRow price = response.getFeatures().get(0);
        assertEquals("$799.00", price.getValues().get("p0"));
        assertEquals("$899.00", price.getValues().get("p1"));
        assertEquals("$779.00", price.getValues().get("p2"));

        // The Storage row distinguishes the two Amazon products.
        FeatureRow storage = response.getFeatures().stream()
                .filter(r -> r.getName().equals("Storage"))
                .findFirst().orElseThrow();
        assertEquals("128GB", storage.getValues().get("p0"));
        assertEquals("256GB", storage.getValues().get("p1"));
        assertEquals("128GB", storage.getValues().get("p2"));
    }

    // --- Deduplication of guaranteed rows ----------------------------------

    @Test
    void llmRowsNamedPriceOrConditionAreDroppedSilentlyInFavorOfLocalRows() {
        FeatureExtractionService service = serviceWith(p -> """
                { "features": [
                    { "name": "Price", "values": { "p0": "wrong", "p1": "wrong", "p2": "wrong" } },
                    { "name": "condition", "values": { "p0": "wrong", "p1": "wrong", "p2": "wrong" } },
                    { "name": "Brand", "values": { "p0": "Sony", "p1": "Sony", "p2": "Sony" } }
                ] }
                """);

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        // Only one Price row, one Condition row, both locally computed.
        long priceCount = response.getFeatures().stream()
                .filter(r -> r.getName().equalsIgnoreCase("Price")).count();
        long conditionCount = response.getFeatures().stream()
                .filter(r -> r.getName().equalsIgnoreCase("Condition")).count();
        assertEquals(1, priceCount);
        assertEquals(1, conditionCount);

        // The local Price row still has the right value, not "wrong".
        FeatureRow price = response.getFeatures().stream()
                .filter(r -> r.getName().equals("Price"))
                .findFirst().orElseThrow();
        assertEquals("$349.00", price.getValues().get("p0"));

        // No warning is emitted: the dedup is silent so a successful
        // extraction doesn't look buggy to the user.
        List<String> warnings = response.getWarnings();
        assertTrue(warnings == null || warnings.isEmpty(),
                "Expected no warnings, got " + warnings);
    }

    // --- Defensive parsing -------------------------------------------------

    @Test
    void markdownFencedJsonIsStripped() {
        FeatureExtractionService service = serviceWith(p -> "```json\n" + wellFormedLlmResponse() + "\n```");

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        assertEquals(6, response.getFeatures().size());
        assertEquals("Brand", response.getFeatures().get(2).getName());
    }

    @Test
    void emptyLlmResponseProducesOnlyGuaranteedRowsAndWarns() {
        FeatureExtractionService service = serviceWith(p -> "");

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        assertEquals(2, response.getFeatures().size());
        assertEquals("Price", response.getFeatures().get(0).getName());
        assertEquals("Condition", response.getFeatures().get(1).getName());
        assertNotNull(response.getWarnings());
        assertFalse(response.getWarnings().isEmpty());
    }

    @Test
    void nullLlmResponseProducesOnlyGuaranteedRowsAndWarns() {
        FeatureExtractionService service = serviceWith(p -> null);

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        assertEquals(2, response.getFeatures().size());
        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().get(0).toLowerCase().contains("empty"));
    }

    @Test
    void malformedJsonProducesGuaranteedRowsAndWarning() {
        FeatureExtractionService service = serviceWith(p -> "{ this is not valid json }");

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        assertEquals(2, response.getFeatures().size());
        assertNotNull(response.getWarnings());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.toLowerCase().contains("failed to parse")));
    }

    @Test
    void missingFeaturesArrayProducesGuaranteedRowsAndWarning() {
        FeatureExtractionService service = serviceWith(p -> "{ \"rows\": [] }");

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        assertEquals(2, response.getFeatures().size());
        assertNotNull(response.getWarnings());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("features")));
    }

    @Test
    void malformedFeatureRowsAreSilentlyDropped() {
        FeatureExtractionService service = serviceWith(p -> """
                { "features": [
                    { "name": "Brand", "values": { "p0": "Sony" } },
                    { "name": "Broken" },
                    { "name": "Model", "values": { "p0": "WH-1000XM5" } }
                ] }
                """);

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(sampleProducts()));

        long llmRowCount = response.getFeatures().stream()
                .filter(r -> !r.getName().equals("Price") && !r.getName().equals("Condition"))
                .count();
        assertEquals(2, llmRowCount);
    }

    // --- Edge cases on the input list --------------------------------------

    @Test
    void emptyProductListReturnsEmptyResponseWithWarning() {
        FeatureExtractionService service = serviceWith(p -> wellFormedLlmResponse());

        FeatureExtractionRequest request = new FeatureExtractionRequest();
        request.setProducts(List.of());

        FeatureExtractionResponse response = service.extract(request);

        assertEquals(0, response.getColumns().size());
        assertEquals(0, response.getFeatures().size());
        assertNotNull(response.getWarnings());
        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().get(0).toLowerCase().contains("no products"));
    }

    @Test
    void nullProductListReturnsEmptyResponseWithWarning() {
        FeatureExtractionService service = serviceWith(p -> wellFormedLlmResponse());

        FeatureExtractionRequest request = new FeatureExtractionRequest();
        request.setProducts(null);

        FeatureExtractionResponse response = service.extract(request);

        assertEquals(0, response.getColumns().size());
        assertEquals(0, response.getFeatures().size());
        assertNotNull(response.getWarnings());
    }

    @Test
    void productsWithoutRetailerNameAreSkipped() {
        FeatureExtractionService service = serviceWith(p -> wellFormedLlmResponse());

        List<PriceResult> products = new ArrayList<>();
        products.add(product("Amazon", "Sony WH-1000XM5", "$349.00"));
        products.add(new PriceResult(null, "Bad Row", "$1.00", "USD", "https://example.com"));
        products.add(new PriceResult("", "Also Bad", "$2.00", "USD", "https://example.com"));

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(products));

        // Only Amazon is in the column set; the bad rows are skipped silently.
        assertEquals(1, response.getColumns().size());
        assertEquals("Amazon", response.getColumns().get(0).getRetailerName());
    }

    // --- Condition detection ----------------------------------------------

    @Test
    void conditionRowReflectsKeywordsInProductName() {
        FeatureExtractionService service = serviceWith(p -> "");

        List<PriceResult> products = List.of(
                product("Amazon", "Apple iPhone 15 (Renewed)", "$699.00"),
                product("Walmart", "Apple iPhone 15 - Refurbished", "$679.00"),
                product("Newegg", "Apple iPhone 15 Open Box", "$719.00"),
                product("eBay", "Apple iPhone 15 Used", "$649.00"),
                product("Etsy", "Apple iPhone 15", "$799.00")
        );

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(products));

        FeatureRow condition = response.getFeatures().stream()
                .filter(r -> r.getName().equals("Condition"))
                .findFirst().orElseThrow();
        assertEquals("Renewed", condition.getValues().get("p0"));
        assertEquals("Refurbished", condition.getValues().get("p1"));
        assertEquals("Open Box", condition.getValues().get("p2"));
        assertEquals("Used", condition.getValues().get("p3"));
        assertEquals("New", condition.getValues().get("p4"));
    }

    // --- LLM input contract -----------------------------------------------

    @Test
    void llmCallerReceivesTheOriginalProductsList() {
        AtomicReference<List<PriceResult>> captured = new AtomicReference<>();
        FeatureExtractionService service = serviceWith(p -> {
            captured.set(p);
            return wellFormedLlmResponse();
        });

        List<PriceResult> products = sampleProducts();
        service.extract(new FeatureExtractionRequest(products));

        assertEquals(products, captured.get());
    }

    @Test
    void priceRowPrependsDollarSignWhenCurrencyIsUsd() {
        FeatureExtractionService service = serviceWith(p -> "");

        List<PriceResult> products = List.of(
                new PriceResult("Amazon", "Widget", "49.99", "USD", "https://example.com"),
                new PriceResult("Walmart", "Widget", "$29.99", "USD", "https://example.com")
        );

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(products));

        FeatureRow price = response.getFeatures().get(0);
        assertEquals("$49.99", price.getValues().get("p0"));
        assertEquals("$29.99", price.getValues().get("p1")); // not "$$29.99"
    }

    @Test
    void nonUsdCurrencyIsPreservedAsPrefix() {
        FeatureExtractionService service = serviceWith(p -> "");

        List<PriceResult> products = List.of(
                new PriceResult("Etsy", "Bicycle", "120.00", "EUR", "https://example.com")
        );

        FeatureExtractionResponse response = service.extract(new FeatureExtractionRequest(products));

        FeatureRow price = response.getFeatures().get(0);
        assertEquals("EUR 120.00", price.getValues().get("p0"));
    }

    // --- Prompt regression guard ------------------------------------------

    @Test
    void extractFeaturesPromptIsStable() {
        // If the production prompt changes, this test fails on purpose. The
        // mock tests above use a fake caller, so the prompt's exact wording
        // is the only thing that actually ships through to the LLM. Lock it
        // down so accidental edits to the prompt are caught in CI.
        String prompt = PriceHawkPrompt.EXTRACT_FEATURES.prompt;
        assertTrue(prompt.contains("canonical set of discriminating features"),
                "Prompt should describe the canonicalisation goal");
        assertTrue(prompt.contains("Do NOT infer"),
                "Prompt should forbid the LLM from inventing values");
        assertTrue(prompt.contains("\"features\""),
                "Prompt should specify the JSON shape");
        assertTrue(prompt.contains("Brand"),
                "Prompt should require a Brand row");
        assertTrue(prompt.contains("Model"),
                "Prompt should require a Model row");
        assertTrue(prompt.contains("HARD RULES"),
                "Prompt should frame its hard rules prominently so the model treats them as a contract");
        assertTrue(prompt.contains("rejected"),
                "Prompt should warn the LLM that violations break the response");
        assertTrue(prompt.contains("DO NOT include a \"Price\" row"),
                "Prompt should explicitly forbid a Price row");
        assertTrue(prompt.contains("DO NOT include a \"Condition\" row"),
                "Prompt should explicitly forbid a Condition row");
        assertFalse(prompt.contains("Titanium"),
                "Prompt should not contain category-specific rules (kept generic on purpose)");
        // The example JSON should use column keys (p0, p1, p2) not retailer names.
        assertTrue(prompt.contains("\"p0\""),
                "Prompt example should use column keys (p0, p1, p2) not retailer names");
        assertFalse(prompt.contains("\"Amazon\""),
                "Prompt example should not key values by retailer name (column keys only)");
    }
}
