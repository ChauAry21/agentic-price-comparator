package com.agenticprice.tracking.api;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.annotation.Nullable;

@Data
public class PriceHistoryResponse {
    private UUID trackedQueryId;
    private String canonicalProductName;
    private List<PriceHistoryPoint> points;

    @Data
    public static class PriceHistoryPoint {
        private OffsetDateTime scrapedAt;
        private @Nullable BigDecimal bestMatchedPrice;
        private String bestRetailer;
        private List<RetailerPrice> retailerPrices;
    }

    @Data
    public static class RetailerPrice {
        private String retailerName;
        private BigDecimal price;
        private boolean matched;
    }
}
