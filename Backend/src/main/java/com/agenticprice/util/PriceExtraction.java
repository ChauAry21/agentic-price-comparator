package com.agenticprice.util;

import java.math.BigDecimal;

/**
 * Result of parsing the structured JSON returned by EXTRACT_PRICE.
 *
 * - price    : headline numeric price (one-time total, or computed total for financed plans)
 * - currency : ISO 4217 code, or null when unknown
 * - financed : true when this is a recurring installment plan
 */
public record PriceExtraction(
        BigDecimal price,
        String currency,
        boolean financed) {
}
