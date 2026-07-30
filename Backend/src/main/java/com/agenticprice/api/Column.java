package com.agenticprice.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identifies a single product in the comparison table. The {@code key} is a
 * stable identifier the LLM uses when populating row values; the
 * {@code retailerName} and {@code productName} are display labels for the
 * column header. The two-product-from-same-retailer case is supported
 * because each product gets its own Column. {@code financed} lets the UI
 * render a "Financed" badge next to the price cell in the comparison
 * table without re-fetching the underlying listing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    private String key;
    private String retailerName;
    private String productName;
    private boolean financed;
}
