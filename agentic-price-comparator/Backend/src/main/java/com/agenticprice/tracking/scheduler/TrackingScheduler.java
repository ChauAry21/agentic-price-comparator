package com.agenticprice.tracking.scheduler;

import com.agenticprice.tracking.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingScheduler {

    private final TrackingService trackingService;

    @Scheduled(cron = "${TRACKING_CRON:0 0 0/12 * * *}")
    public void runTracking() {
        log.info("Running tracking snapshots...");
        try {
            trackingService.runSnapshots();
        } catch (Exception e) {
            log.error("Tracking scheduler failed: {}", e.getMessage());
        }
        log.info("Tracking snapshots complete.");
    }
}
