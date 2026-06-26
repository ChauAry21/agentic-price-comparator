package com.agenticprice.tracking.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PriceHistoryResponse {
    private String retailerName;
    private String productName;
    private BigDecimal price;
    private String currency;
    private String url;
    private OffsetDateTime scrapedAt;
}
