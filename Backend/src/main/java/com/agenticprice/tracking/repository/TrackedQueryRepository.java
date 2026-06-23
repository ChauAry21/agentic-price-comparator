package com.agenticprice.tracking.repository;

import java.util.UUID;
import java.util.List;
import com.agenticprice.tracking.model.TrackedQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.agenticprice.tracking.model.TrackingStatus;

public interface TrackedQueryRepository extends JpaRepository<TrackedQuery, UUID> {
    List<TrackedQuery> findByUserIdAndStatus(UUID userId, TrackingStatus status);
    List<TrackedQuery> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<TrackedQuery> findByIdAndUserId(UUID id, UUID userId);
}
