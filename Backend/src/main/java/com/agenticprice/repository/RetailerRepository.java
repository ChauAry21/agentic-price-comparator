package com.agenticprice.repository;

import com.agenticprice.model.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RetailerRepository extends JpaRepository<Retailer, UUID> {
}