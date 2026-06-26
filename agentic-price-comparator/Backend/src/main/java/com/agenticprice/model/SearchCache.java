package com.agenticprice.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "search_cache")
public class SearchCache {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String query;

    @Column(name = "response_json", nullable = false, columnDefinition = "text")
    private String responseJson;

    @Column(name = "cached_at")
    private OffsetDateTime cachedAt;
}