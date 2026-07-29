package com.agenticprice.controller;

import com.agenticprice.api.CreateAlertRequest;
import com.agenticprice.model.PriceAlert;
import com.agenticprice.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    /**
     * Resolves the caller's email from either the OAuth2 principal (preferred
     * once login is wired up) or the legacy X-User-Email header used during
     * the OTP-only flow. Returns null if neither is present.
     */
    private String resolveEmail(OAuth2User principal, String headerEmail) {
        if (principal != null) {
            Object attr = principal.getAttribute("email");
            if (attr != null) return attr.toString();
        }
        if (headerEmail != null && !headerEmail.isBlank()) {
            return headerEmail;
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<PriceAlert> createAlert(
            @RequestBody CreateAlertRequest request,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail) {
        String email = resolveEmail(principal, headerEmail);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        PriceAlert alert = priceAlertService.createAlert(
                request.getProductQuery(),
                request.getThresholdPrice(),
                email
        );
        return ResponseEntity.ok(alert);
    }

    @GetMapping
    public ResponseEntity<List<PriceAlert>> getAlerts(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail) {
        // Identity comes from the OAuth2 principal when present (real auth)
        // and falls back to the X-User-Email header for the OTP-only flow.
        String email = resolveEmail(principal, headerEmail);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(priceAlertService.getAlertsForEmail(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable UUID id,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestHeader(value = "X-User-Email", required = false) String headerEmail) {
        String email = resolveEmail(principal, headerEmail);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        boolean owned = priceAlertService.deleteIfOwnedBy(id, email);
        return owned ? ResponseEntity.noContent().build()
                     : ResponseEntity.notFound().build();
    }
}