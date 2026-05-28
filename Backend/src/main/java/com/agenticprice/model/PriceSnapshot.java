package com.agenticprice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "price_snapshots")
public class PriceSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "retailer_id")
    private Retailer retailer;

    @Column(nullable = false)
    private BigDecimal price;

    @Column
    private String currency;

    @Column
    private String url;

    @Column(name = "scraped_at")
    private OffsetDateTime scrapedAt;
}