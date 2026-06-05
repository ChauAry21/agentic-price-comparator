package com.agenticprice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

   
    // Keys match user registration emails; values retain plaintext password credentials
    private static final Map<String, String> customUserDatabase = new ConcurrentHashMap<>();
    
    // In-memory runtime block allocation caching active login 2FA tokens
    private final Map<String, String> email2faCache = new ConcurrentHashMap<>();

    static {
        // Pre-populating default test variables
        customUserDatabase.put("mahima@test.com", "password123");
    }

    // 1. New Dynamic Signup Endpoint
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> signupRequest) {
        String email = signupRequest.get("email");
        String password = signupRequest.get("password");

        if (email == null || password == null || password.length() < 6) {
            return ResponseEntity.status(400).body(Map.of("error", "Invalid sign up values."));
        }

        // Return error if user already exists
        if (customUserDatabase.containsKey(email)) {
            return ResponseEntity.status(400).body(Map.of("error", "This email address is already registered!"));
        }

        // Commit profile parameters into local database simulation cache
        customUserDatabase.put(email, password);
        System.out.println("📝 DYNAMIC REGISTRATION REGISTERED -> User Email: " + email);

        return ResponseEntity.ok(Map.of("message", "User account registered successfully!"));
    }

    // 2. Updated Login Validation Route Mapping
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        String email = loginRequest.get("email");
        String password = loginRequest.get("password");

        // Fetch user from our dynamic custom user registry map
        String savedPassword = customUserDatabase.get(email);

        // Process validation matching credentials checks
        if (savedPassword != null && savedPassword.equals(password)) {
            String twoFactorCode = generate6DigitCode();
            email2faCache.put(email, twoFactorCode);
            
            sendEmailNotification(email, twoFactorCode);

            return ResponseEntity.ok(Map.of("status", "STEP_2_REQUIRED"));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    // 3. Phase 2 2FA verification route
    @PostMapping("/verify-2fa")
    public ResponseEntity<?> verify2FA(@RequestBody Map<String, String> twoFactorRequest) {
        String email = twoFactorRequest.get("email");
        String enteredCode = twoFactorRequest.get("code");

        String correctCode = email2faCache.get(email);

        if (correctCode != null && correctCode.equals(enteredCode)) {
            email2faCache.remove(email);
            String mockAccessToken = "mock-oauth2-access-token-for-" + email;
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "accessToken", mockAccessToken
            ));
        }

        return ResponseEntity.status(400).body(Map.of("error", "Invalid or expired verification code."));
    }

    private String generate6DigitCode() {
        Random random = new Random();
        int number = random.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    private void sendEmailNotification(String toEmail, String code) {
        System.out.println("\n=== 📧 INTERNAL MAIL SERVER SIMULATOR ===");
        System.out.println("Sending Email To: " + toEmail);
        System.out.println("Subject: Your PricePilot AI Verification Code");
        System.out.println("Body: Hello, your 6-digit login verification code is: [" + code + "]");
        System.out.println("=========================================\n");
    }
}