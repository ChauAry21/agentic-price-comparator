package com.agenticprice.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * One row of the feature comparison table. The feature name is the row label
 * (e.g. "Storage"); the values map is keyed by {@link Column#getKey()}, the
 * stable column identifier assigned by the backend. A null or empty value
 * means that product did not advertise that feature in the listing.
 *
 * Keys are column keys, not retailer names, so two products from the same
 * retailer get distinct values in the same row.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRow {
    private String name;
    private Map<String, String> values;
}
