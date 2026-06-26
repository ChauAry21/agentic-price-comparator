package com.agenticprice.tracking.controller;

import com.agenticprice.tracking.api.CreateTrackedQueryRequest;
import com.agenticprice.tracking.api.PriceHistoryResponse;
import com.agenticprice.tracking.api.TrackingCandidateResponse;
import com.agenticprice.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @PostMapping
    public ResponseEntity<TrackingCandidateResponse> trackQuery(@RequestBody CreateTrackedQueryRequest request) {
        return ResponseEntity.ok(trackingService.trackQuery(request.getEmail(), request.getRawQuery()));
    }

    @GetMapping
    public ResponseEntity<List<TrackingCandidateResponse>> getTrackedQueries(@RequestParam String email) {
        return ResponseEntity.ok(trackingService.getTrackedQueries(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrackedQuery(@RequestParam String email, @PathVariable UUID id) {
        trackingService.deleteTrackedQuery(email, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<PriceHistoryResponse>> getPriceHistory(
            @RequestParam String email,
            @PathVariable UUID id) {
        return ResponseEntity.ok(trackingService.getPriceHistory(email, id));
    }
}
