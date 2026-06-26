package com.agenticprice.tracking.repository;

import com.agenticprice.tracking.model.TrackedQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackedQueryRepository extends JpaRepository<TrackedQuery, UUID> {

    List<TrackedQuery> findByEmailAndActiveTrue(String email);

    List<TrackedQuery> findByActiveTrue();
}
