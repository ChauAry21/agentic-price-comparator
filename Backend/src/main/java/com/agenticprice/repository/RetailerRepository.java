package com.agenticprice.repository;

import com.agenticprice.model.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface RetailerRepository extends JpaRepository<Retailer, UUID> {
    Optional<Retailer> findByNameIgnoreCase(String name);
}