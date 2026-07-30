package com.agenticprice.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @Value("${SENDGRID_API_KEY}")
    private String sendGridApiKey;

    private static final String FROM_EMAIL = "PriceHawkNotifications@gmail.com";
    private static final String FROM_NAME = "PriceHawk AI";

    public void sendPriceAlert(String toEmail, String productQuery, String price, String url, String threshold) {
        String subject = "PriceHawk Alert: " + productQuery + " is now " + price;
        String body =
                "Good news! A price alert you set on PriceHawk has been triggered.\n\n" +
                        "Product: " + productQuery + "\n" +
                        "Current Price: " + price + "\n" +
                        "Your Threshold: $" + threshold + "\n" +
                        "Link: " + url + "\n\n" +
                        "This alert has been deactivated. Visit PriceHawk to set a new one.";
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendOtpEmail(String toEmail, String code) {
        String subject = "Your PriceHawk login code";
        String body =
                "Your PriceHawk login code is: " + code + "\n\n" +
                        "This code expires in 10 minutes.\n\n" +
                        "If you didn't request this, ignore this email.";
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String toEmail, String subject, String body) {
        try {
            Email from = new Email(FROM_EMAIL, FROM_NAME);
            Email to = new Email(toEmail);
            Content content = new Content("text/plain", body);
            Mail mail = new Mail(from, subject, to, content);

            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error {}: {}", response.getStatusCode(), response.getBody());
            } else {
                log.info("Email sent to {} via SendGrid (status {})", toEmail, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}