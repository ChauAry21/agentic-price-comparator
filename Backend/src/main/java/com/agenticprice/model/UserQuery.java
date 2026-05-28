package com.agenticprice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_queries")
public class UserQuery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "raw_query", nullable = false)
    private String rawQuery;

    @ManyToOne
    @JoinColumn(name = "resolved_product_id")
    private Product resolvedProduct;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}