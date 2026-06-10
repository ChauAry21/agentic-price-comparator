package com.agenticprice.tracking.model;

import jakarta.persistence.*;
import lombok.Data;
import com.agenticprice.model.User;
import com.agenticprice.model.Product;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tracked_queries")
public class TrackedQuery {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "rawQuery", nullable = false)
    private String rawQuery;

    @Column(name = "parsedQuery", nullable = false)
    private String parsedQuery;

    @Column(name = "canonicalProductKey", nullable = false)
    private String canonicalProductKey;

    @Column(name = "canonicalProductName", nullable = false)
    private String canonicalProductName;

    @Column(name = "referenceUrl", nullable = false)
    private String referenceUrl;

    @ManyToOne
    @JoinColumn(name = "resolved_product_id")
    private Product resolvedProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrackingStatus status;

    @Column(name = "createdAt")
    private OffsetDateTime createdAt;

    @Column(name = "lastScrapedAt")
    private OffsetDateTime lastScrapedAt;
}
