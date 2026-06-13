package com.agenticprice.tracking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import com.agenticprice.tracking.service.TrackingService;
import com.agenticprice.tracking.model.TrackedQuery;
import com.agenticprice.tracking.repository.TrackedQueryRepository;
import com.agenticprice.tracking.model.TrackingStatus;
import com.agenticprice.model.User;
import com.agenticprice.repository.UserRepository;


@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingScheduler {
    private final TrackingService trackingService;
    private final TrackedQueryRepository trackedQueryRepository;
    private final UserRepository userRepository;

    @Value("${tracking.queries:laptop,headphones,keyboard}")
    private String defaultQueries;

    @Scheduled(cron="${TRACKING_CRON:0 0 */6 * * *}")
    public void runTrackingScrape() {
        // load ACTIVE queries
        // for each: trackingService.recordSnapshot(tracked)

        log.info("Running tracking scrape...");
        
        for(User user: userRepository.findAll()) {
            List<TrackedQuery> queries = trackedQueryRepository.findByUserIdAndStatus(user.getId(), TrackingStatus.ACTIVE);
            for (TrackedQuery query: queries) {
                try {
                    trackingService.recordSnapshot(query);
                } catch (Exception e) {
                    log.error("Tracking scrape failed for query '{}': {}", query.getRawQuery(), e.getMessage());
                }
            }
            log.info("Tracking scrape completed for user '{}'", user.getEmail());
        }
        log.info("Tracking scrape completed for all users");
    }
}
