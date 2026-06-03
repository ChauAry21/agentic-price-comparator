package com.agenticprice.repository;

import com.agenticprice.model.SearchCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SearchCacheRepository extends JpaRepository<SearchCache, UUID> {
    Optional<SearchCache> findByQueryIgnoreCase(String query);
    void deleteByQueryIgnoreCase(String query);
    void deleteByCachedAtBefore(OffsetDateTime cutoff);
}