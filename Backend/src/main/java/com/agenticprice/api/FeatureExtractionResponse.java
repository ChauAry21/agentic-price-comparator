package com.agenticprice.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response body for POST /api/prices/extract-features. The {@code columns}
 * list is the column set the frontend should render; each column corresponds
 * to a single product the user selected, identified by a stable {@code key}.
 * The {@code features} list is the rows (one entry per discriminating
 * feature); each row's {@code values} map is keyed by column key, not by
 * retailer name, so two products from the same retailer get two separate
 * columns with their own values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureExtractionResponse {
    private List<Column> columns;
    private List<FeatureRow> features;
    private List<String> warnings;
}
