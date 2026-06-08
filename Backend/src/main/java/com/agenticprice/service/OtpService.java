package com.agenticprice.service;

import com.agenticprice.model.OtpCode;
import com.agenticprice.model.User;
import com.agenticprice.repository.OtpCodeRepository;
import com.agenticprice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public void sendOtp(String email) {
        otpCodeRepository.deleteByEmail(email);
        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        OtpCode otp = new OtpCode();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(OffsetDateTime.now().plusMinutes(10));
        otp.setUsed(false);
        otp.setCreatedAt(OffsetDateTime.now());
        otpCodeRepository.save(otp);
        notificationService.sendOtpEmail(email, code);
        log.info("OTP sent to {}", email);
    }

    public boolean verifyOtp(String email, String code) {
        return otpCodeRepository.findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .filter(otp -> otp.getCode().equals(code))
                .filter(otp -> otp.getExpiresAt().isAfter(OffsetDateTime.now()))
                .map(otp -> {
                    otp.setUsed(true);
                    otpCodeRepository.save(otp);
                    if (userRepository.findByEmail(email).isEmpty()) {
                        User user = new User();
                        user.setEmail(email);
                        user.setCreatedAt(OffsetDateTime.now());
                        userRepository.save(user);
                    }
                    return true;
                })
                .orElse(false);
    }
}