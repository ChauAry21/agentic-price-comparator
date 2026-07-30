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

    /**
     * Creates a new alert. The {@code email} parameter MUST be the resolved
     * identity of the signed-in user (from OAuth principal or the
     * X-User-Email fallback). It is trimmed before persisting so that
     * downstream ownership checks compare cleanly.
     */
    public PriceAlert createAlert(String productQuery, BigDecimal thresholdPrice, String email) {
        PriceAlert alert = new PriceAlert();
        alert.setProductQuery(productQuery);
        alert.setThresholdPrice(thresholdPrice);
        alert.setEmail(email == null ? null : email.trim());
        alert.setActive(true);
        alert.setCreatedAt(OffsetDateTime.now());
        return priceAlertRepository.save(alert);
    }

    public List<PriceAlert> getActiveAlerts() {
        return priceAlertRepository.findByActiveTrue();
    }


    public List<PriceAlert> getAlertsForEmail(String email)
    {
        return priceAlertRepository.findByEmail(email);
    }

    /**
     * Deletes an alert only if it belongs to the given email. Returns true when
     * the alert existed, was owned by the caller, and was deleted. Returns false
     * for any other case (missing, owned by someone else) so callers can return 404
     * instead of leaking existence.
     */
    /**
     * Deletes an alert only if it belongs to the given email. Both sides are
     * trimmed before comparison so trailing whitespace from earlier saves
     * doesn't cause false 404s. Returns true when the alert existed, was
     * owned by the caller, and was deleted. Returns false for any other
     * case (missing, owned by someone else) so callers can return 404
     * instead of leaking existence.
     */
    public boolean deleteIfOwnedBy(java.util.UUID id, String email) {
        if (id == null || email == null || email.isBlank()) return false;
        String caller = email.trim();
        return priceAlertRepository.findById(id)
                .filter(alert -> caller.equalsIgnoreCase(alert.getEmail().trim()))
                .map(alert -> {
                    priceAlertRepository.delete(alert);
                    return true;
                })
                .orElse(false);
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