package com.agenticprice.service;

import com.agenticprice.model.PriceAlert;
import com.agenticprice.repository.PriceAlertRepository;
import com.agenticprice.scraper.PriceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final NotificationService notificationService;

    public PriceAlert createAlert(String productQuery, BigDecimal thresholdPrice, String email) {
        PriceAlert alert = new PriceAlert();
        alert.setProductQuery(productQuery);
        alert.setThresholdPrice(thresholdPrice);
        alert.setEmail(email);
        alert.setActive(true);
        alert.setCreatedAt(OffsetDateTime.now());
        return priceAlertRepository.save(alert);
    }

    public List<PriceAlert> getActiveAlerts() {
        return priceAlertRepository.findByActiveTrue();
    }

    public List<PriceAlert> getAllAlerts() {
        return priceAlertRepository.findAll();
    }

    public void deleteAlert(java.util.UUID id) {
        priceAlertRepository.deleteById(id);
    }

    public void checkAlerts(List<PriceResult> results) {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByActiveTrue();

        for (PriceAlert alert : activeAlerts) {
            for (PriceResult result : results) {
                if (!result.getProductName().toLowerCase().contains(alert.getProductQuery().toLowerCase())) {
                    continue;
                }
                try {
                    String cleaned = result.getPrice().replaceAll("[^0-9.]", "");
                    BigDecimal resultPrice = new BigDecimal(cleaned);
                    if (resultPrice.compareTo(alert.getThresholdPrice()) <= 0) {
                        notificationService.sendPriceAlert(
                                alert.getEmail(),
                                alert.getProductQuery(),
                                result.getPrice(),
                                result.getUrl(),
                                alert.getThresholdPrice().toString()
                        );
                        alert.setActive(false);
                        alert.setTriggeredAt(OffsetDateTime.now());
                        priceAlertRepository.save(alert);
                        log.info("Alert triggered for {} at {}", alert.getProductQuery(), result.getPrice());
                        break;
                    }
                } catch (Exception e) {
                    log.warn("Could not parse price '{}': {}", result.getPrice(), e.getMessage());
                }
            }
        }
    }
}