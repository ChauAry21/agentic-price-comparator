package com.agenticprice.scraper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceResult {
    private String retailerName;
    private String productName;
    private String price;
    private String currency;
    private String url;
}