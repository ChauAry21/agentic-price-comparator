package com.agenticprice.repository;

import com.agenticprice.model.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, UUID> {
    List<PriceAlert> findByActiveTrue();
}