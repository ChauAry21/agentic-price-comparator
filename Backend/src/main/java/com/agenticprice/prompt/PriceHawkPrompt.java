package com.agenticprice.prompt;

public enum PriceHawkPrompt {

    EXTRACT_PRICE(
            "Extract the product price from this HTML. " +
            "Return only the numeric price value and currency symbol (e.g. $29.99), nothing else. " +
            "If no price is found, return PRICE_NOT_FOUND. HTML: "
    ),

    PARSE_QUERY(
            "Parse this product search query and return a clean product name suitable for searching retail websites. " +
            "Return only the product name, nothing else. Query: "
    ),

    MATCH_PRODUCT(
            "Given a target product name and a list of search results, identify which result best matches the target product. " +
            "Return only the index number (0-based) of the best match, nothing else. " +
            "If no result is a good match, return -1. Target: "
    ),

    NORMALIZE_PRODUCT_NAME(
            "Normalize this product name into a clean, canonical form suitable for deduplication across retailers. " +
            "Remove retailer-specific suffixes, promotional text, and irrelevant details. " +
            "Return only the normalized name. Product: "
    ),

    EXTRACT_PRODUCT_URL(
            "Extract the direct product page URL from this HTML snippet. " +
            "Return only the URL, nothing else. If no URL is found, return URL_NOT_FOUND. HTML: "
    );

    public final String prompt;

    PriceHawkPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String with(String input) {
        return this.prompt + input;
    }
}