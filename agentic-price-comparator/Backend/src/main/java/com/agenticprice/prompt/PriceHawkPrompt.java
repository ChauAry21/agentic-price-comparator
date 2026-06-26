package com.agenticprice.prompt;

public enum PriceHawkPrompt {

        EXTRACT_PRICE(
                        "Extract the product price from this HTML. " +
                                        "Return only the numeric price value and currency symbol (e.g. $29.99), nothing else. "
                                        +
                                        "If no price is found, return PRICE_NOT_FOUND. HTML: "),

        PARSE_QUERY(
                        "Parse this product search query and return a clean product name suitable for searching retail websites. "
                                        +
                                        "Return only the product name, nothing else. Query: "),

        MATCH_PRODUCT(
                        "Given a target product name and a list of search results, identify which result best matches the target product. "
                                        +
                                        "Return only the index number (0-based) of the best match, nothing else. " +
                                        "If no result is a good match, return -1. Target: "),

        NORMALIZE_PRODUCT_NAME(
                        "Normalize this product name into a clean, canonical form suitable for deduplication across retailers. "
                                        +
                                        "Remove retailer-specific suffixes, promotional text, and irrelevant details. "
                                        +
                                        "Return only the normalized name. Product: "),

        EXTRACT_PRODUCT_URL(
                        "Extract the direct product page URL from this HTML snippet. " +
                                        "Return only the URL, nothing else. If no URL is found, return URL_NOT_FOUND. HTML: "),
        RANK_PRODUCTS(
                        """
                                        Rank these products by overall semantic fit to the query, not by exact word overlap.

                                        Prefer products whose attributes, specs, and intent match the query best.
                                        If the query includes explicit preferences or constraints, treat them as high-priority signals.
                                        Treat paraphrases and equivalent feature descriptions as matching signals.
                                        Example: "Laptop with good memory" would favor results with "32 GB ram" in product_name over "16 GB memory".

                                        Favor products with numeric measures on fields on interest.
                                        Example: Query: "Earbuds with long battery life." Rank "Airpod pro 2 60H battery" over "Airpod pro 2 long battery life".
                                        If there are no explicit request, sort based on general semantic fit, then favor lower prices.

                                        Return only valid JSON in this exact format:
                                        {
                                          "ordered_indices": [2, 0, 1]
                                        }

                                        Rules:
                                        - Use 0-based indices
                                        - Include every product exactly once
                                        - Return JSON only
                                        """);

        public final String prompt;

        PriceHawkPrompt(String prompt) {
                this.prompt = prompt;
        }

        public String with(String input) {
                return this.prompt + input;
        }
}