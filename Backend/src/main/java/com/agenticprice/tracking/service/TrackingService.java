package com.agenticprice.tracking.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

import com.agenticprice.model.User;
import com.agenticprice.repository.UserRepository;
import com.agenticprice.service.OpenAIService;
import com.agenticprice.service.ScraperService;
import com.agenticprice.scraper.PriceResult;
import com.agenticprice.model.Retailer;
import com.agenticprice.repository.RetailerRepository;

import com.agenticprice.tracking.model.TrackedQuery;
import com.agenticprice.tracking.model.TrackingStatus;
import com.agenticprice.tracking.api.CreateTrackedQueryRequest;
import com.agenticprice.tracking.model.QueryPriceSnapshot;
import com.agenticprice.tracking.api.PriceHistoryResponse;
import com.agenticprice.tracking.repository.QueryPriceSnapshotRepository;
import com.agenticprice.tracking.service.QueryResolutionService;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.agenticprice.tracking.repository.TrackedQueryRepository;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class TrackingService {
    private final ScraperService scraperService;
    private final OpenAIService openAIService;
    private final RetailerRepository retailerRepository;
    private final UserRepository userRepository;
    private final TrackedQueryRepository trackedQueryRepository;
    private final QueryPriceSnapshotRepository queryPriceSnapshotRepository;
    private final QueryResolutionService queryResolutionService;
    
    public User resolveUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public TrackedQuery createTrackedQuery(String email, CreateTrackedQueryRequest request) {
        //resolve user
        User user = resolveUser(email);

        //parse query
        String parsedQuery = openAIService.parseQuery(request.getRawQuery());

        //Build tracked query
        TrackedQuery trackedQuery = new TrackedQuery();
        trackedQuery.setUser(user);
        trackedQuery.setRawQuery(request.getRawQuery());
        trackedQuery.setParsedQuery(parsedQuery);
        trackedQuery.setCanonicalProductKey(request.getCanonicalProductKey());
        trackedQuery.setCanonicalProductName(request.getCanonicalProductName());
        trackedQuery.setReferenceUrl(request.getReferenceUrl());
        trackedQuery.setStatus(TrackingStatus.ACTIVE);
        trackedQuery.setCreatedAt(OffsetDateTime.now());

        //Save tracked query
        return trackedQueryRepository.save(trackedQuery);
    }

    // Return user's queries orded by created at descending
    public List<TrackedQuery> listTrackedQueries(String email) {
        User user = resolveUser(email);
        return trackedQueryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    // Find a tracked query by id, delete or throw 404
    public void deleteTrackedQuery(String email, UUID id) {
        User user = resolveUser(email);
        TrackedQuery trackedQuery = trackedQueryRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracked query not found"));
        trackedQueryRepository.delete(Objects.requireNonNull(trackedQuery));
    }

    // Price History for a tracked query
    public PriceHistoryResponse getPriceHistory(String email, UUID id, int days) {
        // Load tracked query
        User user = resolveUser(email);
        TrackedQuery trackedQuery = trackedQueryRepository.findByIdAndUserId(id, user.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tracked query not found"));

        // Load price snapshots for last N days
        OffsetDateTime after = OffsetDateTime.now().minusDays(days);
        List<QueryPriceSnapshot> snapshots = queryPriceSnapshotRepository.findByTrackedQueryIdAndScrapedAtAfterOrderByScrapedAtAsc(id, after);
        
        // Group by scrapedAt (same timestamp bucket per scrape run)
        Map<OffsetDateTime, List<QueryPriceSnapshot>> groupedSnapshots = snapshots.stream()
            .collect(Collectors.groupingBy(QueryPriceSnapshot::getScrapedAt));

        // Build response
        PriceHistoryResponse response = new PriceHistoryResponse();
        response.setTrackedQueryId(trackedQuery.getId());
        response.setCanonicalProductName(trackedQuery.getCanonicalProductName());
        // List of price history points, one per scrapedAt bucket, sorted ascending
        response.setPoints(groupedSnapshots.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> {
            List<QueryPriceSnapshot> bucket = entry.getValue();
            PriceHistoryResponse.PriceHistoryPoint point = new PriceHistoryResponse.PriceHistoryPoint();
            point.setScrapedAt(entry.getKey());
            point.setRetailerPrices(bucket.stream()
                .map(snapshot -> {
                    PriceHistoryResponse.RetailerPrice rp = new PriceHistoryResponse.RetailerPrice();
                    rp.setRetailerName(snapshot.getRetailer().getName());
                    rp.setPrice(snapshot.getPrice());
                    rp.setMatched(snapshot.isMatched());
                    return rp;
                })
                .toList());
            bucket.stream()
                .filter(QueryPriceSnapshot::isMatched)
                .min(Comparator.comparing(QueryPriceSnapshot::getPrice))
                .ifPresent(best -> {
                    point.setBestMatchedPrice(best.getPrice());
                    point.setBestRetailer(best.getRetailer().getName());
                });
            return point;
        })
        .toList());
        return response;
    }

    public void recordSnapshot(TrackedQuery tracked){
        List<PriceResult> results = scraperService.search(tracked.getParsedQuery());

        List<QueryResolutionService.MatchedResult> matched = queryResolutionService.matchResults(results, tracked);

        OffsetDateTime scrapedAt = OffsetDateTime.now();

        // For each matched result find/create Retailer, save QueryPriceSnapshot
        for (QueryResolutionService.MatchedResult matchedResult : matched) {
            PriceResult result = matchedResult.result();
            Retailer retailer = retailerRepository.findByNameIgnoreCase(result.getRetailerName()).orElseGet(() -> {
                Retailer r = new Retailer();
                r.setName(result.getRetailerName());
                r.setBaseUrl("https://www." + result.getRetailerName().toLowerCase() + ".com");
                return retailerRepository.save(r);
            });

            // Save snapshot
            QueryPriceSnapshot snapshot = new QueryPriceSnapshot();
            snapshot.setTrackedQuery(tracked);
            snapshot.setRetailer(retailer);
            snapshot.setPrice(matchedResult.price());
            snapshot.setCurrency(result.getCurrency());
            snapshot.setUrl(result.getUrl());
            snapshot.setMatched(matchedResult.matched());
            snapshot.setMatchMethod(matchedResult.matchMethod());
            snapshot.setScrapedAt(scrapedAt);
            queryPriceSnapshotRepository.save(snapshot);
        }

        // Update tracked.lastScrapedAt = now
        tracked.setLastScrapedAt(scrapedAt);
        trackedQueryRepository.save(tracked);
    }
}
