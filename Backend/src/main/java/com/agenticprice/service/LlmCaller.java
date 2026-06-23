package com.agenticprice.service;

import com.agenticprice.scraper.PriceResult;

import java.util.List;

/**
 * Thin seam between the feature extractor and the LLM client. Lets unit tests
 * inject a hand-rolled fake without dragging in the real OpenAI client
 * (which requires an API key at construction time).
 */
@FunctionalInterface
public interface LlmCaller {
    String call(List<PriceResult> products);
}
