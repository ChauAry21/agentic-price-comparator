package com.agenticprice.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Tolerant parser for the JSON the EXTRACT_PRICE prompt returns.
 *
 * Returns null when the LLM response clearly indicates "no price found"
 * so callers can drop the listing entirely. Falls back to a numeric
 * regex strip for legacy callers that pass a bare price string.
 */
public final class PriceExtractionParser {

    private static final Pattern NON_NUMERIC = Pattern.compile("[^0-9.]");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PriceExtractionParser() {
    }

    public static PriceExtraction parse(String rawLlmResponse) {
        if (rawLlmResponse == null) return null;
        String trimmed = rawLlmResponse.trim();
        if (trimmed.isEmpty()) return null;

        // Strip markdown fences if present.
        String stripped = stripFences(trimmed);

        // Try the structured JSON path first.
        try {
            JsonNode node = MAPPER.readTree(stripped);
            if (node.isObject()) {
                JsonNode priceNode = node.get("price");
                String currency = textOrNull(node.get("currency"));
                boolean financed = node.has("financed") && node.get("financed").asBoolean(false);

                if (priceNode == null || priceNode.isNull()) {
                    // No numeric price in an otherwise-valid JSON response = no price.
                    return null;
                }
                BigDecimal maybePrice = priceNode.decimalValue();
                // Treat 0 the same as null: GPT often encodes "missing" as a
                // literal 0 to satisfy the numeric type. We can't rank or
                // display a free $0.00 listing without misleading users.
                if (maybePrice.signum() == 0) {
                    return null;
                }
                return new PriceExtraction(maybePrice, currency, financed);


            }
        } catch (Exception ignored) {
            // Fall through to legacy parsing.
        }

        // Legacy path: the LLM returned a plain price string.
        BigDecimal price;
        String cleaned = NON_NUMERIC.matcher(trimmed).replaceAll("");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            price = new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return new PriceExtraction(price, "USD", false);
    }

    private static String stripFences(String s) {
        if (!s.startsWith("```")) return s;
        int firstNl = s.indexOf('\n');
        if (firstNl < 0) return s;
        String body = s.substring(firstNl + 1);
        if (body.endsWith("```")) {
            body = body.substring(0, body.length() - 3);
        }
        return body.trim();
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String s = node.asText();
        return s.isBlank() ? null : s;
    }
}
