package com.agenticprice.scraper;

import lombok.Data;

@Data
public class PriceResult {
    private String retailerName;
    private String productName;
    private String price;
    private String currency;
    private String url;
    private boolean financed;

    public PriceResult() {
    }

    public PriceResult(String retailerName,
                       String productName,
                       String price,
                       String currency,
                       String url,
                       boolean financed) {
        this.retailerName = retailerName;
        this.productName = productName;
        this.price = price;
        this.currency = currency;
        this.url = url;
        this.financed = financed;
    }

    // Convenience overload for callers that don't know whether a listing is
    // financed (treated as one-time price).
    public PriceResult(String retailerName,
                       String productName,
                       String price,
                       String currency,
                       String url) {
        this(retailerName, productName, price, currency, url, false);
    }
}
