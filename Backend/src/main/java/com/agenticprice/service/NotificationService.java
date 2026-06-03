package com.agenticprice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${MAIL_USERNAME}")
    private String fromEmail;

    public void sendPriceAlert(String toEmail, String productQuery, String price, String url, String threshold) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("PriceHawk Alert: " + productQuery + " is now " + price);
            message.setText(
                    "Good news! A price alert you set on PriceHawk has been triggered.\n\n" +
                            "Product: " + productQuery + "\n" +
                            "Current Price: " + price + "\n" +
                            "Your Threshold: $" + threshold + "\n" +
                            "Link: " + url + "\n\n" +
                            "This alert has been deactivated. Visit PriceHawk to set a new one."
            );
            mailSender.send(message);
            log.info("Price alert email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send alert email to {}: {}", toEmail, e.getMessage());
        }
    }
}