package com.agenticprice.tracking.service;

import com.agenticprice.scraper.PriceResult;
import com.agenticprice.tracking.api.PriceHistoryResponse;
import com.agenticprice.tracking.api.TrackingCandidateResponse;
import com.agenticprice.tracking.model.QueryPriceSnapshot;
import com.agenticprice.tracking.model.TrackedQuery;
import com.agenticprice.tracking.repository.QueryPriceSnapshotRepository;
import com.agenticprice.tracking.repository.TrackedQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final TrackedQueryRepository trackedQueryRepository;
    private final QueryPriceSnapshotRepository snapshotRepository;
    private final QueryResolutionService queryResolutionService;

    @Transactional
    public TrackingCandidateResponse trackQuery(String email, String rawQuery) {
        TrackedQuery tq = new TrackedQuery();
        tq.setEmail(email);
        tq.setRawQuery(rawQuery);
        tq = trackedQueryRepository.save(tq);
        return toResponse(tq);
    }

    public List<TrackingCandidateResponse> getTrackedQueries(String email) {
        return trackedQueryRepository.findByEmailAndActiveTrue(email)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteTrackedQuery(String email, UUID id) {
        trackedQueryRepository.findById(id).ifPresent(tq -> {
            if (tq.getEmail().equals(email)) {
                tq.setActive(false);
                trackedQueryRepository.save(tq);
            }
        });
    }

    public List<PriceHistoryResponse> getPriceHistory(String email, UUID trackedQueryId) {
        return trackedQueryRepository.findById(trackedQueryId)
                .filter(tq -> tq.getEmail().equals(email))
                .map(tq -> snapshotRepository.findByTrackedQueryOrderByScrapedAtDesc(tq)
                        .stream()
                        .map(this::toHistoryResponse)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public void runSnapshots() {
        List<TrackedQuery> active = trackedQueryRepository.findByActiveTrue();
        for (TrackedQuery tq : active) {
            try {
                List<PriceResult> results = queryResolutionService.resolve(tq.getRawQuery());
                for (PriceResult r : results) {
                    QueryPriceSnapshot snap = new QueryPriceSnapshot();
                    snap.setTrackedQuery(tq);
                    snap.setRetailerName(r.getRetailerName());
                    snap.setProductName(r.getProductName());
                    snap.setPrice(parsePrice(r.getPrice()));
                    snap.setCurrency(r.getCurrency() != null ? r.getCurrency() : "USD");
                    snap.setUrl(r.getUrl());
                    snap.setScrapedAt(OffsetDateTime.now());
                    snapshotRepository.save(snap);
                }
                tq.setLastCheckedAt(OffsetDateTime.now());
                trackedQueryRepository.save(tq);
            } catch (Exception e) {
                log.error("Tracking snapshot failed for query '{}': {}", tq.getRawQuery(), e.getMessage());
            }
        }
    }

    private BigDecimal parsePrice(String priceStr) {
        try {
            return new BigDecimal(priceStr.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private TrackingCandidateResponse toResponse(TrackedQuery tq) {
        TrackingCandidateResponse r = new TrackingCandidateResponse();
        r.setId(tq.getId());
        r.setRawQuery(tq.getRawQuery());
        r.setActive(tq.isActive());
        return r;
    }

    private PriceHistoryResponse toHistoryResponse(QueryPriceSnapshot snap) {
        PriceHistoryResponse r = new PriceHistoryResponse();
        r.setRetailerName(snap.getRetailerName());
        r.setProductName(snap.getProductName());
        r.setPrice(snap.getPrice());
        r.setCurrency(snap.getCurrency());
        r.setUrl(snap.getUrl());
        r.setScrapedAt(snap.getScrapedAt());
        return r;
    }
}
