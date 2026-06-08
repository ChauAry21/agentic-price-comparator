package com.agenticprice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "price_alerts")
public class PriceAlert {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "product_query", nullable = false)
    private String productQuery;

    @Column(name = "threshold_price", nullable = false)
    private BigDecimal thresholdPrice;

    @Column(nullable = false)
    private String email;

    @Column
    private boolean active = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "triggered_at")
    private OffsetDateTime triggeredAt;
}