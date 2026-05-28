package com.agenticprice.repository;

import com.agenticprice.model.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, UUID> {
    List<PriceSnapshot> findByProductId(UUID productId);
    List<PriceSnapshot> findByProductIdOrderByScrapedAtDesc(UUID productId);
}