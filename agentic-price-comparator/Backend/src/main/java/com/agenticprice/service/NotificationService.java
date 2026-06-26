package com.agenticprice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${MAIL_USERNAME:}")
    private String fromEmail;

    private boolean isEmailConfigured() {
        return fromEmail != null && !fromEmail.isBlank();
    }

    @Async
    public void sendPriceAlert(String toEmail, String productQuery, String price, String url, String threshold) {
        if (!isEmailConfigured()) {
            log.info("[DEV] Price alert for {}: {} is now {} (threshold: ${}). URL: {}",
                    toEmail, productQuery, price, threshold, url);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("PricePilot Alert: " + productQuery + " is now " + price);
            message.setText(
                    "Good news! A price alert you set on PricePilot has been triggered.\n\n" +
                            "Product: " + productQuery + "\n" +
                            "Current Price: " + price + "\n" +
                            "Your Threshold: $" + threshold + "\n" +
                            "Link: " + url + "\n\n" +
                            "This alert has been deactivated. Visit PricePilot to set a new one."
            );
            mailSender.send(message);
            log.info("Price alert email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send alert email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendOtpEmail(String toEmail, String code) {
        if (!isEmailConfigured()) {
            log.info("╔══════════════════════════════════════╗");
            log.info("║  [DEV] OTP for: {}  ║", toEmail);
            log.info("║  Code: {}                            ║", code);
            log.info("╚══════════════════════════════════════╝");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your PricePilot login code");
            message.setText(
                    "Your PricePilot login code is: " + code + "\n\n" +
                            "This code expires in 10 minutes.\n\n" +
                            "If you didn't request this, ignore this email."
            );
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
}