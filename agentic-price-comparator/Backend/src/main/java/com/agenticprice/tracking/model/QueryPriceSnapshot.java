package com.agenticprice.tracking.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "query_price_snapshots")
public class QueryPriceSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_query_id", nullable = false)
    private TrackedQuery trackedQuery;

    @Column(name = "retailer_name", nullable = false)
    private String retailerName;

    @Column(name = "product_name")
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column
    private String currency = "USD";

    @Column
    private String url;

    @Column(name = "scraped_at", nullable = false)
    private OffsetDateTime scrapedAt = OffsetDateTime.now();
}
