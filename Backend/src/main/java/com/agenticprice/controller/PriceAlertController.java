package com.agenticprice.controller;

import com.agenticprice.api.CreateAlertRequest;
import com.agenticprice.model.PriceAlert;
import com.agenticprice.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @PostMapping
    public ResponseEntity<PriceAlert> createAlert(@RequestBody CreateAlertRequest request) {
        PriceAlert alert = priceAlertService.createAlert(
                request.getProductQuery(),
                request.getThresholdPrice(),
                request.getEmail()
        );
        return ResponseEntity.ok(alert);
    }

    @GetMapping
    public ResponseEntity<List<PriceAlert>> getAlerts() {
        return ResponseEntity.ok(priceAlertService.getAllAlerts());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable UUID id) {
        priceAlertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}