package com.agenticprice.tracking.repository;

import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.agenticprice.tracking.model.QueryPriceSnapshot;
import java.time.OffsetDateTime;

public interface QueryPriceSnapshotRepository extends JpaRepository<QueryPriceSnapshot, UUID> {
    List<QueryPriceSnapshot> findByTrackedQueryIdOrderByScrapedAtAsc(UUID trackedQueryId);
    List<QueryPriceSnapshot> findByTrackedQueryIdAndScrapedAtAfterOrderByScrapedAtAsc(UUID id, OffsetDateTime after);
}
