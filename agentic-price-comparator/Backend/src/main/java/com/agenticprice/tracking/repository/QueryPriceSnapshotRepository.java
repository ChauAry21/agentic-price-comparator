package com.agenticprice.tracking.repository;

import com.agenticprice.tracking.model.QueryPriceSnapshot;
import com.agenticprice.tracking.model.TrackedQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QueryPriceSnapshotRepository extends JpaRepository<QueryPriceSnapshot, UUID> {

    List<QueryPriceSnapshot> findByTrackedQueryOrderByScrapedAtDesc(TrackedQuery trackedQuery);
}
