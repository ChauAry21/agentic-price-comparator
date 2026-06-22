package com.agenticprice.api;

import com.agenticprice.scraper.PriceResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for POST /api/prices/extract-features. The caller passes the
 * product listings the user has selected for side-by-side comparison, and the
 * backend returns a canonicalised feature table.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureExtractionRequest {
    private List<PriceResult> products;
}
