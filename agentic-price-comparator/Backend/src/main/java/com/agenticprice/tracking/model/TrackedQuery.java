package com.agenticprice.tracking.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "tracked_queries")
public class TrackedQuery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "raw_query", nullable = false)
    private String rawQuery;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(nullable = false)
    private boolean active = true;
}
