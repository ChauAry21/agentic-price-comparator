package com.agenticprice.tracking.model;

import jakarta.persistence.*;
import lombok.Data;
import com.agenticprice.model.Retailer;
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

    @ManyToOne
    @JoinColumn(name = "tracked_query_id", nullable = false)
    private TrackedQuery trackedQuery;

    @ManyToOne(optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    private Retailer retailer;

    @Column(nullable = false)
    private BigDecimal price;

    @Column
    private String currency;

    @Column
    private String url;

    @Column(nullable = false)
    private boolean matched;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_method", nullable = false)
    private MatchMethod matchMethod;
    
    @Column(name = "scraped_at")
    private OffsetDateTime scrapedAt;
}
