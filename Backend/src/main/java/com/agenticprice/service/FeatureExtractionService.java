package com.agenticprice.service;

import com.agenticprice.api.Column;
import com.agenticprice.api.FeatureExtractionRequest;
import com.agenticprice.api.FeatureExtractionResponse;
import com.agenticprice.api.FeatureRow;
import com.agenticprice.scraper.PriceResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds a feature comparison table for a list of product listings the user
 * has selected. The LLM is invoked through a {@link LlmCaller} seam so the
 * parsing, validation, and merging logic can be unit-tested without
 * contacting OpenAI.
 *
 * The response is column-oriented: each selected product gets its own
 * {@link Column} (keyed by a stable identifier), and every feature row's
 * values map is keyed by column key rather than by retailer name. This
 * supports the case where the user selects two products from the same
 * retailer; both get separate columns with their own values.
 */
@Slf4j
@Service
public class FeatureExtractionService {

    private final LlmCaller llmCaller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public FeatureExtractionService(OpenAIService openAIService) {
        this(openAIService::extractFeatures);
    }

    /**
     * Test-friendly constructor. Production wiring goes through the
     * {@link OpenAIService}-taking constructor above (marked @Autowired);
     * tests pass a deterministic fake here.
     */
    public FeatureExtractionService(LlmCaller llmCaller) {
        this.llmCaller = llmCaller;
    }

    public FeatureExtractionResponse extract(FeatureExtractionRequest request) {
        List<PriceResult> products = request.getProducts();
        if (products == null || products.isEmpty()) {
            FeatureExtractionResponse empty = new FeatureExtractionResponse();
            empty.setColumns(List.of());
            empty.setFeatures(List.of());
            empty.setWarnings(List.of("No products provided"));
            return empty;
        }

        List<Column> columns = new ArrayList<>();
        Map<String, String> priceRow = new LinkedHashMap<>();
        Map<String, String> conditionRow = new LinkedHashMap<>();
        for (int i = 0; i < products.size(); i++) {
            PriceResult p = products.get(i);
            String retailer = p.getRetailerName();
            if (retailer == null || retailer.isBlank())
                continue;
            String key = "p" + i;
            columns.add(new Column(key, retailer, p.getProductName()));
            priceRow.put(key, formatPrice(p));
            conditionRow.put(key, detectCondition(p.getProductName()));
        }

        List<FeatureRow> features = new ArrayList<>();
        features.add(new FeatureRow("Price", priceRow));
        features.add(new FeatureRow("Condition", conditionRow));

        List<String> warnings = new ArrayList<>();
        String raw = llmCaller.call(products);
        if (raw == null || raw.isBlank()) {
            warnings.add("LLM returned empty response");
        } else {
            try {
                List<FeatureRow> llmRows = parseLlmResponse(raw, columns);
                mergeRows(features, llmRows, columns, warnings);
            } catch (Exception e) {
                log.warn("Failed to parse LLM feature response: {}", e.getMessage());
                warnings.add("Failed to parse LLM response: " + e.getMessage());
            }
        }

        FeatureExtractionResponse response = new FeatureExtractionResponse();
        response.setColumns(columns);
        response.setFeatures(features);
        response.setWarnings(warnings);
        return response;
    }

    private String formatPrice(PriceResult p) {
        String price = p.getPrice();
        if (price == null || price.isBlank())
            return null;
        String currency = p.getCurrency();
        if (currency == null || currency.isBlank() || "USD".equalsIgnoreCase(currency)) {
            return price.startsWith("$") ? price : "$" + price;
        }
        return currency + " " + price;
    }

    private String detectCondition(String productName) {
        if (productName == null)
            return "New";
        String lower = productName.toLowerCase(Locale.ROOT);
        if (lower.contains("renewed"))
            return "Renewed";
        if (lower.contains("refurbished"))
            return "Refurbished";
        if (lower.contains("open box"))
            return "Open Box";
        if (lower.contains("used") || lower.contains("pre-owned"))
            return "Used";
        return "New";
    }

    /**
     * Parses the LLM's JSON response into a list of {@link FeatureRow}.
     * Strips markdown fences defensively (some models wrap the JSON) and
     * silently drops malformed rows so a single bad entry doesn't sink the
     * whole response. The values map preserves whatever keys the LLM
     * emitted; mergeRows is responsible for normalising those to the
     * canonical column-key set.
     */
    private List<FeatureRow> parseLlmResponse(String raw, List<Column> columns) throws Exception {
        String cleaned = stripMarkdownFences(raw);

        JsonNode root = objectMapper.readTree(cleaned);
        JsonNode featuresNode = root.get("features");
        if (featuresNode == null || !featuresNode.isArray()) {
            throw new IllegalStateException("Response missing 'features' array");
        }

        List<FeatureRow> rows = new ArrayList<>();
        for (JsonNode node : featuresNode) {
            JsonNode nameNode = node.get("name");
            JsonNode valuesNode = node.get("values");
            if (nameNode == null || !nameNode.isTextual() || valuesNode == null || !valuesNode.isObject()) {
                continue;
            }
            Map<String, String> values = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = valuesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode v = field.getValue();
                if (v == null || v.isNull()) {
                    values.put(field.getKey(), null);
                } else if (v.isTextual()) {
                    values.put(field.getKey(), v.asText());
                } else {
                    values.put(field.getKey(), v.toString());
                }
            }
            rows.add(new FeatureRow(nameNode.asText(), values));
        }
        return rows;
    }

    private String stripMarkdownFences(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline > 0)
                cleaned = cleaned.substring(firstNewline + 1);
            if (cleaned.endsWith("```"))
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            cleaned = cleaned.trim();
        }
        return cleaned;
    }

    private void mergeRows(List<FeatureRow> target, List<FeatureRow> incoming,
            List<Column> columns, List<String> warnings) {
        // The backend computes Price and Condition locally, so any Price or
        // Condition row from the LLM is dropped silently. The prompt asks
        // the LLM not to emit them, and mergeRows is a defensive backstop;
        // If the LLM's values ever disagree with the local
        // computation, the local answer wins.
        for (FeatureRow row : incoming) {
            String name = row.getName();
            if (name == null)
                continue;
            String normalized = name.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("price") || normalized.equals("condition")) {
                continue;
            }

            Map<String, String> normalizedValues = new LinkedHashMap<>();
            for (Column column : columns) {
                String value = row.getValues() == null ? null : row.getValues().get(column.getKey());
                if (value != null && !value.isBlank()) {
                    normalizedValues.put(column.getKey(), value.trim());
                } else {
                    normalizedValues.put(column.getKey(), null);
                }
            }
            target.add(new FeatureRow(name.trim(), normalizedValues));
        }
    }
}
