package com.agenticprice.tracking.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

import com.agenticprice.tracking.service.TrackingService;
import com.agenticprice.tracking.api.CreateTrackedQueryRequest;
import com.agenticprice.tracking.model.TrackedQuery;
import com.agenticprice.tracking.service.QueryResolutionService;
import com.agenticprice.tracking.api.TrackingCandidateResponse;
import com.agenticprice.tracking.api.PriceHistoryResponse;


@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {
    private final TrackingService trackingService;
    private final QueryResolutionService queryResolutionService;

    @GetMapping("/candidates")
    public ResponseEntity<TrackingCandidateResponse> getCandidates(@RequestParam String query){
        return ResponseEntity.ok(queryResolutionService.getCandidates(query));
    }

    @PostMapping("/queries")
    public ResponseEntity<TrackedQuery> createTrackedQuery(@RequestHeader("X-User-Email") String email, @RequestBody CreateTrackedQueryRequest request) {
        return ResponseEntity.ok(trackingService.createTrackedQuery(email, request));
    }

    @GetMapping("/queries/{id}/history")
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(@RequestHeader("X-User-Email") String email, @PathVariable UUID id, @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(trackingService.getPriceHistory(email, id, days));
    }

    @DeleteMapping("/queries/{id}")
    public ResponseEntity<Void> deleteTrackedQuery(@RequestHeader("X-User-Email") String email, @PathVariable UUID id) {
        trackingService.deleteTrackedQuery(email, id);
        return ResponseEntity.noContent().build();
    }
}
