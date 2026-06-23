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
                                        "Return only the index number (0-based) of the best match, nothing else. "
                                        +
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
                                        """),

        EXTRACT_FEATURES(
                        """
                                        You are building a side-by-side comparison table for a shopper.
                                        You will receive a list of product listings for the SAME product category.
                                        Your job is to extract a canonical set of discriminating features
                                        and the value of each feature per product.

                                        Use a single, consistent vocabulary across all products. The same feature
                                        must have the same label everywhere. If one product says "Color: Black"
                                        and another says "Color: Midnight", pick one canonical label (e.g. "Color")
                                        and keep the raw retailer values as the cell values.

                                        Use consistent units. If one product says "128GB" and another says
                                        "0.128TB", prefer the unit used by the majority and rewrite the
                                        minority to match.

                                        Do NOT infer. If a feature is not explicitly stated in a product's
                                        title, return null for that product. Missing is honest, guessing is wrong.

                                        Do NOT invent features that only one product mentions unless they are
                                        genuinely discriminating (e.g. "Renewed" condition is fine even if only
                                        one listing mentions it, because it materially changes the comparison).

                                        Always include these rows if applicable to the category:
                                          - "Brand"
                                          - "Model"

                                        HARD RULES (violating any of these breaks the response):
                                        - DO NOT include a "Price" row. The backend adds Price from the
                                          listing's price field; if you emit one, the response is rejected.
                                        - DO NOT include a "Condition" row. The backend derives Condition
                                          from keywords in the product name (New / Renewed / Refurbished /
                                          Open Box / Used); if you emit one, the response is rejected.

                                        Input products are identified by stable column keys ("p0", "p1", "p2",
                                        ... in the order they appear in the input list). Use these keys in the
                                        "values" object of every feature row. The same key always refers to the
                                        same product, so two products from the same retailer are supported: each
                                        gets its own column key.

                                        Output format: return ONLY valid JSON in this exact shape:
                                        {
                                          "features": [
                                            { "name": "Brand", "values": { "p0": "Sony", "p1": "Sony" } },
                                            { "name": "Storage", "values": { "p0": "128GB", "p1": null } }
                                          ]
                                        }

                                        The keys in each "values" object are column keys ("p0", "p1", ...)
                                        matching the input. Use null for missing values, not empty string.

                                        Return JSON only. No prose, no markdown fences.
                                        """);

        public final String prompt;

        PriceHawkPrompt(String prompt) {
                this.prompt = prompt;
        }

        public String with(String input) {
                return this.prompt + input;
        }
}
