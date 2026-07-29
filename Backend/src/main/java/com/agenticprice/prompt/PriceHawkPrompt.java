package com.agenticprice.prompt;

public enum PriceHawkPrompt {

        EXTRACT_PRICE(
        """
        Extract pricing from this product listing HTML.

        Return ONLY valid JSON (no markdown fences, no comments, no trailing
        commas, no extra fields) with EXACTLY this shape and types:

        {
          "price":    <number>,
          "currency": "<ISO 4217 code>",
          "financed": <true|false>
        }

        Rules:
        - One-time purchase: price = the single total number; financed = false.
        - Financed / recurring plan (e.g. "$10.75/month with $791 down for 24 months"):
          price = (monthly * term_months) + down_payment, rounded to 2 decimals,
          and MUST be the TOTAL cost. The example above -> price = 1049.00.
        - Never include shipping, taxes, or promotional credits in price.
        - Never convert units (do not turn cents into dollars, do not turn
          monthly into yearly, do not strip a down payment).
        - No price on the page at all:
            {"price": null, "currency": null, "financed": false}

        HTML: """),
        
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

                                        Primary vs. secondary products: identify the primary product the user is searching for.
                                        A result is "primary" if it IS that product (e.g. "iPhone 17 Pro 256GB").
                                        A result is "secondary" if it is FOR, COMPATIBLE WITH, or an ACCESSORY to that product
                                        (cases, mounts, cables, screen protectors, replacement parts, bags, stands, chargers).
                                        Secondary products always rank at the very bottom of the list, after every primary product,
                                        regardless of price, retailer, or input position. No primary product may appear below a secondary one.

                                        Worked example of the primary/secondary rule:
                                        Input indices and titles:
                                          [0] "iPhone 17 Pro Silicone Case with MagSafe"   (secondary: case FOR iPhone 17 Pro)
                                          [1] "2 Pack iPhone 17 Pro Max Screen Protector"   (secondary: protector FOR iPhone)
                                          [2] "Apple iPhone 17 Pro, 256GB, Unlocked"        (primary: IS the iPhone 17 Pro)
                                          [3] "Camera Lens Protector for iPhone 17 Pro"    (secondary: protector FOR iPhone)
                                          [4] "Apple iPhone 17 Pro, 512GB, Silver"         (primary: IS the iPhone 17 Pro)
                                        Expected output: [2, 4, 0, 1, 3]
                                        All primary products (indices 2, 4) come first, every secondary (0, 1, 3) after.

                                        Numeric signal preference: when two results are otherwise equally relevant, prefer
                                        the one whose title includes numeric measures on the queried attributes.
                                        Do not prefer a result solely because it has more digits in its title.
                                        Example: Query "Earbuds with long battery life." Rank "Airpod pro 2 60H battery"
                                        over "Airpod pro 2 long battery life".

                                        Tie-break by price (lower first) only among results in the same category.
                                        Do not price-rank a primary product below a secondary/accessory one.
                                        If there are no explicit preferences in the query, sort by general semantic fit,
                                        then by price within the same category.

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
